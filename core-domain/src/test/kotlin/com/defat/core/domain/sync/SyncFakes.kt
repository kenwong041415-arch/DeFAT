package com.defat.core.domain.sync

import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthOutcome
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** One local row with its sync bookkeeping, as Room would hold it. */
data class FakeRow(val doc: SyncDoc, val syncedAt: Long?) {
    val isPending: Boolean get() = syncedAt == null || syncedAt < doc.updatedAt
}

/**
 * A local collection with the real semantics: pending = never synced or edited
 * since, markSynced is conditional on updatedAt, applyRemote stores a synced row.
 */
class FakeSource(override val name: String, private val log: MutableList<String> = mutableListOf()) : SyncableCollection {
    val rows = linkedMapOf<String, FakeRow>()

    /** Insert as a freshly logged, never-uploaded row (what every pre-sync row looks like). */
    fun log(id: String, updatedAt: Long = 1_000L, deletedAt: Long? = null, fields: Map<String, Any?> = mapOf("n" to id)) {
        rows[id] = FakeRow(SyncDoc(id, updatedAt, deletedAt, fields), syncedAt = null)
    }

    /** Insert as a row that was already uploaded. */
    fun logSynced(id: String, updatedAt: Long = 1_000L) {
        rows[id] = FakeRow(SyncDoc(id, updatedAt, null, mapOf("n" to id)), syncedAt = updatedAt)
    }

    override suspend fun hasRows() = rows.isNotEmpty()
    override suspend fun pending() = rows.values.filter { it.isPending }.map { it.doc }
    override suspend fun markSynced(doc: SyncDoc) {
        val row = rows[doc.id] ?: return
        if (row.doc.updatedAt == doc.updatedAt) rows[doc.id] = row.copy(syncedAt = doc.updatedAt)
    }

    override suspend fun stamp(id: String) = rows[id]?.doc?.stamp
    override suspend fun applyRemote(doc: SyncDoc) {
        log += "apply:$name:${doc.id}"
        rows[doc.id] = FakeRow(doc, syncedAt = doc.updatedAt)
    }

    override suspend fun markAllPending() {
        rows.replaceAll { _, row -> row.copy(syncedAt = null) }
    }

    override suspend fun deleteAllLocal() {
        log += "wipe:$name"
        rows.clear()
    }

    override suspend fun liveDocs() = rows.values.map { it.doc }.filter { it.deletedAt == null }
}

class FakeRemote(override val uid: String = "uid-1") : RemoteSyncStore {
    /** collection -> id -> (doc, serverUpdatedAt) */
    val tree = linkedMapOf<String, LinkedHashMap<String, RemoteDoc>>()
    val calls = mutableListOf<String>()
    val batchSizes = mutableListOf<Int>()
    var serverClock = 10_000L
    var failWrites = false
    var failReads = false
    var failDeletes = false

    /** Collections whose deletes are silently ignored: the bug RemoteTreeDeleter must catch. */
    val undeletable = mutableSetOf<String>()

    fun put(collection: String, id: String, updatedAt: Long, deletedAt: Long? = null, fields: Map<String, Any?> = mapOf("n" to id)) {
        serverClock += 1
        tree.getOrPut(collection) { linkedMapOf() }[id] = RemoteDoc(SyncDoc(id, updatedAt, deletedAt, fields), serverClock)
    }

    fun totalDocs() = tree.values.sumOf { it.size }

    override suspend fun writeBatch(items: List<CollectionDoc>) {
        calls += "write:${items.size}"
        if (failWrites) error("write failed")
        require(items.size <= PUSH_BATCH_SIZE) { "batch of ${items.size} exceeds $PUSH_BATCH_SIZE" }
        batchSizes += items.size
        serverClock += 1
        for (item in items) {
            tree.getOrPut(item.collection) { linkedMapOf() }[item.doc.id] = RemoteDoc(item.doc, serverClock)
        }
    }

    override suspend fun readChangedSince(collection: String, cursorMillis: Long): List<RemoteDoc> {
        calls += "read:$collection"
        if (failReads) error("read failed")
        return tree[collection].orEmpty().values.filter { it.serverUpdatedAt >= cursorMillis }
    }

    override suspend fun listIds(collection: String, limit: Int): List<String> {
        calls += "list:$collection"
        return tree[collection].orEmpty().keys.take(limit)
    }

    override suspend fun deleteDocs(collection: String, ids: List<String>) {
        calls += "delete:$collection:${ids.size}"
        if (failDeletes) error("delete failed")
        if (collection in undeletable) return
        ids.forEach { tree[collection]?.remove(it) }
    }
}

class FakeState : SyncStateStore {
    private val uid = MutableStateFlow<String?>(null)
    private val last = MutableStateFlow<Long?>(null)
    val cursors = linkedMapOf<String, Long>()

    override val lastSignedInUid: Flow<String?> = uid
    override val lastSyncAt: Flow<Long?> = last
    override suspend fun getLastSignedInUid() = uid.value
    override suspend fun setLastSignedInUid(uid: String) { this.uid.value = uid }
    override suspend fun pullCursor(collection: String) = cursors[collection] ?: 0L
    override suspend fun setPullCursor(collection: String, millis: Long) { cursors[collection] = millis }
    override suspend fun resetPullCursors() { cursors.clear() }
    override suspend fun setLastSyncAt(millis: Long) { last.value = millis }
    override suspend fun clearAll() {
        uid.value = null
        last.value = null
        cursors.clear()
    }
}

class FakeAuth(initial: AuthUser? = null) : AuthRepository {
    private val user = MutableStateFlow(initial)
    var reauthResult: AuthError? = null
    var deleteResult: AuthError? = null
    val calls = mutableListOf<String>()

    override val currentUser: Flow<AuthUser?> = user
    override fun currentUserNow() = user.value
    override suspend fun signUp(email: String, password: String): AuthOutcome = error("not used")
    override suspend fun signIn(email: String, password: String): AuthOutcome = error("not used")
    override suspend fun sendPasswordReset(email: String): AuthError? = null
    override suspend fun signOut() { user.value = null }
    override suspend fun reauthenticate(password: String): AuthError? {
        calls += "reauth"
        return reauthResult
    }
    override suspend fun deleteAccount(): AuthError? {
        calls += "deleteAuth"
        if (deleteResult == null) user.value = null
        return deleteResult
    }
}

/** The real set of sources the engine demands, backed by in-memory fakes. */
class FakeWorld(uid: String = "uid-1", lastUid: String? = null) {
    val log = mutableListOf<String>()
    val sources: Map<String, FakeSource> =
        (listOf(SyncCollections.PROFILE) + SYNCED_SUBCOLLECTIONS.filter { it !in SUBCOLLECTIONS_NOT_YET_WRITTEN })
            .associateWith { FakeSource(it, log) }
    val remote = FakeRemote(uid)
    val state = FakeState()
    val auth = FakeAuth(AuthUser(uid, "$uid@example.com"))
    var nowMillis = 50_000L

    init {
        if (lastUid != null) kotlinx.coroutines.runBlocking { state.setLastSignedInUid(lastUid) }
    }

    operator fun get(name: String): FakeSource = sources.getValue(name)

    fun coordinator(batchSize: Int = PUSH_BATCH_SIZE) = DefaultSyncCoordinator(
        auth = auth,
        sources = sources.values.toList(),
        remoteFor = { remote },
        state = state,
        nowMillis = { nowMillis },
        batchSize = batchSize,
    )

    fun engine(batchSize: Int = PUSH_BATCH_SIZE) = SyncEngine(sources.values.toList(), remote, state, batchSize)

    fun localRowCount() = sources.values.sumOf { it.rows.size }
}

package com.defat.core.data.sync

import com.defat.core.domain.sync.CollectionDoc
import com.defat.core.domain.sync.RemoteDoc
import com.defat.core.domain.sync.RemoteSyncStore
import com.defat.core.domain.sync.SyncCollections
import com.defat.core.domain.sync.SyncDoc
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firestore, bound to one uid. Every path is built here from that uid and the
 * constants in SyncCollections — never from anything inside a document — which
 * mirrors the security rules: ownership is the path (phase1.5 plan §2).
 *
 * Every call has a timeout. With no network a Firestore write Task does not fail,
 * it waits; an unbounded wait would hold the sync lock forever. A timed-out write
 * is harmless: the rows stay pending and are simply written again next pass
 * (a `set` is idempotent).
 */
class FirestoreSyncStore(
    private val firestore: FirebaseFirestore,
    override val uid: String,
) : RemoteSyncStore {

    init {
        require(uid.isNotBlank()) { "uid must not be blank" }
    }

    private val userDoc: DocumentReference get() = firestore.collection(USERS).document(uid)

    private fun docRef(collection: String, id: String): DocumentReference =
        if (collection == SyncCollections.PROFILE) userDoc else userDoc.collection(collection).document(id)

    override suspend fun writeBatch(items: List<CollectionDoc>) {
        if (items.isEmpty()) return
        val batch = firestore.batch()
        for ((collection, doc) in items) {
            val data = HashMap<String, Any?>(doc.fields)
            data[UPDATED_AT] = doc.updatedAt
            data[DELETED_AT] = doc.deletedAt
            // The pull cursor. Assigned by the server so that two phones with
            // different clocks still order their commits consistently.
            data[SERVER_UPDATED_AT] = FieldValue.serverTimestamp()
            batch.set(docRef(collection, doc.id), data)
        }
        withTimeout(TIMEOUT_MS) { batch.commit().await() }
    }

    override suspend fun readChangedSince(collection: String, cursorMillis: Long): List<RemoteDoc> {
        if (collection == SyncCollections.PROFILE) {
            val snapshot = withTimeout(TIMEOUT_MS) { userDoc.get(Source.SERVER).await() }
            return listOfNotNull(snapshot.toRemoteDocOrNull(cursorMillis))
        }
        val query = userDoc.collection(collection)
            .whereGreaterThanOrEqualTo(SERVER_UPDATED_AT, Timestamp(cursorMillis / 1000, ((cursorMillis % 1000) * 1_000_000).toInt()))
        val snapshot = withTimeout(TIMEOUT_MS) { query.get(Source.SERVER).await() }
        return snapshot.documents.mapNotNull { it.toRemoteDocOrNull(cursorMillis) }
    }

    override suspend fun listIds(collection: String, limit: Int): List<String> {
        if (collection == SyncCollections.PROFILE) {
            val snapshot = withTimeout(TIMEOUT_MS) { userDoc.get(Source.SERVER).await() }
            return if (snapshot.exists()) listOf(uid) else emptyList()
        }
        val snapshot = withTimeout(TIMEOUT_MS) {
            userDoc.collection(collection).limit(limit.toLong()).get(Source.SERVER).await()
        }
        return snapshot.documents.map { it.id }
    }

    override suspend fun deleteDocs(collection: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = firestore.batch()
        ids.forEach { id -> batch.delete(docRef(collection, id)) }
        withTimeout(TIMEOUT_MS) { batch.commit().await() }
    }

    /** Null for a missing document or one without the fields every write of ours carries. */
    private fun DocumentSnapshot.toRemoteDocOrNull(cursorMillis: Long): RemoteDoc? {
        val data = this.data ?: return null
        val updatedAt = (data[UPDATED_AT] as? Number)?.toLong() ?: return null
        val server = (data[SERVER_UPDATED_AT] as? Timestamp)?.let { it.seconds * 1000 + it.nanoseconds / 1_000_000 }
            ?: return null
        if (server < cursorMillis) return null
        val fields = data.filterKeys { it != UPDATED_AT && it != DELETED_AT && it != SERVER_UPDATED_AT }
        return RemoteDoc(
            doc = SyncDoc(
                id = id,
                updatedAt = updatedAt,
                deletedAt = (data[DELETED_AT] as? Number)?.toLong(),
                fields = fields,
            ),
            serverUpdatedAt = server,
        )
    }

    private companion object {
        const val USERS = "users"
        const val UPDATED_AT = "updatedAt"
        const val DELETED_AT = "deletedAt"
        const val SERVER_UPDATED_AT = "serverUpdatedAt"
        const val TIMEOUT_MS = 30_000L
    }
}

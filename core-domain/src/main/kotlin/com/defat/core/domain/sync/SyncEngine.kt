package com.defat.core.domain.sync

/** Firestore allows 500 writes per batch; 400 leaves headroom (§6.1). */
const val PUSH_BATCH_SIZE = 400

/**
 * Moves rows between the local [sources] and one user's [remote] tree.
 *
 * Deliberately knows nothing about Room, DataStore or Firestore, so its
 * ordering and batching are covered by plain JVM tests.
 *
 * The engine never deletes a local row because a remote document is *absent*.
 * Pulling only upserts what exists; the only thing that removes local data is
 * an explicit [replaceLocalWithRemote] (Case C, after the user said Replace)
 * or account deletion. That property is what makes "empty cloud" harmless.
 */
class SyncEngine(
    private val sources: List<SyncableCollection>,
    private val remote: RemoteSyncStore,
    private val state: SyncStateStore,
    private val batchSize: Int = PUSH_BATCH_SIZE,
) {
    init {
        require(batchSize in 1..PUSH_BATCH_SIZE) { "batchSize must be 1..$PUSH_BATCH_SIZE" }
        val expected = setOf(SyncCollections.PROFILE) +
            (SYNCED_SUBCOLLECTIONS.toSet() - SUBCOLLECTIONS_NOT_YET_WRITTEN)
        val actual = sources.map { it.name }
        require(actual.size == actual.toSet().size) { "duplicate sync sources: $actual" }
        require(actual.toSet() == expected) {
            "sync sources $actual do not match SYNCED_SUBCOLLECTIONS " +
                "(expected $expected). Add or remove a source, or update SyncCollections."
        }
    }

    private val byName: Map<String, SyncableCollection> = sources.associateBy { it.name }

    suspend fun hasLocalRows(): Boolean = sources.any { it.hasRows() }

    /** Case A: every local row becomes pending, so the next push uploads all of it. */
    suspend fun markAllPending() {
        sources.forEach { it.markAllPending() }
    }

    /**
     * Uploads every pending row in batches of at most [batchSize], stamping each
     * batch as synced only after the server accepted it. A failure leaves the
     * remaining rows pending for the next pass.
     *
     * @return the number of documents uploaded.
     */
    suspend fun pushPending(): Int {
        val items = sources.flatMap { source ->
            source.pending().map { doc -> CollectionDoc(source.name, doc) }
        }
        return push(items)
    }

    /**
     * Case A (`SignInPlan.ADOPT_LOCAL`): this phone's rows become the account's
     * data.
     *
     * Pushes local rows first and never removes or overwrites anything local,
     * so an empty cloud is harmless — that is the property the whole phase is
     * built around. The one refinement over a blind push: a local row is held
     * back when the cloud already holds a **newer** document with the same id.
     * Weights are keyed by date, water by day and the profile is one fixed
     * document, so without that guard a phone used offline and signed in later
     * would overwrite newer records made on a phone that was already signed
     * in — losing data in the opposite direction from the one §6.3 worries
     * about.
     *
     * Reading the remote tree here is a comparison, not a download-first:
     * [applyAll] only ever upserts, and the engine never deletes a local row
     * because a remote document is absent. Rows held back stay pending, and
     * applying the remote copy below clears them.
     *
     * @return documents uploaded, plus documents applied from the cloud.
     */
    suspend fun adoptLocal(): Int {
        markAllPending()
        val remoteDocs = sources.associateWith { remote.readChangedSince(it.name, 0L) }

        val items = sources.flatMap { source ->
            val remoteStamps = remoteDocs.getValue(source).associate { it.doc.id to it.doc.stamp }
            source.pending()
                .filter { local ->
                    val theirs = remoteStamps[local.id]
                    // Keep the row only if the cloud has nothing newer for it.
                    theirs == null || ConflictResolution.resolve(local.stamp, theirs) != ConflictWinner.TAKE_REMOTE
                }
                .map { doc -> CollectionDoc(source.name, doc) }
        }
        var moved = push(items)

        for ((source, docs) in remoteDocs) {
            moved += applyAll(source, docs)
            if (docs.isNotEmpty()) state.setPullCursor(source.name, docs.maxOf { it.serverUpdatedAt })
        }
        return moved
    }

    private suspend fun push(items: List<CollectionDoc>): Int {
        var uploaded = 0
        for (chunk in items.chunked(batchSize)) {
            remote.writeBatch(chunk)
            for (item in chunk) byName.getValue(item.collection).markSynced(item.doc)
            uploaded += chunk.size
        }
        return uploaded
    }

    /**
     * Fetches documents changed since each collection's stored cursor and applies
     * the ones that win [ConflictResolution]. The cursor only advances after the
     * whole collection was applied.
     *
     * @return the number of documents applied locally.
     */
    suspend fun pullSince(): Int {
        var applied = 0
        for (source in sources) {
            val cursor = state.pullCursor(source.name)
            val fetched = remote.readChangedSince(source.name, cursor)
            applied += applyAll(source, fetched)
            if (fetched.isNotEmpty()) {
                state.setPullCursor(source.name, maxOf(cursor, fetched.maxOf { it.serverUpdatedAt }))
            }
        }
        return applied
    }

    /** First sign-in with nothing local: forget cursors, then pull everything. */
    suspend fun fullPull(): Int {
        state.resetPullCursors()
        return pullSince()
    }

    /**
     * Case C, after the user chose Replace. Downloads first and only then
     * removes local rows, so a failed download (offline, rules) cannot leave the
     * phone with neither the old records nor the new ones.
     */
    suspend fun replaceLocalWithRemote(): Int {
        val fetched = sources.associateWith { remote.readChangedSince(it.name, 0L) }
        sources.forEach { it.deleteAllLocal() }
        state.resetPullCursors()
        var applied = 0
        for ((source, docs) in fetched) {
            applied += applyAll(source, docs)
            if (docs.isNotEmpty()) state.setPullCursor(source.name, docs.maxOf { it.serverUpdatedAt })
        }
        return applied
    }

    private suspend fun applyAll(source: SyncableCollection, docs: List<RemoteDoc>): Int {
        var applied = 0
        for (remoteDoc in docs) {
            val local = source.stamp(remoteDoc.doc.id)
            // A tombstone for a row this phone never had has nothing to delete;
            // storing it would only make an otherwise empty phone look populated.
            if (local == null && remoteDoc.doc.deletedAt != null) continue
            val winner = ConflictResolution.resolve(local, remoteDoc.doc.stamp)
            if (winner == ConflictWinner.TAKE_REMOTE) {
                source.applyRemote(remoteDoc.doc)
                applied++
            }
        }
        return applied
    }
}

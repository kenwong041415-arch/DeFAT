package com.defat.core.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * One synced document, in the shape both Room and Firestore can round-trip.
 *
 * [fields] holds only plain values — String, Long, Int, Double, Boolean, null,
 * or a nested Map of those — so the same map is what Firestore stores and what
 * the "export my data" file contains. The bookkeeping timestamps are kept out
 * of [fields] and carried explicitly.
 */
data class SyncDoc(
    val id: String,
    val updatedAt: Long,
    val deletedAt: Long?,
    val fields: Map<String, Any?>,
) {
    val stamp: SyncStamp get() = SyncStamp(updatedAt, deletedAt)
}

/** A [SyncDoc] together with the collection it belongs to, for mixed batches. */
data class CollectionDoc(val collection: String, val doc: SyncDoc)

/** A pulled document plus the server's commit time, which is the pull cursor. */
data class RemoteDoc(val doc: SyncDoc, val serverUpdatedAt: Long)

/**
 * The local (Room / DataStore) side of one synced collection. [name] is a
 * member of [SYNCED_SUBCOLLECTIONS], or [SyncCollections.PROFILE].
 *
 * "Pending" means `syncedAt` is null or older than `updatedAt` (§6.1). Every
 * ordinary local write produces a row with `syncedAt = null`, so a write can
 * never be mistaken for already uploaded.
 */
interface SyncableCollection {
    val name: String

    /** True if there is any row at all, tombstones included. */
    suspend fun hasRows(): Boolean

    /** Every row that still has to be uploaded, tombstones included. */
    suspend fun pending(): List<SyncDoc>

    /**
     * Records that [doc] reached the server. Must be conditional on the row's
     * `updatedAt` still being [SyncDoc.updatedAt], so an edit made while the
     * upload was in flight stays pending.
     */
    suspend fun markSynced(doc: SyncDoc)

    /** The stamp of the local row with this id, tombstones included, or null. */
    suspend fun stamp(id: String): SyncStamp?

    /** Upserts a pulled document as an already-synced row (or tombstone). */
    suspend fun applyRemote(doc: SyncDoc)

    /** Case A: forget that anything was uploaded, so every row is pending. */
    suspend fun markAllPending()

    /** Case C "Replace" and account deletion: remove this collection's local rows. */
    suspend fun deleteAllLocal()

    /** Live (not tombstoned) rows, for "export my data". */
    suspend fun liveDocs(): List<SyncDoc>
}

/**
 * The Firestore side, bound to one uid: every path it touches is
 * `users/{uid}/...` for that uid and no other. [SyncCollections.PROFILE] is the
 * `users/{uid}` document itself.
 */
interface RemoteSyncStore {
    val uid: String

    /** Writes the batch atomically. Callers keep it at or below [PUSH_BATCH_SIZE]. */
    suspend fun writeBatch(items: List<CollectionDoc>)

    /**
     * Documents whose server commit time is at or after [cursorMillis]. The
     * boundary is inclusive on purpose: two commits in the same millisecond
     * must not be skipped, and re-reading one is harmless (apply is idempotent).
     */
    suspend fun readChangedSince(collection: String, cursorMillis: Long): List<RemoteDoc>

    /** Up to [limit] document ids currently in [collection], straight from the server. */
    suspend fun listIds(collection: String, limit: Int): List<String>

    suspend fun deleteDocs(collection: String, ids: List<String>)
}

/** Small persisted facts about sync itself (DataStore). */
interface SyncStateStore {
    val lastSignedInUid: Flow<String?>
    val lastSyncAt: Flow<Long?>

    suspend fun getLastSignedInUid(): String?
    suspend fun setLastSignedInUid(uid: String)

    suspend fun pullCursor(collection: String): Long
    suspend fun setPullCursor(collection: String, millis: Long)
    suspend fun resetPullCursors()

    suspend fun setLastSyncAt(millis: Long)

    /** Account deletion: forget the uid, the cursors and the sync time. */
    suspend fun clearAll()
}

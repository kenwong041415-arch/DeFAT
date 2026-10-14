package com.defat.core.domain.sync

/**
 * The two timestamps that decide a conflict, for one document on one side.
 * [deletedAt] is the tombstone (§6.2): a delete is a write that sets it, never
 * a hard delete, so it propagates instead of being resurrected by the other
 * device's push.
 */
data class SyncStamp(val updatedAt: Long, val deletedAt: Long? = null) {
    /**
     * The moment of the last change of any kind. A delete normally bumps
     * `updatedAt` too, but taking the max means a tombstone can never look
     * older than the write it replaced, even if some writer forgot to.
     */
    val effectiveAt: Long get() = maxOf(updatedAt, deletedAt ?: Long.MIN_VALUE)
    val isDeleted: Boolean get() = deletedAt != null
}

enum class ConflictWinner { KEEP_LOCAL, TAKE_REMOTE }

/** Last-write-wins per document (phase1.5 plan D4 / §6.2). */
object ConflictResolution {

    /**
     * @param local the stamp of the row already on this phone, or null if there
     *   is no such row.
     * @param remote the stamp of the document that was just pulled.
     *
     * A newer remote wins and overwrites; **equal timestamps keep local**; a
     * tombstone is just a change like any other, so it beats an older live
     * write and loses to a newer one.
     */
    fun resolve(local: SyncStamp?, remote: SyncStamp): ConflictWinner = when {
        local == null -> ConflictWinner.TAKE_REMOTE
        remote.effectiveAt > local.effectiveAt -> ConflictWinner.TAKE_REMOTE
        else -> ConflictWinner.KEEP_LOCAL
    }
}

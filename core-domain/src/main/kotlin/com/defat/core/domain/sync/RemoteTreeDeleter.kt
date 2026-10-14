package com.defat.core.domain.sync

/**
 * Empties `users/{uid}` in the cloud (phase1.5 plan §7.1, risk R3).
 *
 * Firestore has no client-side recursive delete. This walks
 * [SYNCED_SUBCOLLECTIONS] — the same list [SyncEngine] is built from — so a
 * collection added for sync is deleted too, and then deletes the profile
 * document itself. It finishes by asking the server whether anything is left
 * and fails loudly if so: a silent leftover would be health data still in the
 * cloud after the user asked for it to be destroyed.
 */
object RemoteTreeDeleter {

    /** 10,000 pages of 400 is far beyond any real log; it only stops a runaway loop. */
    private const val MAX_PAGES_PER_COLLECTION = 10_000

    suspend fun deleteAll(remote: RemoteSyncStore) {
        for (name in SYNCED_SUBCOLLECTIONS) {
            var pages = 0
            while (true) {
                val ids = remote.listIds(name, PUSH_BATCH_SIZE)
                if (ids.isEmpty()) break
                remote.deleteDocs(name, ids)
                pages++
                check(pages <= MAX_PAGES_PER_COLLECTION) { "users/{uid}/$name did not empty" }
            }
        }
        remote.deleteDocs(SyncCollections.PROFILE, listOf(remote.uid))

        val leftovers = (SYNCED_SUBCOLLECTIONS + SyncCollections.PROFILE)
            .filter { remote.listIds(it, 1).isNotEmpty() }
        check(leftovers.isEmpty()) { "cloud data remains after deletion in: $leftovers" }
    }
}

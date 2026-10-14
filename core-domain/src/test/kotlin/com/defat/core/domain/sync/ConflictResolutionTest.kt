package com.defat.core.domain.sync

import kotlin.test.assertEquals
import org.junit.Test

class ConflictResolutionTest {

    private fun resolve(local: SyncStamp?, remote: SyncStamp) = ConflictResolution.resolve(local, remote)

    @Test
    fun `a newer remote wins`() {
        assertEquals(ConflictWinner.TAKE_REMOTE, resolve(SyncStamp(100), SyncStamp(101)))
    }

    @Test
    fun `an older remote loses`() {
        assertEquals(ConflictWinner.KEEP_LOCAL, resolve(SyncStamp(101), SyncStamp(100)))
    }

    @Test
    fun `equal timestamps keep local`() {
        assertEquals(ConflictWinner.KEEP_LOCAL, resolve(SyncStamp(100), SyncStamp(100)))
    }

    @Test
    fun `no local row takes the remote`() {
        assertEquals(ConflictWinner.TAKE_REMOTE, resolve(null, SyncStamp(1)))
    }

    @Test
    fun `a newer tombstone beats an older live write`() {
        assertEquals(
            ConflictWinner.TAKE_REMOTE,
            resolve(local = SyncStamp(updatedAt = 100), remote = SyncStamp(updatedAt = 200, deletedAt = 200)),
        )
    }

    @Test
    fun `a tombstone cannot look older than the write it replaced even if updatedAt was not bumped`() {
        // A writer that set deletedAt but forgot to bump updatedAt.
        assertEquals(
            ConflictWinner.TAKE_REMOTE,
            resolve(local = SyncStamp(updatedAt = 100), remote = SyncStamp(updatedAt = 50, deletedAt = 200)),
        )
    }

    @Test
    fun `a newer local edit survives an older remote tombstone`() {
        assertEquals(
            ConflictWinner.KEEP_LOCAL,
            resolve(local = SyncStamp(updatedAt = 300), remote = SyncStamp(updatedAt = 200, deletedAt = 200)),
        )
    }

    @Test
    fun `a local tombstone beats an older remote write`() {
        assertEquals(
            ConflictWinner.KEEP_LOCAL,
            resolve(local = SyncStamp(updatedAt = 200, deletedAt = 200), remote = SyncStamp(updatedAt = 100)),
        )
    }

    @Test
    fun `equal tombstone and write keep local`() {
        assertEquals(
            ConflictWinner.KEEP_LOCAL,
            resolve(local = SyncStamp(updatedAt = 200), remote = SyncStamp(updatedAt = 200, deletedAt = 200)),
        )
    }
}

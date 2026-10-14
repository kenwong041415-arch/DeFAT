package com.defat.core.domain.sync

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Case A — the adopt path, added in the Phase 1.5 Opus review pass.
 *
 * The plan's §6.3 guarded one direction of data loss: never let an empty cloud
 * erase the phone. The implementation pass pointed out the other direction,
 * which the plan missed: weights are keyed by date, water by day and the
 * profile is a single fixed document, so a phone used offline and signed in
 * later would blind-push over newer records made on a phone that was already
 * signed in. Both directions are now covered here.
 */
class AdoptLocalTest {

    private val meals = SyncCollections.MEALS
    private val weights = SyncCollections.WEIGHTS

    @Test
    fun `an empty cloud never removes a local row`() = runTest {
        val w = FakeWorld()
        w[meals].log("m1", updatedAt = 1_000L)
        w[meals].log("m2", updatedAt = 1_100L)

        w.engine().adoptLocal()

        assertEquals(2, w[meals].rows.size)
        assertEquals(2, w.remote.tree.getValue(meals).size)
    }

    @Test
    fun `local rows are uploaded even though the cloud is read for comparison`() = runTest {
        val w = FakeWorld()
        w[meals].log("m1", updatedAt = 1_000L)

        w.engine().adoptLocal()

        assertEquals(mapOf("n" to "m1"), w.remote.tree.getValue(meals).getValue("m1").doc.fields)
        assertTrue(w[meals].rows.getValue("m1").syncedAt != null, "the pushed row must not stay pending")
    }

    @Test
    fun `a newer cloud document is not overwritten by an older local row`() = runTest {
        val w = FakeWorld()
        // Same key on both sides — the 碟頭飯 logged on this phone at 09:00,
        // and a correction made on the signed-in phone at 11:00.
        w[weights].log("2026-10-14", updatedAt = 1_000L, fields = mapOf("kg" to 80.0))
        w.remote.put(weights, "2026-10-14", updatedAt = 2_000L, fields = mapOf("kg" to 79.4))

        w.engine().adoptLocal()

        assertEquals(
            mapOf("kg" to 79.4),
            w.remote.tree.getValue(weights).getValue("2026-10-14").doc.fields,
            "the newer cloud reading must survive the adopt",
        )
        assertEquals(
            mapOf("kg" to 79.4),
            w[weights].rows.getValue("2026-10-14").doc.fields,
            "and the phone must end up holding it too",
        )
    }

    @Test
    fun `an older cloud document is overwritten by a newer local row`() = runTest {
        val w = FakeWorld()
        w[weights].log("2026-10-14", updatedAt = 3_000L, fields = mapOf("kg" to 79.0))
        w.remote.put(weights, "2026-10-14", updatedAt = 2_000L, fields = mapOf("kg" to 80.0))

        w.engine().adoptLocal()

        assertEquals(mapOf("kg" to 79.0), w.remote.tree.getValue(weights).getValue("2026-10-14").doc.fields)
    }

    @Test
    fun `a held-back row does not stay pending once the cloud copy is applied`() = runTest {
        val w = FakeWorld()
        w[weights].log("2026-10-14", updatedAt = 1_000L)
        w.remote.put(weights, "2026-10-14", updatedAt = 2_000L)

        w.engine().adoptLocal()

        val row = w[weights].rows.getValue("2026-10-14")
        assertTrue(!row.isPending, "applying the remote copy must clear the pending flag")
    }

    @Test
    fun `cloud rows the phone never had are brought down`() = runTest {
        val w = FakeWorld()
        w[meals].log("m1", updatedAt = 1_000L)
        w.remote.put(meals, "from-other-phone", updatedAt = 2_000L)

        w.engine().adoptLocal()

        assertEquals(setOf("m1", "from-other-phone"), w[meals].rows.keys)
    }

    @Test
    fun `the pull cursor advances so the next pass does not refetch everything`() = runTest {
        val w = FakeWorld()
        w[meals].log("m1", updatedAt = 1_000L)
        w.remote.put(meals, "other", updatedAt = 2_000L)

        w.engine().adoptLocal()

        assertTrue(w.state.pullCursor(meals) > 0L)
    }

    @Test
    fun `a tombstone for a row this phone never had is not resurrected as a local row`() = runTest {
        val w = FakeWorld()
        w[meals].log("m1", updatedAt = 1_000L)
        w.remote.put(meals, "deleted-elsewhere", updatedAt = 2_000L, deletedAt = 2_000L)

        w.engine().adoptLocal()

        assertNull(w[meals].rows["deleted-elsewhere"])
    }
}

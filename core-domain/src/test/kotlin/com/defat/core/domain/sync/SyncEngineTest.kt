package com.defat.core.domain.sync

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncEngineTest {

    // ---- push batching (plan §6.1: batches of 400) ----

    private suspend fun pushMeals(count: Int): FakeWorld {
        val world = FakeWorld()
        repeat(count) { world[SyncCollections.MEALS].log("m$it") }
        world.engine().pushPending()
        return world
    }

    @Test
    fun `399 pending rows go up in one batch`() = runTest {
        val world = pushMeals(399)
        assertEquals(listOf(399), world.remote.batchSizes)
    }

    @Test
    fun `exactly 400 pending rows still fit in one batch`() = runTest {
        val world = pushMeals(400)
        assertEquals(listOf(400), world.remote.batchSizes)
    }

    @Test
    fun `401 pending rows split into 400 and 1`() = runTest {
        val world = pushMeals(401)
        assertEquals(listOf(400, 1), world.remote.batchSizes)
        assertEquals(401, world.remote.totalDocs())
    }

    @Test
    fun `800 and 801 rows split on the 400 boundary`() = runTest {
        assertEquals(listOf(400, 400), pushMeals(800).remote.batchSizes)
        assertEquals(listOf(400, 400, 1), pushMeals(801).remote.batchSizes)
    }

    @Test
    fun `a batch can span collections and the limit counts all of them`() = runTest {
        val world = FakeWorld()
        repeat(300) { world[SyncCollections.MEALS].log("m$it") }
        repeat(300) { world[SyncCollections.WEIGHTS].log("w$it") }
        world.engine().pushPending()
        assertEquals(listOf(400, 200), world.remote.batchSizes)
        assertEquals(300, world.remote.tree.getValue(SyncCollections.MEALS).size)
        assertEquals(300, world.remote.tree.getValue(SyncCollections.WEIGHTS).size)
    }

    @Test
    fun `nothing pending means no network call at all`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].logSynced("m1")
        assertEquals(0, world.engine().pushPending())
        assertTrue(world.remote.calls.isEmpty())
    }

    @Test
    fun `pushed rows are stamped synced and are no longer pending`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("m1", updatedAt = 5)
        world.engine().pushPending()
        assertTrue(world[SyncCollections.MEALS].rows.getValue("m1").syncedAt == 5L)
        assertTrue(world[SyncCollections.MEALS].pending().isEmpty())
    }

    @Test
    fun `a failed batch leaves the rows pending and the earlier batch stamped`() = runTest {
        val world = FakeWorld()
        repeat(450) { world[SyncCollections.MEALS].log("m${it.toString().padStart(3, '0')}") }
        val flaky = object : RemoteSyncStore by world.remote {
            var n = 0
            override suspend fun writeBatch(items: List<CollectionDoc>) {
                if (++n == 2) error("network dropped")
                world.remote.writeBatch(items)
            }
        }
        val engine = SyncEngine(world.sources.values.toList(), flaky, world.state)
        assertFailsWith<IllegalStateException> { engine.pushPending() }
        assertEquals(50, world[SyncCollections.MEALS].pending().size)
        assertEquals(400, world.remote.totalDocs())
    }

    @Test
    fun `an edit made while the upload is in flight stays pending`() = runTest {
        val world = FakeWorld()
        val meals = world[SyncCollections.MEALS]
        meals.log("m1", updatedAt = 100)
        val inFlight = meals.pending().single()
        // The user edits the meal after the batch was read but before it was stamped.
        meals.log("m1", updatedAt = 105)
        meals.markSynced(inFlight)
        assertEquals(listOf(105L), meals.pending().map { it.updatedAt })
    }

    @Test
    fun `tombstones are pushed like any other write`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("m1", updatedAt = 200, deletedAt = 200)
        world.engine().pushPending()
        assertEquals(200L, world.remote.tree.getValue(SyncCollections.MEALS).getValue("m1").doc.deletedAt)
    }

    // ---- pull ----

    @Test
    fun `pull applies a newer remote document and keeps a newer local one`() = runTest {
        val world = FakeWorld()
        val meals = world[SyncCollections.MEALS]
        meals.logSynced("older-locally", updatedAt = 100)
        meals.logSynced("newer-locally", updatedAt = 300)
        world.remote.put(SyncCollections.MEALS, "older-locally", updatedAt = 200)
        world.remote.put(SyncCollections.MEALS, "newer-locally", updatedAt = 200)

        val applied = world.engine().pullSince()

        assertEquals(1, applied)
        assertEquals(200L, meals.rows.getValue("older-locally").doc.updatedAt)
        assertEquals(300L, meals.rows.getValue("newer-locally").doc.updatedAt)
    }

    @Test
    fun `a pulled tombstone deletes the local row`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].logSynced("m1", updatedAt = 100)
        world.remote.put(SyncCollections.MEALS, "m1", updatedAt = 200, deletedAt = 200)

        world.engine().pullSince()

        assertEquals(200L, world[SyncCollections.MEALS].rows.getValue("m1").doc.deletedAt)
        assertTrue(world[SyncCollections.MEALS].liveDocs().isEmpty())
    }

    @Test
    fun `a pulled tombstone for a row this phone never had is not stored`() = runTest {
        val world = FakeWorld()
        world.remote.put(SyncCollections.MEALS, "ghost", updatedAt = 200, deletedAt = 200)
        world.engine().pullSince()
        assertFalse(world.engine().hasLocalRows())
    }

    @Test
    fun `the cursor advances so the next pull fetches only newer documents`() = runTest {
        val world = FakeWorld()
        world.remote.put(SyncCollections.MEALS, "a", updatedAt = 1)
        val engine = world.engine()
        engine.pullSince()
        val cursor = world.state.cursors.getValue(SyncCollections.MEALS)

        world.remote.put(SyncCollections.MEALS, "b", updatedAt = 2)
        world.remote.calls.clear()
        engine.pullSince()

        assertTrue(world.state.cursors.getValue(SyncCollections.MEALS) > cursor)
        assertEquals(setOf("a", "b"), world[SyncCollections.MEALS].rows.keys)
    }

    @Test
    fun `the cursor boundary is inclusive so a same-millisecond commit is not skipped`() = runTest {
        val world = FakeWorld()
        world.remote.put(SyncCollections.MEALS, "a", updatedAt = 1)
        world.engine().pullSince()
        val cursor = world.state.cursors.getValue(SyncCollections.MEALS)
        // A second document committed in the very same server millisecond.
        world.remote.tree.getValue(SyncCollections.MEALS)["b"] =
            RemoteDoc(SyncDoc("b", 2, null, mapOf("n" to "b")), serverUpdatedAt = cursor)

        world.engine().pullSince()

        assertTrue("b" in world[SyncCollections.MEALS].rows)
    }

    @Test
    fun `pulling from an empty cloud deletes nothing locally`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("m1")
        world[SyncCollections.WEIGHTS].log("2026-03-05")
        world.engine().pullSince()
        world.engine().fullPull()
        assertEquals(2, world.localRowCount())
    }

    @Test
    fun `a pull failure does not advance that collection's cursor`() = runTest {
        val world = FakeWorld()
        world.remote.put(SyncCollections.MEALS, "a", updatedAt = 1)
        world.remote.failReads = true
        assertFailsWith<IllegalStateException> { world.engine().pullSince() }
        assertNull(world.state.cursors[SyncCollections.MEALS])
    }

    // ---- replace (Case C, after the user said Replace) ----

    @Test
    fun `replace downloads before it wipes, so a failed download keeps the local rows`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("mine")
        world.remote.failReads = true
        assertFailsWith<IllegalStateException> { world.engine().replaceLocalWithRemote() }
        assertEquals(setOf("mine"), world[SyncCollections.MEALS].rows.keys)
    }

    @Test
    fun `replace swaps the local rows for the account's rows`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("old-account-meal")
        world.remote.put(SyncCollections.MEALS, "new-account-meal", updatedAt = 10)

        world.engine().replaceLocalWithRemote()

        assertEquals(setOf("new-account-meal"), world[SyncCollections.MEALS].rows.keys)
        assertTrue(world[SyncCollections.MEALS].pending().isEmpty())
    }

    // ---- the list drives the engine ----

    @Test
    fun `the engine refuses to start with a source missing`() {
        val world = FakeWorld()
        val withoutWater = world.sources.values.filter { it.name != SyncCollections.WATER }
        assertFailsWith<IllegalArgumentException> { SyncEngine(withoutWater, world.remote, world.state) }
    }

    @Test
    fun `the engine refuses an unknown source`() {
        val world = FakeWorld()
        val extra = world.sources.values + FakeSource("somethingNew")
        assertFailsWith<IllegalArgumentException> { SyncEngine(extra.toList(), world.remote, world.state) }
    }
}

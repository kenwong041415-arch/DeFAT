package com.defat.core.domain.sync

import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.SyncAccountState
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The first-sign-in tests. Every one that matters here runs against a
 * POPULATED local store: proving Case A on an empty phone proves nothing.
 */
class SyncCoordinatorTest {

    /** The owner's phone: real logged data across every collection, never uploaded. */
    private fun ownersPhone(uid: String = "owner", lastUid: String? = null): FakeWorld {
        val world = FakeWorld(uid = uid, lastUid = lastUid)
        repeat(5) { world[SyncCollections.MEALS].log("meal-$it", updatedAt = 1_000L + it) }
        repeat(3) { world[SyncCollections.WEIGHTS].log("2026-03-0${it + 1}", updatedAt = 2_000L + it) }
        world[SyncCollections.EXERCISE].log("run-1", updatedAt = 3_000)
        world[SyncCollections.WATER].log("20526", updatedAt = 4_000)
        world[SyncCollections.FOOD_OVERRIDES].log("cha_siu_rice", updatedAt = 5_000)
        world[SyncCollections.PROFILE].log("profile", updatedAt = 6_000)
        return world
    }

    // ---- Case A ----

    @Test
    fun `case A - the owner's first sign-in uploads every local row and loses none`() = runTest {
        val world = ownersPhone()
        val before = world.localRowCount()
        assertEquals(12, before)

        val outcome = world.coordinator().syncNow()

        assertIs<SyncOutcome.Synced>(outcome)
        assertEquals(before, world.localRowCount(), "no local row may disappear")
        assertEquals(before, world.remote.totalDocs(), "every local row must reach the cloud")
        assertTrue(world.sources.values.all { it.pending().isEmpty() })
        assertEquals("owner", world.state.getLastSignedInUid())
    }

    @Test
    fun `case A - reading the cloud never costs the phone a row`() = runTest {
        // This test used to assert the stricter mechanism "no read happens until
        // every row has been pushed". The Opus review pass replaced that with the
        // property the mechanism was standing in for, because adoptLocal now does
        // read the cloud first — to compare stamps, so it does not blind-push over
        // a newer remote document (weights are keyed by date, water by day, the
        // profile is one document). Reading is safe; *acting* on an empty or older
        // cloud is what is not, and that is what this asserts. The two tests
        // either side of it cover the same property from the other directions.
        val world = ownersPhone()
        val before = world.localRowCount()

        world.coordinator().syncNow()

        assertTrue(world.remote.calls.none { it.startsWith("wipe:") }, world.remote.calls.toString())
        assertTrue(world.log.none { it.startsWith("wipe:") }, "no collection may be cleared in Case A: ${world.log}")
        assertEquals(before, world.localRowCount(), "the comparison read must not cost a local row")
        assertEquals(before, world.remote.totalDocs(), "and every local row must still reach the cloud")
    }

    @Test
    fun `case A - an empty cloud is never treated as the truth`() = runTest {
        val world = ownersPhone()
        assertEquals(0, world.remote.totalDocs())

        world.coordinator().syncNow()
        // A second pass, now with the cloud holding what we pushed, must still lose nothing.
        world.coordinator().syncNow()

        assertEquals(12, world.localRowCount())
        assertTrue(world.log.none { it.startsWith("wipe:") }, "nothing may be wiped: ${world.log}")
    }

    @Test
    fun `case A - rows that were already marked synced are re-pushed because nobody has signed in`() = runTest {
        val world = ownersPhone()
        // A stale syncedAt (e.g. left from an earlier build) must not hide a row from adoption.
        world[SyncCollections.MEALS].logSynced("stale", updatedAt = 10)

        world.coordinator().syncNow()

        assertTrue("stale" in world.remote.tree.getValue(SyncCollections.MEALS))
    }

    @Test
    fun `case A - a failed push does not record the uid, so the next pass adopts again`() = runTest {
        val world = ownersPhone()
        world.remote.failWrites = true

        val outcome = world.coordinator().syncNow()

        assertIs<SyncOutcome.Failed>(outcome)
        assertNull(world.state.getLastSignedInUid())
        assertEquals(12, world.localRowCount())

        world.remote.failWrites = false
        assertIs<SyncOutcome.Synced>(world.coordinator().syncNow())
        assertEquals(12, world.remote.totalDocs())
    }

    @Test
    fun `case A - if the cloud already has this account's data it is merged in after the push`() = runTest {
        val world = ownersPhone()
        world.remote.put(SyncCollections.MEALS, "from-other-phone", updatedAt = 99_000)

        world.coordinator().syncNow()

        assertTrue("from-other-phone" in world[SyncCollections.MEALS].rows)
        assertEquals(6, world[SyncCollections.MEALS].rows.size)
    }

    // ---- Case B ----

    @Test
    fun `case B - same account pulls then pushes`() = runTest {
        val world = ownersPhone(lastUid = "owner")
        world[SyncCollections.MEALS].rows.replaceAll { _, row -> row.copy(syncedAt = row.doc.updatedAt) }
        world[SyncCollections.MEALS].log("new-meal", updatedAt = 9_000)
        world.remote.put(SyncCollections.MEALS, "from-phone-2", updatedAt = 8_000)

        world.coordinator().syncNow()

        val firstRead = world.remote.calls.indexOfFirst { it.startsWith("read:") }
        val firstWrite = world.remote.calls.indexOfFirst { it.startsWith("write:") }
        assertTrue(firstRead in 0 until firstWrite, "case B pulls before it pushes: ${world.remote.calls}")
        assertTrue("from-phone-2" in world[SyncCollections.MEALS].rows)
        assertTrue("new-meal" in world.remote.tree.getValue(SyncCollections.MEALS))
    }

    @Test
    fun `case B - a stale offline edit does not overwrite a newer edit from the other phone`() = runTest {
        val world = FakeWorld(lastUid = "uid-1")
        world[SyncCollections.MEALS].log("m1", updatedAt = 100, fields = mapOf("n" to "stale"))
        world.remote.put(SyncCollections.MEALS, "m1", updatedAt = 200, fields = mapOf("n" to "newer"))

        world.coordinator().syncNow()

        assertEquals("newer", world.remote.tree.getValue(SyncCollections.MEALS).getValue("m1").doc.fields["n"])
        assertEquals("newer", world[SyncCollections.MEALS].rows.getValue("m1").doc.fields["n"])
    }

    // ---- download only ----

    @Test
    fun `a second device with nothing local downloads everything`() = runTest {
        val world = FakeWorld()
        world.remote.put(SyncCollections.MEALS, "m1", updatedAt = 1)
        world.remote.put(SyncCollections.WEIGHTS, "2026-03-05", updatedAt = 2)
        world.remote.put(SyncCollections.PROFILE, "profile", updatedAt = 3)

        world.coordinator().syncNow()

        assertEquals(3, world.localRowCount())
        assertEquals("uid-1", world.state.getLastSignedInUid())
    }

    // ---- Case C ----

    @Test
    fun `case C - another account's rows are not touched, uploaded or downloaded until the user chooses`() = runTest {
        val world = ownersPhone(uid = "student-b", lastUid = "student-a")
        world.remote.put(SyncCollections.MEALS, "b-cloud-meal", updatedAt = 1)
        world.remote.calls.clear()

        val outcome = world.coordinator().syncNow()

        assertIs<SyncOutcome.NeedsReplaceConfirmation>(outcome)
        assertEquals(12, world.localRowCount())
        assertTrue(world.remote.calls.isEmpty(), "case C must not talk to the cloud: ${world.remote.calls}")
        assertTrue(world.log.none { it.startsWith("wipe:") })
        assertEquals("student-a", world.state.getLastSignedInUid())
        assertEquals(0, world.remote.batchSizes.size, "student A's rows must never be uploaded into B's account")
    }

    @Test
    fun `case C - a background pass also leaves the other account's rows alone`() = runTest {
        val world = ownersPhone(uid = "student-b", lastUid = "student-a")
        val coordinator = world.coordinator()
        repeat(3) { coordinator.syncNow() }
        assertEquals(12, world.localRowCount())
        assertEquals(0, world.remote.totalDocs())
    }

    @Test
    fun `case C - Replace swaps in the new account's data`() = runTest {
        val world = ownersPhone(uid = "student-b", lastUid = "student-a")
        world.remote.put(SyncCollections.MEALS, "b-cloud-meal", updatedAt = 1)

        val outcome = world.coordinator().confirmReplace()

        assertIs<SyncOutcome.Synced>(outcome)
        assertEquals(setOf("b-cloud-meal"), world[SyncCollections.MEALS].rows.keys)
        assertEquals(1, world.localRowCount())
        assertEquals("student-b", world.state.getLastSignedInUid())
        assertEquals(1, world.remote.totalDocs(), "A's rows were not uploaded to B")
    }

    @Test
    fun `confirmReplace from a stale dialog wipes nothing when the situation is no longer case C`() = runTest {
        // Same account as last time: a stray Replace tap must not destroy anything.
        val world = ownersPhone(uid = "owner", lastUid = "owner")

        world.coordinator().confirmReplace()

        assertEquals(12, world.localRowCount())
        assertTrue(world.log.none { it.startsWith("wipe:") })
    }

    @Test
    fun `confirmReplace on a never-adopted phone adopts instead of wiping`() = runTest {
        val world = ownersPhone(uid = "owner", lastUid = null)

        world.coordinator().confirmReplace()

        assertEquals(12, world.localRowCount())
        assertEquals(12, world.remote.totalDocs())
    }

    // ---- misc ----

    @Test
    fun `signed out means no sync and no cloud calls`() = runTest {
        val world = ownersPhone()
        world.auth.signOut()

        assertIs<SyncOutcome.NotSignedIn>(world.coordinator().syncNow())
        assertTrue(world.remote.calls.isEmpty())
    }

    @Test
    fun `a successful pass records the sync time and a failed one marks the status`() = runTest {
        val world = FakeWorld(lastUid = "uid-1")
        val coordinator = world.coordinator()

        world.remote.failReads = true
        coordinator.syncNow()
        assertTrue(coordinator.status.first().lastFailed)
        assertNull(coordinator.status.first().lastSyncAt)

        world.remote.failReads = false
        world.nowMillis = 77_000L
        coordinator.syncNow()
        val status = coordinator.status.first()
        assertEquals(77_000L, status.lastSyncAt)
        assertEquals(false, status.lastFailed)
    }

    @Test
    fun `accountState tells a never-synced phone from the same account from another account`() = runTest {
        val world = FakeWorld(uid = "uid-1")
        val coordinator = world.coordinator()
        assertEquals(SyncAccountState.NEVER_SYNCED, coordinator.accountState.first())

        coordinator.syncNow()
        assertEquals(SyncAccountState.SAME_ACCOUNT, coordinator.accountState.first())

        world.state.setLastSignedInUid("someone-else")
        assertEquals(SyncAccountState.OTHER_ACCOUNT, coordinator.accountState.first())
    }

    // ---- account deletion ----

    private suspend fun populatedCloud(world: FakeWorld) {
        for (name in SYNCED_SUBCOLLECTIONS) {
            repeat(450) { world.remote.put(name, "doc-$it", updatedAt = 1) }
        }
        world.remote.put(SyncCollections.PROFILE, world.remote.uid, updatedAt = 1)
    }

    @Test
    fun `deleting the account empties every subcollection and the profile document`() = runTest {
        val world = ownersPhone(lastUid = "owner", uid = "owner")
        populatedCloud(world)
        assertTrue(world.remote.totalDocs() > 0)

        val result = world.coordinator().deleteAccount("hunter22")

        assertEquals(DeleteAccountResult.Deleted, result)
        assertEquals(0, world.remote.totalDocs(), "the cloud tree must be empty: ${world.remote.tree.mapValues { it.value.size }}")
        assertEquals(0, world.localRowCount())
        assertNull(world.state.getLastSignedInUid())
        assertNull(world.auth.currentUserNow())
    }

    @Test
    fun `deletion order is reauthenticate then cloud then auth user then phone`() = runTest {
        val world = ownersPhone(lastUid = "owner", uid = "owner")
        val authAtCloudDelete = mutableListOf<Boolean>()
        val watching = object : RemoteSyncStore by world.remote {
            override suspend fun deleteDocs(collection: String, ids: List<String>) {
                authAtCloudDelete += (world.auth.currentUserNow() != null)
                world.remote.deleteDocs(collection, ids)
            }
        }
        world.remote.put(SyncCollections.MEALS, "m", updatedAt = 1)
        val coordinator = DefaultSyncCoordinator(
            auth = world.auth,
            sources = world.sources.values.toList(),
            remoteFor = { watching },
            state = world.state,
            nowMillis = { 1L },
        )

        coordinator.deleteAccount("pw")

        assertEquals(listOf("reauth", "deleteAuth"), world.auth.calls)
        assertTrue(authAtCloudDelete.isNotEmpty() && authAtCloudDelete.all { it }, "cloud must be deleted while still signed in")
    }

    @Test
    fun `a wrong password deletes nothing anywhere`() = runTest {
        val world = ownersPhone(lastUid = "owner", uid = "owner")
        populatedCloud(world)
        val cloudBefore = world.remote.totalDocs()
        world.auth.reauthResult = AuthError.WRONG_CREDENTIALS

        val result = world.coordinator().deleteAccount("wrong")

        assertEquals(DeleteAccountResult.AuthFailed(AuthError.WRONG_CREDENTIALS), result)
        assertEquals(cloudBefore, world.remote.totalDocs())
        assertEquals(12, world.localRowCount())
        assertTrue(world.remote.calls.isEmpty())
    }

    @Test
    fun `a cloud failure leaves the account and the phone intact and is safe to retry`() = runTest {
        val world = ownersPhone(lastUid = "owner", uid = "owner")
        populatedCloud(world)
        world.remote.failDeletes = true

        val result = world.coordinator().deleteAccount("pw")

        assertEquals(DeleteAccountResult.CloudDeleteFailed, result)
        assertTrue(world.auth.currentUserNow() != null)
        assertEquals(12, world.localRowCount())
        assertEquals("owner", world.state.getLastSignedInUid())

        world.remote.failDeletes = false
        assertEquals(DeleteAccountResult.Deleted, world.coordinator().deleteAccount("pw"))
    }

    @Test
    fun `a collection that survives deletion is reported, never swallowed`() = runTest {
        val world = ownersPhone(lastUid = "owner", uid = "owner")
        populatedCloud(world)
        world.remote.undeletable += SyncCollections.WATER

        val result = world.coordinator().deleteAccount("pw")

        assertEquals(DeleteAccountResult.CloudDeleteFailed, result)
        assertTrue(world.auth.currentUserNow() != null, "the Auth user must survive a failed cloud wipe")
    }
}

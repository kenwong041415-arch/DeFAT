package com.defat.core.domain.sync

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoteTreeDeleterTest {

    @Test
    fun `every name in SYNCED_SUBCOLLECTIONS is emptied, including ones past a single page`() = runTest {
        val remote = FakeRemote()
        for (name in SYNCED_SUBCOLLECTIONS) {
            repeat(PUSH_BATCH_SIZE + 1) { remote.put(name, "d$it", updatedAt = 1) }
        }
        remote.put(SyncCollections.PROFILE, remote.uid, updatedAt = 1)

        RemoteTreeDeleter.deleteAll(remote)

        assertEquals(0, remote.totalDocs())
        for (name in SYNCED_SUBCOLLECTIONS) {
            assertTrue(remote.listIds(name, 1).isEmpty(), "users/{uid}/$name still has documents")
        }
    }

    @Test
    fun `a subcollection added to the list later is deleted with no other change`() = runTest {
        // The point of risk R3: delete reads the same list as sync. Whatever the
        // list contains is walked; there is no second place to forget.
        val remote = FakeRemote()
        val walked = mutableListOf<String>()
        val spying = object : RemoteSyncStore by remote {
            override suspend fun listIds(collection: String, limit: Int): List<String> {
                walked += collection
                return remote.listIds(collection, limit)
            }
        }
        RemoteTreeDeleter.deleteAll(spying)
        assertTrue(walked.toSet().containsAll(SYNCED_SUBCOLLECTIONS), "walked $walked")
    }

    @Test
    fun `a leftover document makes the delete fail loudly`() = runTest {
        val remote = FakeRemote()
        remote.put(SyncCollections.MEALS, "m1", updatedAt = 1)
        remote.undeletable += SyncCollections.MEALS
        // The undeletable collection would loop; the guard turns it into an error.
        assertFailsWith<IllegalStateException> { RemoteTreeDeleter.deleteAll(remote) }
    }

    @Test
    fun `the profile document is deleted by uid`() = runTest {
        val remote = FakeRemote(uid = "abc")
        remote.put(SyncCollections.PROFILE, "abc", updatedAt = 1)
        RemoteTreeDeleter.deleteAll(remote)
        assertFalse(remote.tree.getValue(SyncCollections.PROFILE).containsKey("abc"))
    }
}

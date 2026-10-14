package com.defat.core.domain.sync

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SyncCollectionsTest {

    @Test
    fun `the list has no duplicates and does not contain the profile pseudo-collection`() {
        assertEquals(SYNCED_SUBCOLLECTIONS.size, SYNCED_SUBCOLLECTIONS.toSet().size)
        assertFalse(SyncCollections.PROFILE in SYNCED_SUBCOLLECTIONS)
    }

    @Test
    fun `every name is a legal single Firestore collection id`() {
        // No slash (would nest a path), no leading double underscore (reserved).
        for (name in SYNCED_SUBCOLLECTIONS) {
            assertTrue(Regex("^[A-Za-z][A-Za-z0-9]*$").matches(name), name)
        }
    }

    @Test
    fun `the meals weights exercise water and food overrides of plan section 4 are all listed`() {
        assertTrue(
            SYNCED_SUBCOLLECTIONS.containsAll(
                listOf("meals", "weights", "exercise", "water", "foodOverrides", "days"),
            ),
        )
    }

    @Test
    fun `not-yet-written names are a subset of the list`() {
        assertTrue(SYNCED_SUBCOLLECTIONS.containsAll(SUBCOLLECTIONS_NOT_YET_WRITTEN))
    }
}

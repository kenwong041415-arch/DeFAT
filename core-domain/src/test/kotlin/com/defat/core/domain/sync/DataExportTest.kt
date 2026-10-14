package com.defat.core.domain.sync

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DataExportTest {

    private val at = Instant.parse("2026-10-14T06:00:00Z")

    @Test
    fun `the export carries every live row and omits tombstones`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("m1", fields = mapOf("name" to "雞胸飯", "kcal" to 520.5))
        world[SyncCollections.MEALS].log("m2", updatedAt = 5, deletedAt = 5, fields = mapOf("name" to "deleted one"))
        world[SyncCollections.WEIGHTS].log("2026-03-05", fields = mapOf("weightKg" to 79.4, "fatPct" to null))
        world[SyncCollections.PROFILE].log("profile", fields = mapOf("heightCm" to 175.0, "goal" to mapOf("targetWeightKg" to 75.0)))

        val json = DataExport.build(world.sources.values.toList(), at, "0.4.0", "a@b.com")

        assertTrue("\"format\": \"defat-export\"" in json)
        assertTrue("\"exportedAt\": \"2026-10-14T06:00:00Z\"" in json)
        assertTrue("雞胸飯" in json)
        assertTrue("\"id\": \"m1\"" in json)
        assertFalse("deleted one" in json)
        assertFalse("\"id\": \"m2\"" in json)
        assertTrue("\"targetWeightKg\": 75.0" in json)
        assertTrue("\"fatPct\": null" in json)
        assertTrue("\"account\": \"a@b.com\"" in json)
    }

    @Test
    fun `every synced collection has a key in the export even when empty`() = runTest {
        val world = FakeWorld()
        val json = DataExport.build(world.sources.values.toList(), at, "0.4.0", null)
        for (name in SYNCED_SUBCOLLECTIONS.filter { it !in SUBCOLLECTIONS_NOT_YET_WRITTEN }) {
            assertTrue("\"$name\": []" in json, "missing $name in $json")
        }
        assertTrue("\"profile\": null" in json)
    }

    @Test
    fun `export reads local data only and never touches the cloud`() = runTest {
        val world = FakeWorld()
        world[SyncCollections.MEALS].log("m1")
        DataExport.build(world.sources.values.toList(), at, "0.4.0", null)
        assertTrue(world.remote.calls.isEmpty())
    }
}

class JsonWriterTest {

    @Test
    fun `strings are escaped`() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\"", JsonWriter.write("a\"b\\c\n\t"))
        assertEquals("\"\\u0001\"", JsonWriter.write("\u0001"))
    }

    @Test
    fun `Chinese text passes through unescaped`() {
        assertEquals("\"叉燒飯\"", JsonWriter.write("叉燒飯"))
    }

    @Test
    fun `numbers booleans and null`() {
        assertEquals("520.0", JsonWriter.write(520.0))
        assertEquals("79.4", JsonWriter.write(79.4))
        assertEquals("1700000000000", JsonWriter.write(1_700_000_000_000L))
        assertEquals("true", JsonWriter.write(true))
        assertEquals("null", JsonWriter.write(null))
        assertEquals("null", JsonWriter.write(Double.NaN))
        assertEquals("null", JsonWriter.write(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `maps keep insertion order and empty containers are compact`() {
        val json = JsonWriter.write(linkedMapOf("b" to 1, "a" to emptyList<Any>(), "c" to emptyMap<String, Any>()))
        assertEquals("{\n  \"b\": 1,\n  \"a\": [],\n  \"c\": {}\n}", json)
    }
}

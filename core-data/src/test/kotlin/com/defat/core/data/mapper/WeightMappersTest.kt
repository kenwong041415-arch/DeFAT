package com.defat.core.data.mapper

import com.defat.core.data.db.entity.WeightEntryEntity
import com.defat.core.domain.model.MeasurementSource
import com.defat.core.domain.model.WeightEntry
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class WeightMappersTest {

    @Test
    fun `round-trip preserves every field`() {
        val entry = WeightEntry(
            date = LocalDate.of(2026, 3, 15),
            weightKg = 79.4,
            bodyFatPct = 24.1,
            recordedAt = Instant.parse("2026-03-15T07:00:00Z"),
            source = MeasurementSource.MANUAL,
        )

        val entity = entry.toEntity(updatedAtMillis = 456L)
        val roundTripped = entity.toDomain()

        assertEquals(entry, roundTripped)
    }

    @Test
    fun `round-trip with null bodyFatPct`() {
        val entry = WeightEntry(
            date = LocalDate.of(2026, 3, 16),
            weightKg = 79.0,
            bodyFatPct = null,
            recordedAt = Instant.parse("2026-03-16T07:00:00Z"),
        )

        val roundTripped = entry.toEntity().toDomain()

        assertEquals(entry, roundTripped)
        assertNull(roundTripped.bodyFatPct)
    }

    @Test
    fun `unknown enum name falls back to MANUAL rather than throwing`() {
        val entity = WeightEntryEntity(
            date = "2026-01-01",
            weightKg = 80.0,
            bodyFatPct = null,
            recordedAtMillis = 0L,
            source = "SOME_FUTURE_SOURCE",
            updatedAtMillis = 0L,
        )
        assertEquals(MeasurementSource.MANUAL, entity.toDomain().source)
    }
}

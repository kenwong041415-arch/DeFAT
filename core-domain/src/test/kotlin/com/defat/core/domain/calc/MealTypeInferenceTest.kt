package com.defat.core.domain.calc

import com.defat.core.domain.model.MealType
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class MealTypeInferenceTest {

    @Test
    fun `midnight and the small hours are breakfast`() {
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(0, 0)))
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(3, 0)))
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(7, 30)))
    }

    @Test
    fun `boundaries are half open`() {
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(10, 29)))
        assertEquals(MealType.LUNCH, MealTypeInference.forTime(LocalTime.of(10, 30)))
        assertEquals(MealType.LUNCH, MealTypeInference.forTime(LocalTime.of(14, 29)))
        assertEquals(MealType.AFTERNOON_TEA, MealTypeInference.forTime(LocalTime.of(14, 30)))
        assertEquals(MealType.AFTERNOON_TEA, MealTypeInference.forTime(LocalTime.of(17, 29)))
        assertEquals(MealType.DINNER, MealTypeInference.forTime(LocalTime.of(17, 30)))
        assertEquals(MealType.DINNER, MealTypeInference.forTime(LocalTime.of(20, 59)))
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(21, 0)))
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(23, 59)))
    }

    @Test
    fun `midday and evening land in the obvious buckets`() {
        assertEquals(MealType.LUNCH, MealTypeInference.forTime(LocalTime.of(12, 0)))
        assertEquals(MealType.AFTERNOON_TEA, MealTypeInference.forTime(LocalTime.of(16, 0)))
        assertEquals(MealType.DINNER, MealTypeInference.forTime(LocalTime.of(19, 0)))
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(22, 30)))
    }

    @Test
    fun `forMinuteOfDay agrees with forTime for every minute`() {
        for (m in 0..1439) {
            val expected = MealTypeInference.forTime(LocalTime.of(m / 60, m % 60))
            assertEquals(expected, MealTypeInference.forMinuteOfDay(m), "minute $m")
        }
    }

    @Test
    fun `rejects a minute outside the day`() {
        assertFailsWith<IllegalArgumentException> { MealTypeInference.forMinuteOfDay(-1) }
        assertFailsWith<IllegalArgumentException> { MealTypeInference.forMinuteOfDay(1440) }
    }

    // --- day-start-aware overload (Phase 1.7) ---

    @Test
    fun `before the day start is a late-night snack, not breakfast`() {
        val dayStart = LocalTime.of(4, 0)
        // The frozen rule calls all of these BREAKFAST, which would merge a
        // 01:30 bowl of noodles into the previous day's 早餐 section.
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(0, 0), dayStart))
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(1, 30), dayStart))
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(3, 59), dayStart))
    }

    @Test
    fun `the day start itself is breakfast`() {
        val dayStart = LocalTime.of(4, 0)
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(4, 0), dayStart))
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(4, 1), dayStart))
    }

    @Test
    fun `after the day start the frozen boundaries still apply`() {
        val dayStart = LocalTime.of(4, 0)
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(8, 0), dayStart))
        assertEquals(MealType.LUNCH, MealTypeInference.forTime(LocalTime.of(12, 0), dayStart))
        assertEquals(MealType.AFTERNOON_TEA, MealTypeInference.forTime(LocalTime.of(16, 0), dayStart))
        assertEquals(MealType.DINNER, MealTypeInference.forTime(LocalTime.of(19, 0), dayStart))
        assertEquals(MealType.SNACK, MealTypeInference.forTime(LocalTime.of(22, 30), dayStart))
    }

    @Test
    fun `a midnight day start reproduces the frozen rule for every minute`() {
        val midnight = LocalTime.MIDNIGHT
        for (m in 0..1439) {
            val time = LocalTime.of(m / 60, m % 60)
            assertEquals(
                MealTypeInference.forTime(time),
                MealTypeInference.forTime(time, midnight),
                "minute $m",
            )
        }
    }

    @Test
    fun `the frozen rule is unchanged, so the shipped migration still agrees`() {
        // MealMigrations back-fills v1 rows with these boundaries and
        // MealTypeMigrationSqlTest proves SQL == Kotlin for all 1440 minutes.
        // If this ever fails, the day-start overload has leaked into the
        // frozen path and the migration no longer matches the code.
        assertEquals(MealType.BREAKFAST, MealTypeInference.forTime(LocalTime.of(3, 0)))
        assertEquals(MealType.BREAKFAST, MealTypeInference.forMinuteOfDay(180))
    }
}

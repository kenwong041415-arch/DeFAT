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
}

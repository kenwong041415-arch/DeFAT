package com.defat.core.domain.calc

import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.TdeeSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

/**
 * Plan §15.3. BMR 1800, MODERATE (x 1.55) -> baseline 2790; the logged-day
 * term is 1800 x 1.1 = 1980 plus the exercise.
 */
class DayTdeeCalculatorTest {

    private val bmr = 1800.0
    private val moderate = ActivityLevel.MODERATE

    @Test
    fun `no exercise gives the multiplier result - the P1_6 regression guard`() {
        val result = DayTdeeCalculator.tdee(bmr, moderate, exerciseKcal = 0.0)

        assertEquals(2790.0, result.kcal, 1e-6)
        assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, result.source)
        // Not merely close: the very same double the pre-exercise code returned.
        assertEquals(
            TdeeCalculator.fromActivityLevel(bmr, moderate).toRawBits(),
            result.kcal.toRawBits(),
        )
    }

    @Test
    fun `zero exercise is identical to the multiplier for every activity level and a spread of BMRs`() {
        for (level in ActivityLevel.entries) {
            for (bmrKcal in listOf(1000.0, 1337.7, 1666.0, 1812.75, 2400.5, 3100.0)) {
                val expected = TdeeCalculator.fromActivityLevel(bmrKcal, level)
                val actual = DayTdeeCalculator.tdee(bmrKcal, level, 0.0)
                assertEquals(expected.toRawBits(), actual.kcal.toRawBits(), "$level @ $bmrKcal")
                assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, actual.source)
            }
        }
    }

    @Test
    fun `a small session on a moderate profile does not move the target`() {
        // 1980 + 200 = 2180 < 2790: that session is what "moderate" already assumed.
        val result = DayTdeeCalculator.tdee(bmr, moderate, exerciseKcal = 200.0)
        assertEquals(2790.0, result.kcal, 1e-6)
        assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, result.source)
    }

    @Test
    fun `a big session raises the target by the part the multiplier did not assume`() {
        val result = DayTdeeCalculator.tdee(bmr, moderate, exerciseKcal = 900.0)
        assertEquals(2880.0, result.kcal, 1e-6) // 1980 + 900
        assertEquals(TdeeSource.MANUAL_EXERCISE, result.source)
    }

    @Test
    fun `a very big day`() {
        assertEquals(3480.0, DayTdeeCalculator.tdee(bmr, moderate, exerciseKcal = 1500.0).kcal, 1e-6)
    }

    @Test
    fun `sedentary with 600 kcal logged takes the logged-day term`() {
        // max(1800 x 1.2 = 2160, 1980 + 600 = 2580)
        assertEquals(2580.0, DayTdeeCalculator.tdee(bmr, ActivityLevel.SEDENTARY, exerciseKcal = 600.0).kcal, 1e-6)
    }

    @Test
    fun `negative exercise kcal throws`() {
        assertFailsWith<IllegalArgumentException> { DayTdeeCalculator.tdee(bmr, moderate, exerciseKcal = -1.0) }
    }

    @Test
    fun `the target never falls below the multiplier baseline on a day with exercise`() {
        for (exercise in listOf(0.0, 1.0, 100.0, 500.0, 1000.0, 2000.0)) {
            val kcal = DayTdeeCalculator.tdee(bmr, moderate, exercise).kcal
            assertEquals(true, kcal >= 2790.0 - 1e-9, "exercise $exercise gave $kcal")
        }
    }

    @Test
    fun `TdeeCalculator still prefers the wearable branch when active energy exists`() {
        val result = TdeeCalculator.tdee(bmr, moderate, activeKcal = 500.0, exerciseKcal = 900.0)
        assertEquals(TdeeSource.WEARABLE, result.source)
        assertEquals(1800.0 * 1.1 + 500.0, result.kcal, 1e-6)
    }
}

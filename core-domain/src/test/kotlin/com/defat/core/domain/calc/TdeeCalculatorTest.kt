package com.defat.core.domain.calc

import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.TdeeSource
import kotlin.test.assertEquals
import org.junit.Test

class TdeeCalculatorTest {

    @Test
    fun `activity multipliers match docs04`() {
        assertEquals(1.2, ActivityLevel.SEDENTARY.multiplier, 1e-9)
        assertEquals(1.375, ActivityLevel.LIGHT.multiplier, 1e-9)
        assertEquals(1.55, ActivityLevel.MODERATE.multiplier, 1e-9)
        assertEquals(1.725, ActivityLevel.ACTIVE.multiplier, 1e-9)
        assertEquals(1.9, ActivityLevel.ATHLETE.multiplier, 1e-9)
    }

    @Test
    fun `moderate multiplier on reference BMR`() {
        val tdee = TdeeCalculator.fromActivityLevel(1666.0, ActivityLevel.MODERATE)
        assertEquals(2582.3, tdee, 1e-6)
    }

    @Test
    fun `sedentary on reference BMR`() {
        val tdee = TdeeCalculator.fromActivityLevel(1666.0, ActivityLevel.SEDENTARY)
        assertEquals(1999.2, tdee, 1e-6)
    }

    @Test
    fun `wearable mode uses 1_1 baseline plus active energy`() {
        val tdee = TdeeCalculator.fromWearable(bmrKcal = 1666.0, activeKcal = 500.0)
        assertEquals(2332.6, tdee, 1e-6)
    }

    @Test
    fun `tdee() reports ACTIVITY_MULTIPLIER when no wearable data`() {
        val result = TdeeCalculator.tdee(1666.0, ActivityLevel.MODERATE, activeKcal = null)
        assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, result.source)
        assertEquals(2582.3, result.kcal, 1e-6)
    }

    @Test
    fun `tdee() reports WEARABLE when active energy present`() {
        val result = TdeeCalculator.tdee(1666.0, ActivityLevel.MODERATE, activeKcal = 500.0)
        assertEquals(TdeeSource.WEARABLE, result.source)
        assertEquals(2332.6, result.kcal, 1e-6)
    }
}

package com.defat.core.domain.calc

import com.defat.core.domain.model.Sex
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class CalorieTargetCalculatorTest {

    @Test
    fun `default deficit is 20 percent`() {
        val result = CalorieTargetCalculator.target(tdeeKcal = 2582.3, sex = Sex.MALE)
        assertEquals(2065.84, result.kcal, 1e-6)
        assertFalse(result.floorApplied)
    }

    @Test
    fun `male floor applies at 1500`() {
        val result = CalorieTargetCalculator.target(tdeeKcal = 1800.0, sex = Sex.MALE)
        assertEquals(1500.0, result.kcal, 1e-6)
        assertTrue(result.floorApplied)
    }

    @Test
    fun `female floor applies at 1200`() {
        val result = CalorieTargetCalculator.target(tdeeKcal = 1400.0, sex = Sex.FEMALE)
        assertEquals(1200.0, result.kcal, 1e-6)
        assertTrue(result.floorApplied)
    }

    @Test
    fun `trainer override can go below the floor`() {
        val result = CalorieTargetCalculator.target(
            tdeeKcal = 1400.0,
            sex = Sex.FEMALE,
            trainerOverrideFloorKcal = 1000.0,
        )
        assertEquals(1120.0, result.kcal, 1e-6)
        assertFalse(result.floorApplied)
    }

    @Test
    fun `rejects absurd deficit fractions`() {
        assertFailsWith<IllegalArgumentException> {
            CalorieTargetCalculator.target(tdeeKcal = 2000.0, sex = Sex.MALE, deficitFraction = 0.6)
        }
    }
}

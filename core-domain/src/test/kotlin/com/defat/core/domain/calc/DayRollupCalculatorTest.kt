package com.defat.core.domain.calc

import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.TdeeSource
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class DayRollupCalculatorTest {

    private val date = LocalDate.of(2026, 3, 15)

    private val target = DailyTarget(
        bmrKcal = 1666.0,
        bmrMethod = BmrMethod.KATCH_MCARDLE,
        tdeeKcal = 2582.3,
        tdeeSource = TdeeSource.ACTIVITY_MULTIPLIER,
        targetKcal = 2065.84,
        floorApplied = false,
        macros = MacroTargets(proteinG = 132.0, fatG = 48.0, carbsG = 276.46, carbsClamped = false),
    )

    private fun meal(kcal: Double, protein: Double, carbs: Double, fat: Double) = Meal(
        id = "m-$kcal-$protein",
        loggedAt = Instant.parse("2026-03-15T12:00:00Z"),
        date = date,
        name = "meal",
        kcal = kcal,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        source = MealSource.MANUAL,
    )

    @Test
    fun `empty day leaves the full target remaining`() {
        val rollup = DayRollupCalculator.rollup(date, target, emptyList())
        assertEquals(0.0, rollup.intakeKcal, 1e-6)
        assertEquals(2065.84, rollup.remainingKcal, 1e-6)
        assertEquals(0, rollup.mealCount)
        assertFalse(rollup.isOverTarget)
    }

    @Test
    fun `sums meals`() {
        val meals = listOf(
            meal(kcal = 600.0, protein = 30.0, carbs = 80.0, fat = 15.0),
            meal(kcal = 450.0, protein = 40.0, carbs = 30.0, fat = 12.0),
        )
        val rollup = DayRollupCalculator.rollup(date, target, meals)
        assertEquals(1050.0, rollup.intakeKcal, 1e-6)
        assertEquals(70.0, rollup.proteinG, 1e-6)
        assertEquals(110.0, rollup.carbsG, 1e-6)
        assertEquals(27.0, rollup.fatG, 1e-6)
        assertEquals(1015.84, rollup.remainingKcal, 1e-6)
        assertEquals(62.0, rollup.remainingProteinG, 1e-6)
    }

    @Test
    fun `flags going over target`() {
        val meals = listOf(meal(kcal = 2500.0, protein = 100.0, carbs = 300.0, fat = 60.0))
        val rollup = DayRollupCalculator.rollup(date, target, meals)
        assertEquals(-434.16, rollup.remainingKcal, 1e-6)
        assertTrue(rollup.isOverTarget)
        assertEquals(1f, rollup.progressFraction)
    }

    @Test
    fun `progressFraction is clamped to 0 to 1`() {
        val meals = listOf(meal(kcal = 2500.0, protein = 100.0, carbs = 300.0, fat = 60.0))
        val rollup = DayRollupCalculator.rollup(date, target, meals)
        assertEquals(1f, rollup.progressFraction)
    }
}

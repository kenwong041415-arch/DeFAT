package com.defat.core.domain.calc

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class MacroCalculatorTest {

    @Test
    fun `uses 2_2 g per kg LBM when body fat known`() {
        val macros = MacroCalculator.macroTargets(targetKcal = 2065.84, weightKg = 80.0, bodyFatPct = 25.0)
        assertEquals(132.0, macros.proteinG, 1e-6)
        assertEquals(48.0, macros.fatG, 1e-6)
        assertEquals(276.46, macros.carbsG, 1e-6)
        assertFalse(macros.carbsClamped)
    }

    @Test
    fun `uses 1_8 g per kg bodyweight when body fat unknown`() {
        val macros = MacroCalculator.macroTargets(targetKcal = 2065.84, weightKg = 80.0, bodyFatPct = null)
        assertEquals(144.0, macros.proteinG, 1e-6)
        assertEquals(48.0, macros.fatG, 1e-6)
        assertEquals(264.46, macros.carbsG, 1e-6)
    }

    @Test
    fun `fat floor is 0_6 g per kg`() {
        val macros = MacroCalculator.macroTargets(targetKcal = 3000.0, weightKg = 90.0, bodyFatPct = null)
        assertEquals(0.6 * 90.0, macros.fatG, 1e-6)
    }

    @Test
    fun `carbs clamp to zero and flag when protein plus fat exceed target`() {
        val macros = MacroCalculator.macroTargets(targetKcal = 800.0, weightKg = 100.0, bodyFatPct = 20.0)
        assertEquals(0.0, macros.carbsG, 1e-6)
        assertTrue(macros.carbsClamped)
    }

    @Test
    fun `energyKcal recomposes to the target when not clamped`() {
        val macros = MacroCalculator.macroTargets(targetKcal = 2065.84, weightKg = 80.0, bodyFatPct = 25.0)
        assertEquals(2065.84, macros.energyKcal, 1e-6)
    }
}

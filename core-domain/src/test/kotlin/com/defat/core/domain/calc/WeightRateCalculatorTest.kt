package com.defat.core.domain.calc

import kotlin.test.assertEquals
import org.junit.Test

class WeightRateCalculatorTest {

    @Test
    fun `0_6 kg per week on 80 kg is safe`() {
        assertEquals(0.75, WeightRateCalculator.weeklyRatePctOfBodyweight(-0.6, 80.0), 1e-6)
        assertEquals(RateAssessment.SAFE, WeightRateCalculator.assess(-0.6, 80.0))
    }

    @Test
    fun `1_2 kg per week on 80 kg is too fast`() {
        assertEquals(1.5, WeightRateCalculator.weeklyRatePctOfBodyweight(-1.2, 80.0), 1e-6)
        assertEquals(RateAssessment.TOO_FAST, WeightRateCalculator.assess(-1.2, 80.0))
    }

    @Test
    fun `0_2 kg per week on 80 kg is too slow`() {
        assertEquals(0.25, WeightRateCalculator.weeklyRatePctOfBodyweight(-0.2, 80.0), 1e-6)
        assertEquals(RateAssessment.TOO_SLOW, WeightRateCalculator.assess(-0.2, 80.0))
    }

    @Test
    fun `weight gain is reported as gaining`() {
        assertEquals(RateAssessment.GAINING, WeightRateCalculator.assess(0.3, 80.0))
    }

    @Test
    fun `projected weekly loss uses 7700 kcal per kg`() {
        val kg = WeightRateCalculator.projectedWeeklyLossKg(516.46)
        assertEquals(0.4695, kg, 1e-3)
    }
}

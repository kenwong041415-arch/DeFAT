package com.defat.core.domain.calc

import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodSource
import kotlin.test.assertEquals
import org.junit.Test

/** docs/07 §3 and plan §8.1, rule by rule. */
class FoodConfidenceRulesTest {

    private fun rate(source: FoodSource, kcal: Double = 500.0, low: Double? = null, high: Double? = null) =
        FoodConfidenceRules.confidenceFor(source, kcal, low, high)

    @Test
    fun `rule 1 - OFFICIAL is HIGH`() = assertEquals(FoodConfidence.HIGH, rate(FoodSource.OFFICIAL))

    @Test
    fun `rule 1 - TRAINER is HIGH`() = assertEquals(FoodConfidence.HIGH, rate(FoodSource.TRAINER))

    @Test
    fun `rule 2 - MEDIA is LOW`() = assertEquals(FoodConfidence.LOW, rate(FoodSource.MEDIA))

    @Test
    fun `rule 3 - a spread over 25 percent is LOW`() {
        // (1000 - 700) / 1000 = 30 %
        assertEquals(FoodConfidence.LOW, rate(FoodSource.COMPOSITION_TABLE, 1000.0, 700.0, 1000.0))
    }

    @Test
    fun `rule 3 - exactly 25 percent is not wide`() {
        // (625 - 500) / 500 = 25 % exactly
        assertEquals(FoodConfidence.MEDIUM, rate(FoodSource.DIETITIAN, 500.0, 500.0, 625.0))
    }

    @Test
    fun `rule 4 - COMPOSITION_TABLE and DIETITIAN are MEDIUM`() {
        assertEquals(FoodConfidence.MEDIUM, rate(FoodSource.COMPOSITION_TABLE))
        assertEquals(FoodConfidence.MEDIUM, rate(FoodSource.DIETITIAN))
    }

    @Test
    fun `rule 5 - USER is MEDIUM`() = assertEquals(FoodConfidence.MEDIUM, rate(FoodSource.USER))

    @Test
    fun `MEDIA with a tight range is still LOW - rule 2 beats rule 3`() {
        assertEquals(FoodConfidence.LOW, rate(FoodSource.MEDIA, 500.0, 495.0, 505.0))
    }

    @Test
    fun `DIETITIAN with a 30 percent spread is LOW - rule 3 beats rule 4`() {
        assertEquals(FoodConfidence.LOW, rate(FoodSource.DIETITIAN, 1000.0, 850.0, 1150.0))
    }

    @Test
    fun `TRAINER with a wide range is HIGH - rule 1 beats everything`() {
        assertEquals(FoodConfidence.HIGH, rate(FoodSource.TRAINER, 1000.0, 400.0, 1600.0))
    }

    @Test
    fun `a half-open range is not a range`() {
        assertEquals(FoodConfidence.MEDIUM, rate(FoodSource.USER, 500.0, 100.0, null))
        assertEquals(FoodConfidence.MEDIUM, rate(FoodSource.USER, 500.0, null, 900.0))
    }
}

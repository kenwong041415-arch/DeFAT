package com.defat.core.domain.usecase

import com.defat.core.domain.calc.RateAssessment
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test

class AssessGoalRateUseCaseTest {

    private val assess = AssessGoalRateUseCase()
    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun `10 weeks to lose 6 kg from 80 kg is a safe pace`() {
        val rate = assess(
            currentWeightKg = 80.0,
            targetWeightKg = 74.0,
            targetDate = today.plusWeeks(10),
            today = today,
        )!!

        assertEquals(6.0, rate.totalToLoseKg, 1e-9)
        assertEquals(10.0, rate.weeks, 1e-9)
        assertEquals(0.6, rate.weeklyLossKg, 1e-9)
        assertEquals(0.75, rate.weeklyRatePct, 1e-9)
        assertEquals(RateAssessment.SAFE, rate.assessment)
    }

    @Test
    fun `5 weeks to lose 6 kg from 80 kg is too fast`() {
        val rate = assess(80.0, 74.0, today.plusWeeks(5), today)!!

        assertEquals(1.2, rate.weeklyLossKg, 1e-9)
        assertEquals(1.5, rate.weeklyRatePct, 1e-9)
        assertEquals(RateAssessment.TOO_FAST, rate.assessment)
    }

    @Test
    fun `30 weeks to lose 6 kg from 80 kg is slower than the usual range`() {
        val rate = assess(80.0, 74.0, today.plusWeeks(30), today)!!

        assertEquals(0.2, rate.weeklyLossKg, 1e-9)
        assertEquals(0.25, rate.weeklyRatePct, 1e-9)
        assertEquals(RateAssessment.TOO_SLOW, rate.assessment)
    }

    @Test
    fun `returns null when there is no target date`() {
        assertNull(assess(80.0, 74.0, targetDate = null, today = today))
    }

    @Test
    fun `returns null when the goal is not a loss`() {
        assertNull(assess(80.0, 80.0, today.plusWeeks(10), today))
        assertNull(assess(80.0, 85.0, today.plusWeeks(10), today))
    }

    @Test
    fun `returns null when the target date is today or in the past`() {
        assertNull(assess(80.0, 74.0, today, today))
        assertNull(assess(80.0, 74.0, today.minusWeeks(1), today))
    }

    @Test
    fun `rejects non-positive current weight`() {
        assertFailsWith<IllegalArgumentException> {
            assess(0.0, 74.0, today.plusWeeks(10), today)
        }
    }
}

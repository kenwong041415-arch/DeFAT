package com.defat.core.domain.usecase

import com.defat.core.domain.calc.RateAssessment
import com.defat.core.domain.calc.WeightRateCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * How fast the student would have to lose weight to hit their goal by the
 * target date, and whether that pace is safe (`docs/04`: 0.5–1.0 % of
 * bodyweight per week).
 *
 * Lives in the domain layer because it is arithmetic: `CLAUDE.md` forbids
 * formulas in UI code.
 */
data class GoalRate(
    val totalToLoseKg: Double,
    val weeks: Double,
    val weeklyLossKg: Double,
    val weeklyRatePct: Double,
    val assessment: RateAssessment,
)

class AssessGoalRateUseCase @Inject constructor() {

    /**
     * Returns null when the pace cannot be assessed: no target date, the goal
     * is not a loss, or the date is today/in the past.
     */
    operator fun invoke(
        currentWeightKg: Double,
        targetWeightKg: Double,
        targetDate: LocalDate?,
        today: LocalDate,
    ): GoalRate? {
        require(currentWeightKg > 0) { "currentWeightKg must be positive" }

        val totalToLoseKg = currentWeightKg - targetWeightKg
        if (totalToLoseKg <= 0.0) return null
        if (targetDate == null) return null

        val weeks = ChronoUnit.DAYS.between(today, targetDate) / DAYS_PER_WEEK
        if (weeks <= 0.0) return null

        val weeklyLossKg = totalToLoseKg / weeks
        return GoalRate(
            totalToLoseKg = totalToLoseKg,
            weeks = weeks,
            weeklyLossKg = weeklyLossKg,
            // WeightRateCalculator takes a signed change; loss is negative.
            weeklyRatePct = WeightRateCalculator.weeklyRatePctOfBodyweight(-weeklyLossKg, currentWeightKg),
            assessment = WeightRateCalculator.assess(-weeklyLossKg, currentWeightKg),
        )
    }

    private companion object {
        const val DAYS_PER_WEEK = 7.0
    }
}

package com.defat.core.domain.calc

import kotlin.math.abs

/** D5 — implemented and tested in Phase 1; no UI wired to it yet. */
enum class RateAssessment { GAINING, TOO_SLOW, SAFE, TOO_FAST }

/**
 * Weight-change-rate math — docs/04-health-metabolism.md "Calorie target"
 * (safe rate) and "Trend engine" (7700 kcal ~= 1 kg fat) sections.
 */
object WeightRateCalculator {

    /**
     * Rate as a percentage of bodyweight, on the *magnitude* of the change
     * (a gain and an equal-sized loss return the same percentage).
     */
    fun weeklyRatePctOfBodyweight(weeklyChangeKg: Double, bodyWeightKg: Double): Double {
        require(bodyWeightKg > 0) { "bodyWeightKg must be > 0, was $bodyWeightKg" }
        return abs(weeklyChangeKg) / bodyWeightKg * 100.0
    }

    /** [weeklyChangeKg] is signed: negative = loss, positive = gain. */
    fun assess(weeklyChangeKg: Double, bodyWeightKg: Double): RateAssessment {
        if (weeklyChangeKg > 0) return RateAssessment.GAINING
        val pct = weeklyRatePctOfBodyweight(weeklyChangeKg, bodyWeightKg)
        return when {
            pct < NutritionConstants.SAFE_WEEKLY_LOSS_PCT_MIN -> RateAssessment.TOO_SLOW
            pct > NutritionConstants.SAFE_WEEKLY_LOSS_PCT_MAX -> RateAssessment.TOO_FAST
            else -> RateAssessment.SAFE
        }
    }

    /** docs/04: 7700 kcal ~= 1 kg fat. weeklyLossKg = dailyDeficitKcal * 7 / 7700. */
    fun projectedWeeklyLossKg(dailyDeficitKcal: Double): Double {
        return dailyDeficitKcal * 7.0 / NutritionConstants.KCAL_PER_KG_FAT
    }
}

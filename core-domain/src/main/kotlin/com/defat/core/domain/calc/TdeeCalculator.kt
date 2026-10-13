package com.defat.core.domain.calc

import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.TdeeSource

data class TdeeResult(val kcal: Double, val source: TdeeSource)

/** Total daily energy expenditure — docs/04-health-metabolism.md "TDEE" section. */
object TdeeCalculator {

    /** No-wearable-data mode: TDEE = BMR x activity multiplier. */
    fun fromActivityLevel(bmrKcal: Double, level: ActivityLevel): Double {
        require(bmrKcal > 0) { "bmrKcal must be > 0, was $bmrKcal" }
        return bmrKcal * level.multiplier
    }

    /**
     * Wearable mode: TDEE = BMR x NEAT/TEF baseline + active energy.
     *
     * Double-counting guard (docs/04, critical): [activeKcal] must be
     * *active* energy only (e.g. Health Connect ActiveCaloriesBurned /
     * ExerciseSession) — never pass "total calories burned" (which already
     * includes BMR), and never a steps-derived estimate when active energy
     * exists.
     */
    fun fromWearable(
        bmrKcal: Double,
        activeKcal: Double,
        neatTefFactor: Double = NutritionConstants.NEAT_TEF_BASELINE,
    ): Double {
        require(bmrKcal > 0) { "bmrKcal must be > 0, was $bmrKcal" }
        require(activeKcal >= 0) { "activeKcal must be >= 0, was $activeKcal" }
        return bmrKcal * neatTefFactor + activeKcal
    }

    /**
     * Chooses the wearable branch when [activeKcal] is non-null (Phase 1
     * always passes null; Phase 2 wires Health Connect). Without wearable data,
     * [exerciseKcal] is the day's manually logged exercise and goes through
     * [DayTdeeCalculator]; at 0 that is exactly the multiplier result.
     *
     * A wearable reading wins over manual entries: docs/04 says an overlapping
     * manual entry is not counted once a wearable session exists, and the
     * wearable branch already includes all active energy.
     */
    fun tdee(
        bmrKcal: Double,
        level: ActivityLevel,
        activeKcal: Double?,
        exerciseKcal: Double = 0.0,
    ): TdeeResult {
        return if (activeKcal != null) {
            TdeeResult(fromWearable(bmrKcal, activeKcal), TdeeSource.WEARABLE)
        } else {
            DayTdeeCalculator.tdee(bmrKcal, level, exerciseKcal)
        }
    }
}

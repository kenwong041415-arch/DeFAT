package com.defat.core.domain.calc

import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.TdeeSource

/**
 * TDEE for a day that may have manually logged exercise — docs/04
 * "Manually logged exercise and the activity multiplier" (Phase 1.7 plan §10.1).
 *
 * ```
 * TDEE(day) = max(
 *     BMR x activityMultiplier,               // the profile's stated baseline
 *     BMR x NEAT_TEF_BASELINE + exerciseKcal  // the day as actually logged
 * )
 * ```
 *
 * Why not `BMR x multiplier + exerciseKcal`: the activity multiplier already
 * contains the student's usual training, so adding a logged session on top
 * counts the same workout twice and hands a fat-loss student a target that is
 * too high. With the max, a day with no exercise is exactly the multiplier
 * result, a small session on a "moderate" profile does not move the target, and
 * only the part of a big session that the multiplier did not assume raises it.
 */
object DayTdeeCalculator {

    fun tdee(bmrKcal: Double, level: ActivityLevel, exerciseKcal: Double): TdeeResult {
        require(bmrKcal > 0) { "bmrKcal must be > 0, was $bmrKcal" }
        require(exerciseKcal >= 0) { "exerciseKcal must be >= 0, was $exerciseKcal" }

        val baseline = TdeeCalculator.fromActivityLevel(bmrKcal, level)
        val asLogged = bmrKcal * NutritionConstants.NEAT_TEF_BASELINE + exerciseKcal
        // Strictly greater: on a tie the profile's stated baseline stands, so a
        // day with no exercise keeps reporting ACTIVITY_MULTIPLIER.
        return if (asLogged > baseline) {
            TdeeResult(asLogged, TdeeSource.MANUAL_EXERCISE)
        } else {
            TdeeResult(baseline, TdeeSource.ACTIVITY_MULTIPLIER)
        }
    }

    fun tdeeKcal(bmrKcal: Double, level: ActivityLevel, exerciseKcal: Double): Double =
        tdee(bmrKcal, level, exerciseKcal).kcal
}

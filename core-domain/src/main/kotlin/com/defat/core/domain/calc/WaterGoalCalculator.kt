package com.defat.core.domain.calc

import kotlin.math.floor

/** Water goal (plan §11): 35 ml per kg of bodyweight, a coaching rule of thumb. */
object WaterGoalCalculator {

    /** `35 ml x weightKg`, rounded down to the nearest 100 ml, floored at 1500 ml. */
    fun goalMl(weightKg: Double): Int {
        require(weightKg > 0) { "weightKg must be > 0, was $weightKg" }
        val raw = NutritionConstants.WATER_ML_PER_KG * weightKg
        val step = NutritionConstants.WATER_GOAL_STEP_ML
        val roundedDown = (floor(raw / step) * step).toInt()
        return roundedDown.coerceAtLeast(NutritionConstants.WATER_GOAL_MIN_ML)
    }
}

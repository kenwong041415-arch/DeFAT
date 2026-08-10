package com.defat.core.domain.calc

import com.defat.core.domain.model.Sex

data class TargetResult(val kcal: Double, val floorApplied: Boolean)

/** Daily calorie target — docs/04-health-metabolism.md "Calorie target" section. */
object CalorieTargetCalculator {

    /** docs/04: never below ~1200 kcal female / ~1500 kcal male without trainer override. */
    fun floorFor(sex: Sex): Double = when (sex) {
        Sex.MALE -> NutritionConstants.FLOOR_KCAL_MALE
        Sex.FEMALE -> NutritionConstants.FLOOR_KCAL_FEMALE
    }

    fun target(
        tdeeKcal: Double,
        sex: Sex,
        deficitFraction: Double = NutritionConstants.DEFAULT_DEFICIT_FRACTION,
        trainerOverrideFloorKcal: Double? = null,
    ): TargetResult {
        require(tdeeKcal > 0) { "tdeeKcal must be > 0, was $tdeeKcal" }
        require(deficitFraction in 0.0..0.40) {
            "deficitFraction must be in 0.0..0.40, was $deficitFraction"
        }
        val raw = tdeeKcal * (1 - deficitFraction)
        val floor = trainerOverrideFloorKcal ?: floorFor(sex)
        return if (raw < floor) {
            TargetResult(floor, floorApplied = true)
        } else {
            TargetResult(raw, floorApplied = false)
        }
    }
}

package com.defat.core.domain.calc

import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.Meal
import java.time.LocalDate

/** Pure summation of a day's meals against its target. No caching (D2). */
object DayRollupCalculator {

    fun rollup(date: LocalDate, target: DailyTarget, meals: List<Meal>): DayRollup {
        var kcal = 0.0
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0
        for (meal in meals) {
            kcal += meal.kcal
            protein += meal.proteinG
            carbs += meal.carbsG
            fat += meal.fatG
        }
        return DayRollup(
            date = date,
            target = target,
            intakeKcal = kcal,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
            mealCount = meals.size,
        )
    }
}

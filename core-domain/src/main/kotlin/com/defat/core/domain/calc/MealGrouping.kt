package com.defat.core.domain.calc

import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealType
import com.defat.core.domain.model.MealTypeGroup

object MealGrouping {
    /**
     * Always returns exactly five groups, one per [MealType], in enum
     * declaration order, including empty ones — the UI shows an empty meal type
     * as a prompt to log, so the empty groups are load-bearing, not padding.
     */
    fun groupByType(meals: List<Meal>): List<MealTypeGroup> {
        val buckets = MealType.entries.associateWith { mutableListOf<Meal>() }
        for (meal in meals) {
            buckets.getValue(meal.mealType).add(meal)
        }
        return MealType.entries.map { type ->
            val groupMeals = buckets.getValue(type).sortedBy { it.loggedAt }
            MealTypeGroup(
                type = type,
                meals = groupMeals,
                kcal = groupMeals.sumOf { it.kcal },
                proteinG = groupMeals.sumOf { it.proteinG },
                carbsG = groupMeals.sumOf { it.carbsG },
                fatG = groupMeals.sumOf { it.fatG },
            )
        }
    }
}

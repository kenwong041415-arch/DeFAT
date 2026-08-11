package com.defat.core.domain.model

data class MealTypeGroup(
    val type: MealType,
    /** Ascending by [Meal.loggedAt]. */
    val meals: List<Meal>,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    val isEmpty: Boolean get() = meals.isEmpty()
    val mealCount: Int get() = meals.size
}

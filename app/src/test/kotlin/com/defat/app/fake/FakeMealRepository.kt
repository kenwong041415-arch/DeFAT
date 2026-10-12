package com.defat.app.fake

import com.defat.core.domain.model.Meal
import com.defat.core.domain.repository.MealRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMealRepository(initial: List<Meal> = emptyList()) : MealRepository {

    private val meals = MutableStateFlow(initial)

    var upsertCallCount: Int = 0
        private set
    var lastUpserted: Meal? = null
        private set
    var deleteCallCount: Int = 0
        private set

    override fun mealsOn(date: LocalDate) = meals.map { list -> list.filter { it.date == date } }

    override fun recentMeals(limit: Int) =
        meals.map { list -> list.sortedByDescending { it.loggedAt }.take(limit) }

    override suspend fun mealById(id: String): Meal? = meals.value.firstOrNull { it.id == id }

    override suspend fun upsert(meal: Meal) {
        upsertCallCount++
        lastUpserted = meal
        meals.value = meals.value.filterNot { it.id == meal.id } + meal
    }

    override suspend fun delete(id: String) {
        deleteCallCount++
        meals.value = meals.value.filterNot { it.id == id }
    }
}

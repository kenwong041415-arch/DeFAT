package com.defat.app.fake

import com.defat.core.domain.model.Meal
import com.defat.core.domain.repository.MealRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMealRepository(initial: List<Meal> = emptyList()) : MealRepository {

    private val meals = MutableStateFlow(initial)

    override fun mealsOn(date: LocalDate) = meals.map { list -> list.filter { it.date == date } }

    override suspend fun mealById(id: String): Meal? = meals.value.firstOrNull { it.id == id }

    override suspend fun upsert(meal: Meal) {
        meals.value = meals.value.filterNot { it.id == meal.id } + meal
    }

    override suspend fun delete(id: String) {
        meals.value = meals.value.filterNot { it.id == id }
    }
}

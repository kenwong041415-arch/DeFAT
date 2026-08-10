package com.defat.core.domain.repository

import com.defat.core.domain.model.Meal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun mealsOn(date: LocalDate): Flow<List<Meal>>
    suspend fun mealById(id: String): Meal?
    suspend fun upsert(meal: Meal)
    suspend fun delete(id: String)
}

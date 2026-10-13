package com.defat.core.domain.repository

import com.defat.core.domain.model.Meal
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    /**
     * Meals whose [Meal.loggedAt] lies in `[startInclusive, endExclusive)`,
     * ascending by time. Days are always queried as an instant window computed
     * by [com.defat.core.domain.calc.DayBoundary] (plan §7.3) — never by
     * comparing a stored date string. Use [mealsOnLogicalDay] for "one day".
     */
    fun mealsBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<Meal>>

    /**
     * Every meal ever logged, ascending by time. Feeds the week strip's
     * "has entries" dots and the History list; at this app's scale (a few
     * thousand rows at most) one observable list is simpler than per-day queries.
     */
    fun allMeals(): Flow<List<Meal>>

    /**
     * The [limit] most recently logged meals, newest first. Feeds the
     * frequent-food list; the ranking itself is [com.defat.core.domain.calc.FrequentFoodRanker.rank].
     */
    fun recentMeals(limit: Int): Flow<List<Meal>>
    suspend fun mealById(id: String): Meal?
    suspend fun upsert(meal: Meal)
    suspend fun delete(id: String)
}

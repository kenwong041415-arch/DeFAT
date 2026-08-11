package com.defat.core.domain.calc

import com.defat.core.domain.model.FrequentFood
import com.defat.core.domain.model.Meal
import java.time.Instant

private val WHITESPACE_RUN = Regex("\\s+")

object FrequentFoodRanker {

    const val DEFAULT_LIMIT: Int = 5

    /**
     * Trim, collapse internal whitespace runs to a single space, lowercase with
     * root-locale semantics. Chinese food names are case-less so this is mostly
     * a whitespace fix; English ones ("Chicken Rice" / "chicken rice") merge.
     */
    fun normaliseName(raw: String): String =
        raw.trim().replace(WHITESPACE_RUN, " ").lowercase()

    /**
     * Groups [meals] by [normaliseName], drops blank names, and returns at most
     * [limit] foods ordered by: times logged descending, then most recently
     * logged first, then normalised name ascending (so the order is total and
     * the UI never reshuffles for no reason).
     *
     * Numbers and display spelling come from the most recent entry in the group
     * (plan §2 E9), never an average.
     */
    fun rank(meals: List<Meal>, limit: Int = DEFAULT_LIMIT): List<FrequentFood> {
        require(limit > 0) { "limit must be > 0 but was $limit" }

        val groups: Map<String, List<Meal>> = meals
            .filter { normaliseName(it.name).isNotEmpty() }
            .groupBy { normaliseName(it.name) }

        return groups.entries
            .map { (normalisedName, groupMeals) ->
                val latest = groupMeals.maxBy { it.loggedAt }
                Triple(
                    FrequentFood(
                        name = latest.name,
                        timesLogged = groupMeals.size,
                        kcal = latest.kcal,
                        proteinG = latest.proteinG,
                        carbsG = latest.carbsG,
                        fatG = latest.fatG,
                    ),
                    latest.loggedAt,
                    normalisedName,
                )
            }
            .sortedWith(
                compareByDescending<Triple<FrequentFood, Instant, String>> { it.first.timesLogged }
                    .thenByDescending { it.second }
                    .thenBy { it.third },
            )
            .map { it.first }
            .take(limit)
    }
}

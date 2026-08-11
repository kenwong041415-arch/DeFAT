package com.defat.core.domain.calc

import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class FrequentFoodRankerTest {

    private val date = LocalDate.of(2026, 8, 11)

    private fun meal(
        id: String,
        name: String,
        loggedAt: Instant,
        kcal: Double = 500.0,
        protein: Double = 40.0,
        carbs: Double = 50.0,
        fat: Double = 10.0,
    ) = Meal(
        id = id,
        loggedAt = loggedAt,
        date = date,
        name = name,
        kcal = kcal,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        source = MealSource.MANUAL,
    )

    @Test
    fun `normalises case, ends and internal runs of whitespace`() {
        assertEquals("chicken rice", FrequentFoodRanker.normaliseName("  Chicken   Rice "))
        assertEquals("chicken rice", FrequentFoodRanker.normaliseName("CHICKEN RICE"))
    }

    @Test
    fun `merges the same food logged with different spelling`() {
        val meals = listOf(
            meal("1", " Chicken Rice", Instant.parse("2026-08-11T01:00:00Z")),
            meal("2", "chicken rice", Instant.parse("2026-08-11T02:00:00Z")),
            meal("3", "CHICKEN  RICE", Instant.parse("2026-08-11T03:00:00Z")),
        )
        val ranked = FrequentFoodRanker.rank(meals)
        assertEquals(1, ranked.size)
        assertEquals(3, ranked.single().timesLogged)
    }

    @Test
    fun `ranks by times logged descending`() {
        val meals = listOf(
            meal("a1", "A", Instant.parse("2026-08-11T01:00:00Z")),
            meal("a2", "A", Instant.parse("2026-08-11T02:00:00Z")),
            meal("a3", "A", Instant.parse("2026-08-11T03:00:00Z")),
            meal("b1", "B", Instant.parse("2026-08-11T01:00:00Z")),
            meal("b2", "B", Instant.parse("2026-08-11T02:00:00Z")),
            meal("c1", "C", Instant.parse("2026-08-11T01:00:00Z")),
        )
        val ranked = FrequentFoodRanker.rank(meals)
        assertEquals(listOf("A", "B", "C"), ranked.map { it.name })
        assertEquals(listOf(3, 2, 1), ranked.map { it.timesLogged })
    }

    @Test
    fun `takes numbers and spelling from the most recent entry`() {
        val meals = listOf(
            meal("1", "雞胸飯", Instant.parse("2026-08-11T02:00:00Z"), kcal = 500.0, protein = 45.0, carbs = 50.0, fat = 8.0),
            meal("2", "雞胸飯", Instant.parse("2026-08-11T04:00:00Z"), kcal = 520.0, protein = 48.0, carbs = 52.0, fat = 9.0),
        )
        val ranked = FrequentFoodRanker.rank(meals)
        val food = ranked.single()
        assertEquals(520.0, food.kcal)
        assertEquals(48.0, food.proteinG)
        assertEquals(2, food.timesLogged)
        assertEquals("雞胸飯", food.name)
    }

    @Test
    fun `limits the list`() {
        val meals = (1..8).map { i ->
            meal(i.toString(), "food-$i", Instant.parse("2026-08-11T0${i.coerceAtMost(9)}:00:00Z"))
        }
        val ranked = FrequentFoodRanker.rank(meals, limit = 5)
        assertEquals(5, ranked.size)
    }

    @Test
    fun `ignores blank and whitespace-only names`() {
        val meals = listOf(
            meal("1", "", Instant.parse("2026-08-11T01:00:00Z")),
            meal("2", "   ", Instant.parse("2026-08-11T02:00:00Z")),
        )
        assertTrue(FrequentFoodRanker.rank(meals).isEmpty())
    }

    @Test
    fun `ties break by most recent then name`() {
        val meals = listOf(
            meal("a1", "A", Instant.parse("2026-08-11T01:00:00Z")),
            meal("a2", "A", Instant.parse("2026-08-11T10:00:00Z")),
            meal("b1", "B", Instant.parse("2026-08-11T03:00:00Z")),
            meal("b2", "B", Instant.parse("2026-08-11T12:00:00Z")),
        )
        val ranked = FrequentFoodRanker.rank(meals)
        assertEquals(listOf("B", "A"), ranked.map { it.name })
    }
}

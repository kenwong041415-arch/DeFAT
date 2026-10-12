package com.defat.core.data.mapper

import com.defat.core.data.db.entity.MealEntity
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

class MealMappersTest {

    @Test
    fun `round-trip preserves every field`() {
        val meal = Meal(
            id = "meal-1",
            loggedAt = Instant.parse("2026-03-15T08:30:00Z"),
            date = LocalDate.of(2026, 3, 15),
            name = "Chicken and rice",
            kcal = 620.5,
            proteinG = 45.2,
            carbsG = 70.0,
            fatG = 12.3,
            source = MealSource.MANUAL,
        )

        val entity = meal.toEntity(updatedAtMillis = 123L)
        val roundTripped = entity.toDomain()

        assertEquals(meal, roundTripped)
    }

    @Test
    fun `LocalDate and Instant convert to ISO string and epoch millis`() {
        val meal = Meal(
            id = "meal-2",
            loggedAt = Instant.ofEpochMilli(1_700_000_000_000L),
            date = LocalDate.of(2026, 1, 5),
            name = "Oats",
            kcal = 300.0,
            proteinG = 10.0,
            carbsG = 50.0,
            fatG = 5.0,
        )

        val entity = meal.toEntity()
        assertEquals("2026-01-05", entity.date)
        assertEquals(1_700_000_000_000L, entity.loggedAtMillis)

        val back = entity.toDomain()
        assertEquals(meal.date, back.date)
        assertEquals(meal.loggedAt, back.loggedAt)
    }

    @Test
    fun `enum name round-trips`() {
        val entity = MealEntity(
            id = "m",
            loggedAtMillis = 0L,
            date = "2026-01-01",
            name = "n",
            kcal = 0.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0,
            source = MealSource.PHOTO.name,
            updatedAtMillis = 0L,
        )
        assertEquals(MealSource.PHOTO, entity.toDomain().source)
    }

    @Test
    fun `unknown enum name falls back to MANUAL rather than throwing`() {
        val entity = MealEntity(
            id = "m",
            loggedAtMillis = 0L,
            date = "2026-01-01",
            name = "n",
            kcal = 0.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0,
            source = "SOME_FUTURE_SOURCE",
            updatedAtMillis = 0L,
        )
        assertEquals(MealSource.MANUAL, entity.toDomain().source)
    }

    @Test
    fun `mealType round-trips`() {
        val meal = Meal(
            id = "meal-3",
            loggedAt = Instant.parse("2026-03-15T08:30:00Z"),
            date = LocalDate.of(2026, 3, 15),
            name = "Afternoon snack",
            kcal = 200.0,
            proteinG = 5.0,
            carbsG = 30.0,
            fatG = 6.0,
            mealType = MealType.AFTERNOON_TEA,
        )

        val entity = meal.toEntity()
        assertEquals("AFTERNOON_TEA", entity.mealType)
        assertEquals(MealType.AFTERNOON_TEA, entity.toDomain().mealType)
    }

    @Test
    fun `an unknown mealType name falls back to SNACK`() {
        val entity = MealEntity(
            id = "m",
            loggedAtMillis = 0L,
            date = "2026-01-01",
            name = "n",
            kcal = 0.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0,
            source = MealSource.MANUAL.name,
            updatedAtMillis = 0L,
            mealType = "宵夜",
        )
        assertEquals(MealType.SNACK, entity.toDomain().mealType)
    }
}

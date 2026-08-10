package com.defat.core.domain.model

import java.time.Instant
import java.time.LocalDate

/** Only MANUAL is used in Phase 1; the rest are seeded for later phases. */
enum class MealSource { MANUAL, PHOTO, TEXT, LABEL }

data class Meal(
    val id: String, // UUID string
    val loggedAt: Instant,
    val date: LocalDate, // local date the meal counts toward
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: MealSource = MealSource.MANUAL,
)

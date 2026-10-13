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
    /**
     * Which of the five daily meals this belongs to. Defaults to SNACK only so
     * that test and preview construction sites keep compiling; production code
     * always sets it explicitly (MealEditorViewModel) or reads it back from the
     * database (mappers).
     */
    val mealType: MealType = MealType.SNACK,
    /**
     * The 食物庫 row this meal was picked from, or null for a typed meal
     * (plan §8.7). Kept only so the meal can be re-opened in 點食法.
     * [kcal] and the macros above are the meal's own final numbers and are
     * never recomputed from the current library row.
     */
    val baseFoodId: String? = null,
    /** The modifier ids chosen in 點食法, same purpose as [baseFoodId]. */
    val modifierIds: List<String> = emptyList(),
)

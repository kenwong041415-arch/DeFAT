package com.defat.core.domain.model

/** docs/07 §3 — where a food number comes from. Drives the badge and the confidence. */
enum class FoodSource { OFFICIAL, COMPOSITION_TABLE, DIETITIAN, MEDIA, TRAINER, USER }

/** docs/07 §3 — derived from source and spread by FoodConfidenceRules, then stored. */
enum class FoodConfidence { HIGH, MEDIUM, LOW }

enum class FoodCategory {
    TEA_RESTAURANT, DIM_SUM, ROAST_RICE, NOODLE, FAST_FOOD, DRINK, STAPLE, SNACK
}

/**
 * One row of the 食物庫 (docs/07 §2). The stored numbers are the standard
 * as-served configuration named in [portionLabel]; modifiers (docs/07 §6.3)
 * are deltas relative to it. A null macro means no source published it — it is
 * never a zero.
 */
data class FoodItem(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val portionLabel: String,
    val portionGrams: Double?,
    val kcal: Double,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val kcalLow: Double?,
    val kcalHigh: Double?,
    val source: FoodSource,
    val confidence: FoodConfidence,
    val category: FoodCategory,
    val isSeed: Boolean,
    val editedByOwner: Boolean,
)

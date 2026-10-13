package com.defat.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One 食物庫 row (docs/07 §2). `id` is a stable slug for seed rows
 * ("cha_siu_rice"), never an autoincrement: a stable id is what lets a later
 * app version correct a seed row the owner has not edited while leaving the
 * owner's edits alone (see FoodDao).
 *
 * No column here has a SQL DEFAULT, on purpose — the migration inserts every
 * column explicitly, so there is no default for Room's schema check to
 * disagree with (the trap that bit `meals.mealType` in Phase 1.6).
 */
@Entity(
    tableName = "food_items",
    indices = [Index(value = ["category"], name = "food_items_category_idx")],
)
data class FoodItemEntity(
    @PrimaryKey val id: String,
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
    val source: String, // FoodSource.name
    val confidence: String, // FoodConfidence.name
    val category: String, // FoodCategory.name
    val isSeed: Boolean,
    val editedByOwner: Boolean,
)

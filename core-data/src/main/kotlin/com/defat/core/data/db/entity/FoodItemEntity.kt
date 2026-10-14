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
    /**
     * Phase 1.5 sync. Nullable, unlike the other tables' `updatedAt`, on purpose:
     * seed rows are identical on every install and never sync, so they have no
     * timestamp, and the seed INSERTs (which run on a fresh install after Room
     * creates the table) do not name these columns. A non-null column here would
     * make every fresh install crash while seeding. Only a row with
     * `editedByOwner = true` is ever pending.
     */
    val updatedAtMillis: Long? = null,
    /** See MealEntity.syncedAtMillis. */
    val syncedAtMillis: Long? = null,
)

package com.defat.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One modifier row (docs/07 §6.3).
 *
 * [appliesTo] is a comma-separated list of tokens: an UPPER_CASE token is a
 * FoodCategory name ("ROAST_RICE"), anything else is an item id
 * ("cha_siu_rice"). A join table for fifteen rows of static data is not worth
 * its migration (plan §8.2).
 */
@Entity(tableName = "food_modifiers")
data class FoodModifierEntity(
    @PrimaryKey val id: String,
    val nameZh: String,
    val nameEn: String,
    val groupId: String,
    val appliesTo: String,
    val kcalDelta: Double,
    val proteinDelta: Double?,
    val carbsDelta: Double?,
    val fatDelta: Double?,
    val source: String, // FoodSource.name
    val confidence: String, // FoodConfidence.name
    val editedByOwner: Boolean,
)

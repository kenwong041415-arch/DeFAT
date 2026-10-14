package com.defat.core.data.sync

import com.defat.core.data.db.entity.ExerciseEntryEntity
import com.defat.core.data.db.entity.FoodItemEntity
import com.defat.core.data.db.entity.MealEntity
import com.defat.core.data.db.entity.WaterLogEntity
import com.defat.core.data.db.entity.WeightEntryEntity
import com.defat.core.domain.sync.SyncDoc

/*
 * Room entity <-> SyncDoc (the field maps Firestore stores and the export file
 * contains). Field names follow phase1.5 plan §4.
 *
 * Direction "to doc": total, never throws.
 * Direction "to entity": returns null for a document that is missing a required
 * field or has the wrong type. One malformed document must not wedge every
 * future sync pass, so the caller skips it.
 *
 * A pulled row is stored with `syncedAtMillis = updatedAt`: it came from the
 * server, so it is not pending.
 */

// ---- field readers (Firestore returns Long or Double depending on how a number was written) ----

private fun Map<String, Any?>.string(key: String): String = this[key] as? String ?: error("missing $key")
private fun Map<String, Any?>.stringOrNull(key: String): String? = this[key] as? String
private fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: error("missing $key")
private fun Map<String, Any?>.int(key: String): Int = (this[key] as? Number)?.toInt() ?: error("missing $key")
private fun Map<String, Any?>.double(key: String): Double = (this[key] as? Number)?.toDouble() ?: error("missing $key")
private fun Map<String, Any?>.doubleOrNull(key: String): Double? = (this[key] as? Number)?.toDouble()
private fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: error("missing $key")

// ---- meals ----

internal fun MealEntity.toSyncDoc(): SyncDoc = SyncDoc(
    id = id,
    updatedAt = updatedAtMillis,
    deletedAt = deletedAtMillis,
    fields = linkedMapOf(
        "loggedAt" to loggedAtMillis,
        "date" to date,
        "name" to name,
        "kcal" to kcal,
        "proteinG" to proteinG,
        "carbsG" to carbsG,
        "fatG" to fatG,
        "source" to source,
        "mealType" to mealType,
        "baseFoodId" to baseFoodId,
        "modifierIds" to modifierIds,
    ),
)

internal fun SyncDoc.toMealEntityOrNull(): MealEntity? = runCatching {
    MealEntity(
        id = id,
        loggedAtMillis = fields.long("loggedAt"),
        date = fields.string("date"),
        name = fields.string("name"),
        kcal = fields.double("kcal"),
        proteinG = fields.double("proteinG"),
        carbsG = fields.double("carbsG"),
        fatG = fields.double("fatG"),
        source = fields.string("source"),
        updatedAtMillis = updatedAt,
        remoteId = null,
        pendingSync = false,
        mealType = fields.stringOrNull("mealType") ?: "SNACK",
        baseFoodId = fields.stringOrNull("baseFoodId"),
        modifierIds = fields.stringOrNull("modifierIds"),
        syncedAtMillis = updatedAt,
        deletedAtMillis = deletedAt,
    )
}.getOrNull()

// ---- weights (document id = the ISO date, the table's primary key) ----

internal fun WeightEntryEntity.toSyncDoc(): SyncDoc = SyncDoc(
    id = date,
    updatedAt = updatedAtMillis,
    deletedAt = null,
    fields = linkedMapOf(
        "weightKg" to weightKg,
        "fatPct" to bodyFatPct,
        "source" to source,
        "loggedAt" to recordedAtMillis,
    ),
)

internal fun SyncDoc.toWeightEntityOrNull(): WeightEntryEntity? = runCatching {
    WeightEntryEntity(
        date = id,
        weightKg = fields.double("weightKg"),
        bodyFatPct = fields.doubleOrNull("fatPct"),
        recordedAtMillis = fields.long("loggedAt"),
        source = fields.string("source"),
        updatedAtMillis = updatedAt,
        remoteId = null,
        pendingSync = false,
        syncedAtMillis = updatedAt,
    )
}.getOrNull()

// ---- exercise ----

internal fun ExerciseEntryEntity.toSyncDoc(): SyncDoc = SyncDoc(
    id = id,
    updatedAt = updatedAtMillis,
    deletedAt = deletedAtMillis,
    fields = linkedMapOf(
        "loggedAt" to loggedAtMillis,
        "activityKey" to activityKey,
        "met" to met,
        "minutes" to minutes,
        "weightKgAtTime" to weightKgAtTime,
        "kcal" to kcal,
        "note" to note,
    ),
)

internal fun SyncDoc.toExerciseEntityOrNull(): ExerciseEntryEntity? = runCatching {
    ExerciseEntryEntity(
        id = id,
        loggedAtMillis = fields.long("loggedAt"),
        activityKey = fields.string("activityKey"),
        met = fields.double("met"),
        minutes = fields.int("minutes"),
        weightKgAtTime = fields.double("weightKgAtTime"),
        kcal = fields.double("kcal"),
        note = fields.stringOrNull("note"),
        updatedAtMillis = updatedAt,
        syncedAtMillis = updatedAt,
        deletedAtMillis = deletedAt,
    )
}.getOrNull()

// ---- water (document id = the epoch day) ----

internal fun WaterLogEntity.toSyncDoc(): SyncDoc = SyncDoc(
    id = dateEpochDay.toString(),
    updatedAt = updatedAtMillis,
    deletedAt = null,
    fields = linkedMapOf("ml" to ml),
)

internal fun SyncDoc.toWaterEntityOrNull(): WaterLogEntity? = runCatching {
    WaterLogEntity(
        dateEpochDay = id.toLong(),
        ml = fields.int("ml"),
        updatedAtMillis = updatedAt,
        syncedAtMillis = updatedAt,
    )
}.getOrNull()

// ---- foodOverrides (owner-edited food_items rows; document id = the food id) ----

internal fun FoodItemEntity.toSyncDoc(): SyncDoc = SyncDoc(
    id = id,
    updatedAt = updatedAtMillis ?: 0L,
    deletedAt = null,
    fields = linkedMapOf(
        "nameZh" to nameZh,
        "nameEn" to nameEn,
        "portionLabel" to portionLabel,
        "portionGrams" to portionGrams,
        "kcal" to kcal,
        "proteinG" to proteinG,
        "carbsG" to carbsG,
        "fatG" to fatG,
        "kcalLow" to kcalLow,
        "kcalHigh" to kcalHigh,
        "source" to source,
        "confidence" to confidence,
        "category" to category,
        "isSeed" to isSeed,
    ),
)

internal fun SyncDoc.toFoodOverrideEntityOrNull(): FoodItemEntity? = runCatching {
    FoodItemEntity(
        id = id,
        nameZh = fields.string("nameZh"),
        nameEn = fields.string("nameEn"),
        portionLabel = fields.string("portionLabel"),
        portionGrams = fields.doubleOrNull("portionGrams"),
        kcal = fields.double("kcal"),
        proteinG = fields.doubleOrNull("proteinG"),
        carbsG = fields.doubleOrNull("carbsG"),
        fatG = fields.doubleOrNull("fatG"),
        kcalLow = fields.doubleOrNull("kcalLow"),
        kcalHigh = fields.doubleOrNull("kcalHigh"),
        source = fields.string("source"),
        confidence = fields.string("confidence"),
        category = fields.string("category"),
        isSeed = fields.bool("isSeed"),
        // By definition: only owner-edited rows are ever in foodOverrides.
        editedByOwner = true,
        updatedAtMillis = updatedAt,
        syncedAtMillis = updatedAt,
    )
}.getOrNull()

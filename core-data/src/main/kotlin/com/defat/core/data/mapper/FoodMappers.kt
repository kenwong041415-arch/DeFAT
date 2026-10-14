package com.defat.core.data.mapper

import com.defat.core.data.db.entity.FoodItemEntity
import com.defat.core.data.db.entity.FoodModifierEntity
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.FoodSource

/**
 * [updatedAtMillis] is stamped only for an owner-edited row (the ones that sync);
 * a seed row has none. A fresh entity always has `syncedAtMillis = null`, so an
 * owner edit is pending the moment it is saved.
 */
fun FoodItem.toEntity(updatedAtMillis: Long? = null): FoodItemEntity = FoodItemEntity(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    portionLabel = portionLabel,
    portionGrams = portionGrams,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    kcalLow = kcalLow,
    kcalHigh = kcalHigh,
    source = source.name,
    confidence = confidence.name,
    category = category.name,
    isSeed = isSeed,
    editedByOwner = editedByOwner,
    updatedAtMillis = if (editedByOwner) updatedAtMillis else null,
)

/**
 * An unknown enum name (a row written by a newer build) falls back rather than
 * throwing, like the meal mappers: the least trusted value for source and
 * confidence, and a category that adds no modifier groups.
 */
fun FoodItemEntity.toDomain(): FoodItem = FoodItem(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    portionLabel = portionLabel,
    portionGrams = portionGrams,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    kcalLow = kcalLow,
    kcalHigh = kcalHigh,
    source = runCatching { FoodSource.valueOf(source) }.getOrDefault(FoodSource.MEDIA),
    confidence = runCatching { FoodConfidence.valueOf(confidence) }.getOrDefault(FoodConfidence.LOW),
    category = runCatching { FoodCategory.valueOf(category) }.getOrDefault(FoodCategory.SNACK),
    isSeed = isSeed,
    editedByOwner = editedByOwner,
)

private const val APPLIES_TO_SEPARATOR = ","

private val CATEGORY_NAMES: Set<String> = FoodCategory.entries.map { it.name }.toSet()

/** Splits a stored `appliesTo` into FoodCategory names and item ids. */
internal fun parseAppliesTo(stored: String): Pair<Set<FoodCategory>, Set<String>> {
    val tokens = stored.split(APPLIES_TO_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    val categories = tokens.filter { it in CATEGORY_NAMES }.map { FoodCategory.valueOf(it) }.toSet()
    val itemIds = tokens.filter { it !in CATEGORY_NAMES }.toSet()
    return categories to itemIds
}

/** Categories first (in enum order), then item ids (sorted), so the stored text is stable. */
internal fun formatAppliesTo(categories: Set<FoodCategory>, itemIds: Set<String>): String =
    (categories.sortedBy { it.ordinal }.map { it.name } + itemIds.sorted())
        .joinToString(APPLIES_TO_SEPARATOR)

fun FoodModifier.toEntity(): FoodModifierEntity = FoodModifierEntity(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    groupId = groupId,
    appliesTo = formatAppliesTo(appliesTo, appliesToItemIds),
    kcalDelta = kcalDelta,
    proteinDelta = proteinDelta,
    carbsDelta = carbsDelta,
    fatDelta = fatDelta,
    source = source.name,
    confidence = confidence.name,
    editedByOwner = editedByOwner,
)

fun FoodModifierEntity.toDomain(): FoodModifier {
    val (categories, itemIds) = parseAppliesTo(appliesTo)
    return FoodModifier(
        id = id,
        nameZh = nameZh,
        nameEn = nameEn,
        groupId = groupId,
        appliesTo = categories,
        appliesToItemIds = itemIds,
        kcalDelta = kcalDelta,
        proteinDelta = proteinDelta,
        carbsDelta = carbsDelta,
        fatDelta = fatDelta,
        source = runCatching { FoodSource.valueOf(source) }.getOrDefault(FoodSource.MEDIA),
        confidence = runCatching { FoodConfidence.valueOf(confidence) }.getOrDefault(FoodConfidence.LOW),
        editedByOwner = editedByOwner,
    )
}

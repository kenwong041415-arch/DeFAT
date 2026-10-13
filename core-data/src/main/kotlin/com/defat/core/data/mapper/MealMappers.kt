package com.defat.core.data.mapper

import com.defat.core.data.db.entity.MealEntity
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import java.time.Instant
import java.time.LocalDate

private const val MODIFIER_ID_SEPARATOR = ","

fun Meal.toEntity(updatedAtMillis: Long = System.currentTimeMillis()): MealEntity = MealEntity(
    id = id,
    loggedAtMillis = loggedAt.toEpochMilli(),
    date = date.toString(),
    name = name,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    source = source.name,
    updatedAtMillis = updatedAtMillis,
    mealType = mealType.name,
    baseFoodId = baseFoodId,
    // Null, not "", for a typed meal: the column is nullable (plan §8.7).
    modifierIds = modifierIds.takeIf { it.isNotEmpty() }?.joinToString(MODIFIER_ID_SEPARATOR),
)

fun MealEntity.toDomain(): Meal = Meal(
    id = id,
    loggedAt = Instant.ofEpochMilli(loggedAtMillis),
    date = LocalDate.parse(date),
    name = name,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    // Unknown enum name falls back to MANUAL rather than throwing.
    source = runCatching { MealSource.valueOf(source) }.getOrDefault(MealSource.MANUAL),
    // Unknown enum name falls back to SNACK rather than throwing, matching the
    // existing MealSource behaviour.
    mealType = runCatching { MealType.valueOf(this.mealType) }.getOrDefault(MealType.SNACK),
    baseFoodId = baseFoodId,
    modifierIds = modifierIds
        ?.split(MODIFIER_ID_SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty(),
)

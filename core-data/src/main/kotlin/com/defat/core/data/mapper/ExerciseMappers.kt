package com.defat.core.data.mapper

import com.defat.core.data.db.entity.ExerciseEntryEntity
import com.defat.core.domain.model.ExerciseEntry
import java.time.Instant

fun ExerciseEntry.toEntity(updatedAtMillis: Long = System.currentTimeMillis()): ExerciseEntryEntity = ExerciseEntryEntity(
    id = id,
    loggedAtMillis = loggedAt.toEpochMilli(),
    activityKey = activityKey,
    met = met,
    minutes = minutes,
    weightKgAtTime = weightKgAtTime,
    kcal = kcal,
    note = note,
    updatedAtMillis = updatedAtMillis,
)

fun ExerciseEntryEntity.toDomain(): ExerciseEntry = ExerciseEntry(
    id = id,
    loggedAt = Instant.ofEpochMilli(loggedAtMillis),
    activityKey = activityKey,
    met = met,
    minutes = minutes,
    weightKgAtTime = weightKgAtTime,
    kcal = kcal,
    note = note,
)

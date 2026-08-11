package com.defat.core.data.mapper

import com.defat.core.data.db.entity.WeightEntryEntity
import com.defat.core.domain.model.MeasurementSource
import com.defat.core.domain.model.WeightEntry
import java.time.Instant
import java.time.LocalDate

fun WeightEntry.toEntity(updatedAtMillis: Long = System.currentTimeMillis()): WeightEntryEntity = WeightEntryEntity(
    date = date.toString(),
    weightKg = weightKg,
    bodyFatPct = bodyFatPct,
    recordedAtMillis = recordedAt.toEpochMilli(),
    source = source.name,
    updatedAtMillis = updatedAtMillis,
)

fun WeightEntryEntity.toDomain(): WeightEntry = WeightEntry(
    date = LocalDate.parse(date),
    weightKg = weightKg,
    bodyFatPct = bodyFatPct,
    recordedAt = Instant.ofEpochMilli(recordedAtMillis),
    // Unknown enum name falls back to MANUAL rather than throwing.
    source = runCatching { MeasurementSource.valueOf(source) }.getOrDefault(MeasurementSource.MANUAL),
)

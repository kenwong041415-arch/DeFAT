package com.defat.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val date: String, // one canonical entry per day; re-log replaces
    val weightKg: Double,
    val bodyFatPct: Double?,
    val recordedAtMillis: Long,
    val source: String, // MeasurementSource.name
    val updatedAtMillis: Long,
    val remoteId: String? = null,
    val pendingSync: Boolean = true,
)

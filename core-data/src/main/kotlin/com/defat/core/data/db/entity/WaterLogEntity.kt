package com.defat.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per logical day (plan §11): the day's total in ml. */
@Entity(tableName = "water_log")
data class WaterLogEntity(
    @PrimaryKey val dateEpochDay: Long,
    val ml: Int,
    /**
     * Last local write. Default kept identical to the DEFAULT in
     * SyncMigrations.MIGRATION_3_4_STATEMENTS — see ExerciseEntryEntity.
     */
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0L,
    /** See MealEntity.syncedAtMillis. */
    val syncedAtMillis: Long? = null,
)

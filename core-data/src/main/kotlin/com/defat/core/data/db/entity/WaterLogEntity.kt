package com.defat.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per logical day (plan §11): the day's total in ml. */
@Entity(tableName = "water_log")
data class WaterLogEntity(
    @PrimaryKey val dateEpochDay: Long,
    val ml: Int,
)

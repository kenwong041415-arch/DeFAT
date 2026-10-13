package com.defat.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A manually logged session. [kcal] is stored, never recomputed (plan §10.2). */
@Entity(tableName = "exercise_entries", indices = [Index("loggedAtMillis")])
data class ExerciseEntryEntity(
    @PrimaryKey val id: String,
    val loggedAtMillis: Long,
    val activityKey: String,
    val met: Double,
    val minutes: Int,
    val weightKgAtTime: Double,
    val kcal: Double,
    val note: String?,
)

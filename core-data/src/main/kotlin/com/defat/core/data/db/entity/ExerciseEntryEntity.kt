package com.defat.core.data.db.entity

import androidx.room.ColumnInfo
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
    /**
     * Last local write (phase1.5 sync `updatedAt`). The column default MUST stay
     * identical to the DEFAULT in SyncMigrations.MIGRATION_3_4_STATEMENTS: SQLite
     * refuses `ADD COLUMN ... NOT NULL` without one, and Room throws at launch if
     * the declared and actual defaults differ (the Phase 1.6 `mealType` trap).
     * Real rows always carry a real timestamp; 0 only ever exists transiently
     * inside the migration, before its back-fill UPDATE.
     */
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0L,
    /** See MealEntity.syncedAtMillis. */
    val syncedAtMillis: Long? = null,
    /** Tombstone; see MealEntity.deletedAtMillis. */
    val deletedAtMillis: Long? = null,
)

package com.defat.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "meals", indices = [Index("date")])
data class MealEntity(
    @PrimaryKey val id: String,
    val loggedAtMillis: Long,
    val date: String, // ISO-8601 "yyyy-MM-dd", local date
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val source: String, // MealSource.name
    val updatedAtMillis: Long,
    val remoteId: String? = null, // D8 — Phase 1.5 sync
    val pendingSync: Boolean = true, // D8
    /**
     * MealType.name. The column default MUST stay in sync with the DEFAULT in
     * MealMigrations.MIGRATION_1_2_STATEMENTS — Room compares the declared
     * default against the database's and throws at launch if they differ
     * (plan §3 R4).
     */
    @ColumnInfo(defaultValue = "SNACK")
    val mealType: String = "SNACK",
    /**
     * The 食物庫 row this meal was picked from (plan §8.7), nullable because a
     * typed meal has none. Deliberately no defaultValue: the column is added by
     * `ALTER TABLE ... ADD COLUMN baseFoodId TEXT` with no DEFAULT, so there is
     * nothing for Room to compare. The meal's numbers above are its own and are
     * never recomputed from this row.
     */
    val baseFoodId: String? = null,
    /** Comma-separated modifier ids from 點食法; null for a typed meal. */
    val modifierIds: String? = null,
)

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
)

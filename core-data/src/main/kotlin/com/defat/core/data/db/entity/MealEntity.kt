package com.defat.core.data.db.entity

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
)

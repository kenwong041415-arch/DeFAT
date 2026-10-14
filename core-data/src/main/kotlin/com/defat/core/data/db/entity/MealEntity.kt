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
    /**
     * Last local write, stamped from the injected Clock on every save. Doubles
     * as the sync `updatedAt` (phase1.5 plan §6.1): it has existed since v1, so
     * v4 adds no second copy of it.
     */
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
    /**
     * When this row's current version reached Firestore; null = never. A row is
     * pending when this is null or older than [updatedAtMillis]. Every ordinary
     * write builds a fresh entity with null here, so a write cannot be mistaken
     * for already uploaded. Added in v4 as a nullable column with no DEFAULT.
     */
    val syncedAtMillis: Long? = null,
    /** Tombstone (phase1.5 plan §6.2). Non-null = deleted; every read query filters it out. */
    val deletedAtMillis: Long? = null,
)

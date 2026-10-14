package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.MealEntity
import kotlinx.coroutines.flow.Flow

/**
 * Every read the UI uses filters tombstones (`deletedAtMillis IS NULL`): a
 * deleted meal is a row with a timestamp, not a missing row, so the delete can
 * propagate to the other device (phase1.5 plan §6.2). The `*ForSync` queries
 * are the only ones that see tombstones.
 */
@Dao
interface MealDao {
    /**
     * One logical day, as an instant window computed by DayBoundary in
     * :core-domain (Phase 1.7 plan §7.3) — deliberately not a comparison on
     * the stored `date` string, which is the calendar date and ignores the
     * user's day-start time. Half-open: [startMillis, endMillis).
     */
    @Query(
        "SELECT * FROM meals WHERE deletedAtMillis IS NULL AND loggedAtMillis >= :startMillis " +
            "AND loggedAtMillis < :endMillis ORDER BY loggedAtMillis ASC",
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<MealEntity>>

    /** Every meal, oldest first. Bucketed into logical days in :core-domain. */
    @Query("SELECT * FROM meals WHERE deletedAtMillis IS NULL ORDER BY loggedAtMillis ASC")
    fun observeAll(): Flow<List<MealEntity>>

    /** Newest first; the frequent-food ranking is done in :core-domain. */
    @Query("SELECT * FROM meals WHERE deletedAtMillis IS NULL ORDER BY loggedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id AND deletedAtMillis IS NULL")
    suspend fun byId(id: String): MealEntity?

    @Upsert
    suspend fun upsert(meal: MealEntity)

    /**
     * A delete is a write: it sets the tombstone, bumps `updatedAtMillis` so it
     * outranks the version it replaces, and clears `syncedAtMillis` so it is
     * pending. The row stays until the account is deleted.
     */
    @Query(
        "UPDATE meals SET deletedAtMillis = :nowMillis, updatedAtMillis = :nowMillis, syncedAtMillis = NULL " +
            "WHERE id = :id AND deletedAtMillis IS NULL",
    )
    suspend fun softDelete(id: String, nowMillis: Long)

    // ---- sync ----

    @Query("SELECT * FROM meals WHERE ${SyncSql.PENDING}")
    suspend fun pendingForSync(): List<MealEntity>

    /** Tombstones included: conflict resolution must see them. */
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun anyById(id: String): MealEntity?

    @Query("SELECT COUNT(*) FROM meals")
    suspend fun rowCount(): Int

    /** Conditional on `updatedAtMillis`, so an edit made during the upload stays pending. */
    @Query("UPDATE meals SET syncedAtMillis = :updatedAtMillis WHERE id = :id AND updatedAtMillis = :updatedAtMillis")
    suspend fun markSynced(id: String, updatedAtMillis: Long)

    @Query("UPDATE meals SET syncedAtMillis = NULL")
    suspend fun markAllPending()

    @Query("DELETE FROM meals")
    suspend fun deleteAllRows()

    @Query("SELECT * FROM meals WHERE deletedAtMillis IS NULL ORDER BY loggedAtMillis ASC")
    suspend fun liveForExport(): List<MealEntity>
}

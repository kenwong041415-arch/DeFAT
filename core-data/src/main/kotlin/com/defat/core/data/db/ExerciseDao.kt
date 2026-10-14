package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.ExerciseEntryEntity
import kotlinx.coroutines.flow.Flow

/** Deletes are tombstones; see MealDao for why every UI read filters them. */
@Dao
interface ExerciseDao {
    /** One logical day as an instant window from DayBoundary; half-open `[start, end)`. */
    @Query(
        "SELECT * FROM exercise_entries WHERE deletedAtMillis IS NULL AND loggedAtMillis >= :startMillis " +
            "AND loggedAtMillis < :endMillis ORDER BY loggedAtMillis ASC",
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ExerciseEntryEntity>>

    /** Every entry, oldest first. Bucketed into logical days in :core-domain. */
    @Query("SELECT * FROM exercise_entries WHERE deletedAtMillis IS NULL ORDER BY loggedAtMillis ASC")
    fun observeAll(): Flow<List<ExerciseEntryEntity>>

    @Upsert
    suspend fun upsert(entry: ExerciseEntryEntity)

    @Query(
        "UPDATE exercise_entries SET deletedAtMillis = :nowMillis, updatedAtMillis = :nowMillis, syncedAtMillis = NULL " +
            "WHERE id = :id AND deletedAtMillis IS NULL",
    )
    suspend fun softDelete(id: String, nowMillis: Long)

    // ---- sync ----

    @Query("SELECT * FROM exercise_entries WHERE ${SyncSql.PENDING}")
    suspend fun pendingForSync(): List<ExerciseEntryEntity>

    @Query("SELECT * FROM exercise_entries WHERE id = :id")
    suspend fun anyById(id: String): ExerciseEntryEntity?

    @Query("SELECT COUNT(*) FROM exercise_entries")
    suspend fun rowCount(): Int

    @Query("UPDATE exercise_entries SET syncedAtMillis = :updatedAtMillis WHERE id = :id AND updatedAtMillis = :updatedAtMillis")
    suspend fun markSynced(id: String, updatedAtMillis: Long)

    @Query("UPDATE exercise_entries SET syncedAtMillis = NULL")
    suspend fun markAllPending()

    @Query("DELETE FROM exercise_entries")
    suspend fun deleteAllRows()

    @Query("SELECT * FROM exercise_entries WHERE deletedAtMillis IS NULL ORDER BY loggedAtMillis ASC")
    suspend fun liveForExport(): List<ExerciseEntryEntity>
}

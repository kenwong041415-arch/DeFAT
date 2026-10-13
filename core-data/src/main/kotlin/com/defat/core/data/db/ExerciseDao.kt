package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.ExerciseEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    /** One logical day as an instant window from DayBoundary; half-open `[start, end)`. */
    @Query("SELECT * FROM exercise_entries WHERE loggedAtMillis >= :startMillis AND loggedAtMillis < :endMillis ORDER BY loggedAtMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ExerciseEntryEntity>>

    /** Every entry, oldest first. Bucketed into logical days in :core-domain. */
    @Query("SELECT * FROM exercise_entries ORDER BY loggedAtMillis ASC")
    fun observeAll(): Flow<List<ExerciseEntryEntity>>

    @Upsert
    suspend fun upsert(entry: ExerciseEntryEntity)

    @Query("DELETE FROM exercise_entries WHERE id = :id")
    suspend fun delete(id: String)
}

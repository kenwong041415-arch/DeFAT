package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    /**
     * One logical day, as an instant window computed by DayBoundary in
     * :core-domain (Phase 1.7 plan §7.3) — deliberately not a comparison on
     * the stored `date` string, which is the calendar date and ignores the
     * user's day-start time. Half-open: [startMillis, endMillis).
     */
    @Query("SELECT * FROM meals WHERE loggedAtMillis >= :startMillis AND loggedAtMillis < :endMillis ORDER BY loggedAtMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<MealEntity>>

    /** Every meal, oldest first. Bucketed into logical days in :core-domain. */
    @Query("SELECT * FROM meals ORDER BY loggedAtMillis ASC")
    fun observeAll(): Flow<List<MealEntity>>

    /** Newest first; the frequent-food ranking is done in :core-domain. */
    @Query("SELECT * FROM meals ORDER BY loggedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun byId(id: String): MealEntity?

    @Upsert
    suspend fun upsert(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: String)
}

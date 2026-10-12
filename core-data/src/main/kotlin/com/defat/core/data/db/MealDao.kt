package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date ORDER BY loggedAtMillis ASC")
    fun observeByDate(date: String): Flow<List<MealEntity>>

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

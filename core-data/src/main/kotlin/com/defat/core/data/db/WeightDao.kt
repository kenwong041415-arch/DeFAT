package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>

    @Query("SELECT * FROM weight_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeBetween(from: String, to: String): Flow<List<WeightEntryEntity>>

    @Upsert
    suspend fun upsert(entry: WeightEntryEntity)
}

package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    /** Null when nothing has been logged for that day yet. */
    @Query("SELECT ml FROM water_log WHERE dateEpochDay = :epochDay")
    fun observeMl(epochDay: Long): Flow<Int?>

    @Upsert
    suspend fun upsert(entry: WaterLogEntity)
}

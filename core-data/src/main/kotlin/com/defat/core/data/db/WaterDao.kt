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

    // ---- sync (a day's total is overwritten, never deleted) ----

    @Query("SELECT * FROM water_log WHERE ${SyncSql.PENDING}")
    suspend fun pendingForSync(): List<WaterLogEntity>

    @Query("SELECT * FROM water_log WHERE dateEpochDay = :epochDay")
    suspend fun anyByDay(epochDay: Long): WaterLogEntity?

    @Query("SELECT COUNT(*) FROM water_log")
    suspend fun rowCount(): Int

    @Query("UPDATE water_log SET syncedAtMillis = :updatedAtMillis WHERE dateEpochDay = :epochDay AND updatedAtMillis = :updatedAtMillis")
    suspend fun markSynced(epochDay: Long, updatedAtMillis: Long)

    @Query("UPDATE water_log SET syncedAtMillis = NULL")
    suspend fun markAllPending()

    @Query("DELETE FROM water_log")
    suspend fun deleteAllRows()

    @Query("SELECT * FROM water_log ORDER BY dateEpochDay ASC")
    suspend fun allForExport(): List<WaterLogEntity>
}

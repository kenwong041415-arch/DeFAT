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

    // ---- sync (weights are never deleted, so there is no tombstone) ----

    @Query("SELECT * FROM weight_entries WHERE ${SyncSql.PENDING}")
    suspend fun pendingForSync(): List<WeightEntryEntity>

    @Query("SELECT * FROM weight_entries WHERE date = :date")
    suspend fun anyByDate(date: String): WeightEntryEntity?

    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun rowCount(): Int

    @Query("UPDATE weight_entries SET syncedAtMillis = :updatedAtMillis WHERE date = :date AND updatedAtMillis = :updatedAtMillis")
    suspend fun markSynced(date: String, updatedAtMillis: Long)

    @Query("UPDATE weight_entries SET syncedAtMillis = NULL")
    suspend fun markAllPending()

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAllRows()

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    suspend fun allForExport(): List<WeightEntryEntity>
}

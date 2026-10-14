package com.defat.core.data.sync

import androidx.room.withTransaction
import com.defat.core.data.db.DefatDatabase
import com.defat.core.data.db.ExerciseDao
import com.defat.core.data.db.FoodDao
import com.defat.core.data.db.FoodMigrations
import com.defat.core.data.db.MealDao
import com.defat.core.data.db.WaterDao
import com.defat.core.data.db.WeightDao
import com.defat.core.data.di.IoDispatcher
import com.defat.core.domain.sync.SyncCollections
import com.defat.core.domain.sync.SyncDoc
import com.defat.core.domain.sync.SyncStamp
import com.defat.core.domain.sync.SyncableCollection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/*
 * One SyncableCollection per Room table. Each is a thin wrapper: the rules
 * (what is pending, who wins a conflict, batching) live in :core-domain where
 * they are unit-tested; these only translate to and from the DAOs.
 */

@Singleton
class MealsSyncSource @Inject constructor(
    private val dao: MealDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncableCollection {
    override val name = SyncCollections.MEALS

    override suspend fun hasRows() = withContext(io) { dao.rowCount() > 0 }
    override suspend fun pending() = withContext(io) { dao.pendingForSync().map { it.toSyncDoc() } }
    override suspend fun markSynced(doc: SyncDoc) = withContext(io) { dao.markSynced(doc.id, doc.updatedAt) }
    override suspend fun stamp(id: String) = withContext(io) {
        dao.anyById(id)?.let { SyncStamp(it.updatedAtMillis, it.deletedAtMillis) }
    }

    override suspend fun applyRemote(doc: SyncDoc) = withContext(io) {
        doc.toMealEntityOrNull()?.let { dao.upsert(it) }
        Unit
    }

    override suspend fun markAllPending() = withContext(io) { dao.markAllPending() }
    override suspend fun deleteAllLocal() = withContext(io) { dao.deleteAllRows() }
    override suspend fun liveDocs() = withContext(io) { dao.liveForExport().map { it.toSyncDoc() } }
}

@Singleton
class WeightsSyncSource @Inject constructor(
    private val dao: WeightDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncableCollection {
    override val name = SyncCollections.WEIGHTS

    override suspend fun hasRows() = withContext(io) { dao.rowCount() > 0 }
    override suspend fun pending() = withContext(io) { dao.pendingForSync().map { it.toSyncDoc() } }
    override suspend fun markSynced(doc: SyncDoc) = withContext(io) { dao.markSynced(doc.id, doc.updatedAt) }
    override suspend fun stamp(id: String) = withContext(io) {
        dao.anyByDate(id)?.let { SyncStamp(it.updatedAtMillis) }
    }

    override suspend fun applyRemote(doc: SyncDoc) = withContext(io) {
        doc.toWeightEntityOrNull()?.let { dao.upsert(it) }
        Unit
    }

    override suspend fun markAllPending() = withContext(io) { dao.markAllPending() }
    override suspend fun deleteAllLocal() = withContext(io) { dao.deleteAllRows() }
    override suspend fun liveDocs() = withContext(io) { dao.allForExport().map { it.toSyncDoc() } }
}

@Singleton
class ExerciseSyncSource @Inject constructor(
    private val dao: ExerciseDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncableCollection {
    override val name = SyncCollections.EXERCISE

    override suspend fun hasRows() = withContext(io) { dao.rowCount() > 0 }
    override suspend fun pending() = withContext(io) { dao.pendingForSync().map { it.toSyncDoc() } }
    override suspend fun markSynced(doc: SyncDoc) = withContext(io) { dao.markSynced(doc.id, doc.updatedAt) }
    override suspend fun stamp(id: String) = withContext(io) {
        dao.anyById(id)?.let { SyncStamp(it.updatedAtMillis, it.deletedAtMillis) }
    }

    override suspend fun applyRemote(doc: SyncDoc) = withContext(io) {
        doc.toExerciseEntityOrNull()?.let { dao.upsert(it) }
        Unit
    }

    override suspend fun markAllPending() = withContext(io) { dao.markAllPending() }
    override suspend fun deleteAllLocal() = withContext(io) { dao.deleteAllRows() }
    override suspend fun liveDocs() = withContext(io) { dao.liveForExport().map { it.toSyncDoc() } }
}

@Singleton
class WaterSyncSource @Inject constructor(
    private val dao: WaterDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncableCollection {
    override val name = SyncCollections.WATER

    override suspend fun hasRows() = withContext(io) { dao.rowCount() > 0 }
    override suspend fun pending() = withContext(io) { dao.pendingForSync().map { it.toSyncDoc() } }
    override suspend fun markSynced(doc: SyncDoc) = withContext(io) {
        doc.id.toLongOrNull()?.let { dao.markSynced(it, doc.updatedAt) }
        Unit
    }

    override suspend fun stamp(id: String) = withContext(io) {
        id.toLongOrNull()?.let { day -> dao.anyByDay(day)?.let { SyncStamp(it.updatedAtMillis) } }
    }

    override suspend fun applyRemote(doc: SyncDoc) = withContext(io) {
        doc.toWaterEntityOrNull()?.let { dao.upsert(it) }
        Unit
    }

    override suspend fun markAllPending() = withContext(io) { dao.markAllPending() }
    override suspend fun deleteAllLocal() = withContext(io) { dao.deleteAllRows() }
    override suspend fun liveDocs() = withContext(io) { dao.allForExport().map { it.toSyncDoc() } }
}

/**
 * Owner-edited `food_items` rows only. The seeded rows are identical on every
 * install and never leave the phone (phase1.5 plan §4).
 */
@Singleton
class FoodOverridesSyncSource @Inject constructor(
    private val database: DefatDatabase,
    private val dao: FoodDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncableCollection {
    override val name = SyncCollections.FOOD_OVERRIDES

    override suspend fun hasRows() = withContext(io) { dao.overrideCount() > 0 }
    override suspend fun pending() = withContext(io) { dao.pendingOverrides().map { it.toSyncDoc() } }
    override suspend fun markSynced(doc: SyncDoc) = withContext(io) { dao.markOverrideSynced(doc.id, doc.updatedAt) }

    /** An unedited seed row has no stamp, so any pulled override wins over it. */
    override suspend fun stamp(id: String) = withContext(io) {
        dao.itemById(id)?.takeIf { it.editedByOwner }?.let { SyncStamp(it.updatedAtMillis ?: 0L) }
    }

    override suspend fun applyRemote(doc: SyncDoc) = withContext(io) {
        doc.toFoodOverrideEntityOrNull()?.let { dao.upsertItem(it) }
        Unit
    }

    override suspend fun markAllPending() = withContext(io) { dao.markAllOverridesPending() }

    /**
     * Back to the factory library: the owner's edits belong to the account being
     * removed or replaced. The library is re-seeded in the same transaction so
     * the app is never left with an empty 食物庫 (`onCreate` only runs on a
     * fresh install, so nothing else would refill it).
     */
    override suspend fun deleteAllLocal() = withContext(io) {
        database.withTransaction {
            dao.deleteAllItems()
            dao.deleteAllModifiers()
            val sqlite = database.openHelper.writableDatabase
            FoodMigrations.SEED_STATEMENTS.forEach(sqlite::execSQL)
        }
    }

    override suspend fun liveDocs() = withContext(io) { dao.allOverrides().map { it.toSyncDoc() } }
}

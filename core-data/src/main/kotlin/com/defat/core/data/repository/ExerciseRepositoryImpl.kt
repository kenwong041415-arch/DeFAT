package com.defat.core.data.repository

import com.defat.core.data.db.ExerciseDao
import com.defat.core.data.di.IoDispatcher
import com.defat.core.data.mapper.toDomain
import com.defat.core.data.mapper.toEntity
import com.defat.core.domain.model.ExerciseEntry
import com.defat.core.domain.repository.ExerciseRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ExerciseRepository {

    override fun exerciseBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<ExerciseEntry>> =
        exerciseDao.observeBetween(startInclusive.toEpochMilli(), endExclusive.toEpochMilli())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override fun allExercise(): Flow<List<ExerciseEntry>> =
        exerciseDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun upsert(entry: ExerciseEntry) = withContext(io) {
        exerciseDao.upsert(entry.toEntity(updatedAtMillis = clock.millis()))
    }

    /** A tombstone, not a row delete: the deletion has to sync (phase1.5 plan §6.2). */
    override suspend fun delete(id: String) = withContext(io) {
        exerciseDao.softDelete(id, clock.millis())
    }
}

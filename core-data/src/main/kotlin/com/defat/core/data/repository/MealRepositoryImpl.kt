package com.defat.core.data.repository

import com.defat.core.data.db.MealDao
import com.defat.core.data.di.IoDispatcher
import com.defat.core.data.mapper.toDomain
import com.defat.core.data.mapper.toEntity
import com.defat.core.domain.model.Meal
import com.defat.core.domain.repository.MealRepository
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
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MealRepository {

    override fun mealsBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<Meal>> =
        mealDao.observeBetween(startInclusive.toEpochMilli(), endExclusive.toEpochMilli())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override fun allMeals(): Flow<List<Meal>> =
        mealDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override fun recentMeals(limit: Int): Flow<List<Meal>> =
        mealDao.observeRecent(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun mealById(id: String): Meal? = withContext(io) {
        mealDao.byId(id)?.toDomain()
    }

    override suspend fun upsert(meal: Meal) = withContext(io) {
        mealDao.upsert(meal.toEntity(updatedAtMillis = clock.millis()))
    }

    /** A tombstone, not a row delete: the deletion has to sync (phase1.5 plan §6.2). */
    override suspend fun delete(id: String) = withContext(io) {
        mealDao.softDelete(id, clock.millis())
    }
}

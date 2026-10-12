package com.defat.core.data.repository

import com.defat.core.data.db.MealDao
import com.defat.core.data.di.IoDispatcher
import com.defat.core.data.mapper.toDomain
import com.defat.core.data.mapper.toEntity
import com.defat.core.domain.model.Meal
import com.defat.core.domain.repository.MealRepository
import java.time.LocalDate
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
    @IoDispatcher private val io: CoroutineDispatcher,
) : MealRepository {

    override fun mealsOn(date: LocalDate): Flow<List<Meal>> =
        mealDao.observeByDate(date.toString())
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
        mealDao.upsert(meal.toEntity())
    }

    override suspend fun delete(id: String) = withContext(io) {
        mealDao.delete(id)
    }
}

package com.defat.core.data.repository

import com.defat.core.data.db.WeightDao
import com.defat.core.data.di.IoDispatcher
import com.defat.core.data.mapper.toDomain
import com.defat.core.data.mapper.toEntity
import com.defat.core.domain.model.WeightEntry
import com.defat.core.domain.repository.WeightRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val weightDao: WeightDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WeightRepository {

    override val latest: Flow<WeightEntry?> =
        weightDao.observeLatest().map { it?.toDomain() }.flowOn(io)

    override fun between(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>> =
        weightDao.observeBetween(from.toString(), to.toString())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(io)

    override suspend fun upsert(entry: WeightEntry) = withContext(io) {
        weightDao.upsert(entry.toEntity())
    }
}

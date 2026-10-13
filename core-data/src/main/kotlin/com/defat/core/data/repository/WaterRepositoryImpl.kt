package com.defat.core.data.repository

import com.defat.core.data.db.WaterDao
import com.defat.core.data.db.entity.WaterLogEntity
import com.defat.core.data.di.IoDispatcher
import com.defat.core.domain.repository.WaterRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class WaterRepositoryImpl @Inject constructor(
    private val waterDao: WaterDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WaterRepository {

    override fun waterMl(date: LocalDate): Flow<Int> =
        waterDao.observeMl(date.toEpochDay())
            .map { it ?: 0 }
            .distinctUntilChanged()
            .flowOn(io)

    override suspend fun setWaterMl(date: LocalDate, ml: Int) = withContext(io) {
        waterDao.upsert(WaterLogEntity(dateEpochDay = date.toEpochDay(), ml = ml.coerceAtLeast(0)))
    }
}

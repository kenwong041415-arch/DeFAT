package com.defat.app.fake

import com.defat.core.domain.repository.WaterRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWaterRepository(initial: Map<LocalDate, Int> = emptyMap()) : WaterRepository {

    private val totals = MutableStateFlow(initial)

    override fun waterMl(date: LocalDate): Flow<Int> = totals.map { it[date] ?: 0 }

    override suspend fun setWaterMl(date: LocalDate, ml: Int) {
        totals.value = totals.value + (date to ml)
    }
}

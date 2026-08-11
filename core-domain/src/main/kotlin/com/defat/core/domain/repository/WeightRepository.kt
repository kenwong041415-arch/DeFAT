package com.defat.core.domain.repository

import com.defat.core.domain.model.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    val latest: Flow<WeightEntry?>
    fun between(from: LocalDate, to: LocalDate): Flow<List<WeightEntry>>
    suspend fun upsert(entry: WeightEntry)
}

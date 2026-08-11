package com.defat.app.fake

import com.defat.core.domain.model.WeightEntry
import com.defat.core.domain.repository.WeightRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWeightRepository(initial: List<WeightEntry> = emptyList()) : WeightRepository {

    private val entries = MutableStateFlow(initial)

    override val latest = entries.map { list -> list.maxByOrNull { it.date } }

    override fun between(from: LocalDate, to: LocalDate) =
        entries.map { list -> list.filter { it.date in from..to } }

    override suspend fun upsert(entry: WeightEntry) {
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }
}

package com.defat.app.fake

import com.defat.core.domain.model.ExerciseEntry
import com.defat.core.domain.repository.ExerciseRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExerciseRepository(initial: List<ExerciseEntry> = emptyList()) : ExerciseRepository {

    private val entries = MutableStateFlow(initial)

    var lastUpserted: ExerciseEntry? = null
        private set

    // Window query on the instant, exactly like the real DAO.
    override fun exerciseBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<ExerciseEntry>> =
        entries.map { list ->
            list.filter { it.loggedAt >= startInclusive && it.loggedAt < endExclusive }
                .sortedBy { it.loggedAt }
        }

    override fun allExercise(): Flow<List<ExerciseEntry>> = entries.map { list -> list.sortedBy { it.loggedAt } }

    override suspend fun upsert(entry: ExerciseEntry) {
        lastUpserted = entry
        entries.value = entries.value.filterNot { it.id == entry.id } + entry
    }

    override suspend fun delete(id: String) {
        entries.value = entries.value.filterNot { it.id == id }
    }
}

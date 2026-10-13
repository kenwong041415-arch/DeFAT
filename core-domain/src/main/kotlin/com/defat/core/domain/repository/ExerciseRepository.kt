package com.defat.core.domain.repository

import com.defat.core.domain.model.ExerciseEntry
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    /**
     * Entries whose [ExerciseEntry.loggedAt] lies in `[startInclusive, endExclusive)`,
     * ascending by time. Like meals, days are an instant window computed by
     * DayBoundary — see [exerciseOnLogicalDay].
     */
    fun exerciseBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<ExerciseEntry>>

    /** Every entry ever logged, ascending by time. Feeds the History list's per-day targets. */
    fun allExercise(): Flow<List<ExerciseEntry>>

    suspend fun upsert(entry: ExerciseEntry)
    suspend fun delete(id: String)
}

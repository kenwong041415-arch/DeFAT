package com.defat.core.domain.repository

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/**
 * App-level preferences that are not part of the health profile. Phase 1.7 has
 * exactly one: the time of day at which a new logical day starts (plan §7).
 */
interface SettingsRepository {
    /**
     * Emits the current day-start time, 04:00 until the user changes it.
     * Always a value on the 00:00..08:00, 30-minute grid
     * ([com.defat.core.domain.calc.DayBoundary.dayStartFromMinutes]).
     */
    val dayStart: Flow<LocalTime>

    /** Normalised through DayBoundary.dayStartFromMinutes before it is stored. */
    suspend fun setDayStart(time: LocalTime)
}

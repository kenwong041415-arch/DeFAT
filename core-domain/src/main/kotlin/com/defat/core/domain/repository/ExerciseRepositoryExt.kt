package com.defat.core.domain.repository

import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.model.ExerciseEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

/** One logical day's exercise: the [DayBoundary] window for [date] (plan §7.3). */
fun ExerciseRepository.exerciseOnLogicalDay(
    date: LocalDate,
    zone: ZoneId,
    dayStart: LocalTime,
): Flow<List<ExerciseEntry>> = exerciseBetween(
    DayBoundary.startOfLogicalDay(date, zone, dayStart),
    DayBoundary.endOfLogicalDayExclusive(date, zone, dayStart),
)

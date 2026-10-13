package com.defat.core.domain.repository

import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.model.Meal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

/** One logical day's meals: the [DayBoundary] window for [date] (plan §7.3). */
fun MealRepository.mealsOnLogicalDay(
    date: LocalDate,
    zone: ZoneId,
    dayStart: LocalTime,
): Flow<List<Meal>> = mealsBetween(
    DayBoundary.startOfLogicalDay(date, zone, dayStart),
    DayBoundary.endOfLogicalDayExclusive(date, zone, dayStart),
)

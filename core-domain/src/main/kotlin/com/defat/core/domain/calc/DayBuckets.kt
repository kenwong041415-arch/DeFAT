package com.defat.core.domain.calc

import com.defat.core.domain.model.Meal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Groups meals by the logical day their instant falls in. Uses
 * [Meal.loggedAt] and [DayBoundary], never the stored [Meal.date] string —
 * rows logged between 00:00 and the day-start time before the setting existed
 * carry a calendar date that no longer matches their logical day (plan §17 R1).
 */
object DayBuckets {

    fun byLogicalDate(
        meals: List<Meal>,
        zone: ZoneId,
        dayStart: LocalTime,
    ): Map<LocalDate, List<Meal>> =
        meals.groupBy { DayBoundary.logicalDate(it.loggedAt, zone, dayStart) }

    fun loggedDates(meals: List<Meal>, zone: ZoneId, dayStart: LocalTime): Set<LocalDate> =
        meals.mapTo(HashSet()) { DayBoundary.logicalDate(it.loggedAt, zone, dayStart) }
}

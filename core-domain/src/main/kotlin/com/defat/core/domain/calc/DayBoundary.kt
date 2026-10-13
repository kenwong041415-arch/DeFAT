package com.defat.core.domain.calc

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Maps an instant to the logical day it belongs to, given the user's
 * day-start time. A dayStart of 04:00 means 2026-10-13T01:30 local is still
 * logged against 2026-10-12.
 *
 * Boundaries are half-open on the lower bound: an instant exactly at
 * [dayStart] belongs to the new day. A logical day is the window
 * `[startOfLogicalDay, endOfLogicalDayExclusive)`.
 *
 * Phase 1.7 plan §7. [MealTypeInference] deliberately does NOT use this: it
 * maps a wall-clock time to a meal type regardless of which logical day that
 * time belongs to.
 *
 * Daylight-saving note: the window is built from local date-times, so on a
 * DST-change day it can be 23 or 25 hours long. Hong Kong has no DST, and the
 * windows still tile the timeline without gaps or overlap, which is what the
 * bucketing needs.
 */
object DayBoundary {

    val DEFAULT_DAY_START: LocalTime = LocalTime.of(4, 0)

    /** Day-start choices offered in the setting: 00:00..08:00 in 30-minute steps. */
    const val MIN_DAY_START_MINUTES: Int = 0
    const val MAX_DAY_START_MINUTES: Int = 8 * 60
    const val DAY_START_STEP_MINUTES: Int = 30

    fun logicalDate(at: Instant, zone: ZoneId, dayStart: LocalTime): LocalDate {
        val local = at.atZone(zone).toLocalDateTime()
        return if (local.toLocalTime().isBefore(dayStart)) {
            local.toLocalDate().minusDays(1)
        } else {
            local.toLocalDate()
        }
    }

    fun startOfLogicalDay(date: LocalDate, zone: ZoneId, dayStart: LocalTime): Instant =
        date.atTime(dayStart).atZone(zone).toInstant()

    fun endOfLogicalDayExclusive(date: LocalDate, zone: ZoneId, dayStart: LocalTime): Instant =
        date.plusDays(1).atTime(dayStart).atZone(zone).toInstant()

    /**
     * The inverse of [logicalDate] for a (logical date, wall-clock time) pair:
     * the instant a user means when they say "the 12th, at 01:30". A wall-clock
     * time before [dayStart] falls on the *next* calendar date, because that is
     * the only way it can belong to logical date [date]. Always satisfies
     * `logicalDate(instantAt(d, t, z, s), z, s) == d`.
     */
    fun instantAt(date: LocalDate, time: LocalTime, zone: ZoneId, dayStart: LocalTime): Instant {
        val calendarDate = if (time.isBefore(dayStart)) date.plusDays(1) else date
        return calendarDate.atTime(time).atZone(zone).toInstant()
    }

    /**
     * Normalises a stored/typed day-start value (minutes from midnight) to the
     * offered range: clamped to 00:00..08:00 and floored to a 30-minute step.
     */
    fun dayStartFromMinutes(minutes: Int): LocalTime {
        val clamped = minutes.coerceIn(MIN_DAY_START_MINUTES, MAX_DAY_START_MINUTES)
        val snapped = clamped - clamped % DAY_START_STEP_MINUTES
        return LocalTime.of(snapped / 60, snapped % 60)
    }

    fun toMinutes(dayStart: LocalTime): Int = dayStart.hour * 60 + dayStart.minute
}

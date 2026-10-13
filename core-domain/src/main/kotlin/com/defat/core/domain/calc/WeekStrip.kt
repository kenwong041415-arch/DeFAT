package com.defat.core.domain.calc

import com.defat.core.domain.model.WeekDayCell
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Pure calendar logic for Home's week strip (plan §6.3): Monday-first (the
 * Hong Kong convention), seven cells, future days present but disabled.
 */
object WeekStrip {

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** The seven cells of the week that contains [selected]. */
    fun cells(selected: LocalDate, today: LocalDate, loggedDates: Set<LocalDate>): List<WeekDayCell> {
        val start = weekStart(selected)
        return (0L until DAYS_PER_WEEK).map { offset ->
            val date = start.plusDays(offset)
            WeekDayCell(
                date = date,
                isSelected = date == selected,
                isToday = date == today,
                hasEntries = date in loggedDates,
                isFuture = date.isAfter(today),
            )
        }
    }

    /**
     * The day to select after swiping to the previous week, or null when that
     * week lies entirely before the earliest logged day. Keeps the same weekday
     * where it can; otherwise lands on the earliest logged day.
     */
    fun previousWeekTarget(selected: LocalDate, earliestLogged: LocalDate?, today: LocalDate): LocalDate? {
        val floor = minOf(earliestLogged ?: today, today)
        val previousWeekEnd = weekStart(selected).minusWeeks(1).plusDays(DAYS_PER_WEEK - 1)
        if (previousWeekEnd.isBefore(floor)) return null
        val sameWeekday = selected.minusWeeks(1)
        return if (sameWeekday.isBefore(floor)) floor else sameWeekday
    }

    /**
     * The day to select after swiping to the next week, or null when [selected]
     * is already in the current week. Never goes past [today].
     */
    fun nextWeekTarget(selected: LocalDate, today: LocalDate): LocalDate? {
        val nextWeekStart = weekStart(selected).plusWeeks(1)
        if (nextWeekStart.isAfter(today)) return null
        val sameWeekday = selected.plusWeeks(1)
        return if (sameWeekday.isAfter(today)) today else sameWeekday
    }

    private const val DAYS_PER_WEEK = 7L
}

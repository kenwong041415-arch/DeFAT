package com.defat.core.domain.calc

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class WeekStripTest {

    // 2026-10-13 is a Tuesday.
    private val today = LocalDate.of(2026, 10, 13)

    @Test
    fun `builds seven cells starting on Monday`() {
        val cells = WeekStrip.cells(selected = today, today = today, loggedDates = emptySet())

        assertEquals(7, cells.size)
        assertEquals(LocalDate.of(2026, 10, 12), cells.first().date)
        assertEquals(DayOfWeek.MONDAY, cells.first().date.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, cells.last().date.dayOfWeek)
    }

    @Test
    fun `days after today are future and today is not`() {
        val cells = WeekStrip.cells(selected = today, today = today, loggedDates = emptySet())

        // Mon 12 and Tue 13 (today) are not future; Wed 14 .. Sun 18 are.
        assertEquals(listOf(false, false, true, true, true, true, true), cells.map { it.isFuture })
        assertFalse(cells[1].isFuture)
        assertTrue(cells[2].isFuture)
    }

    @Test
    fun `marks the selected day, today and days with entries`() {
        val monday = LocalDate.of(2026, 10, 12)
        val cells = WeekStrip.cells(selected = monday, today = today, loggedDates = setOf(monday))

        assertTrue(cells[0].isSelected)
        assertTrue(cells[0].hasEntries)
        assertFalse(cells[0].isToday)
        assertTrue(cells[1].isToday)
        assertFalse(cells[1].isSelected)
        assertFalse(cells[1].hasEntries)
    }

    @Test
    fun `a selected day in a past week shows that week`() {
        val cells = WeekStrip.cells(selected = LocalDate.of(2026, 10, 7), today = today, loggedDates = emptySet())

        assertEquals(LocalDate.of(2026, 10, 5), cells.first().date)
        assertTrue(cells.none { it.isFuture })
    }

    @Test
    fun `previous week keeps the weekday`() {
        val target = WeekStrip.previousWeekTarget(
            selected = today,
            earliestLogged = LocalDate.of(2026, 9, 1),
            today = today,
        )
        assertEquals(LocalDate.of(2026, 10, 6), target)
    }

    @Test
    fun `previous week stops at the earliest logged day`() {
        // Earliest meal is Thursday 2026-10-08; the week before today's contains it.
        val clamped = WeekStrip.previousWeekTarget(
            selected = today,
            earliestLogged = LocalDate.of(2026, 10, 8),
            today = today,
        )
        assertEquals(LocalDate.of(2026, 10, 8), clamped)

        // From inside that earliest week there is nothing further back.
        val none = WeekStrip.previousWeekTarget(
            selected = LocalDate.of(2026, 10, 8),
            earliestLogged = LocalDate.of(2026, 10, 8),
            today = today,
        )
        assertNull(none)
    }

    @Test
    fun `with nothing logged the strip cannot go back from the current week`() {
        assertNull(WeekStrip.previousWeekTarget(selected = today, earliestLogged = null, today = today))
    }

    @Test
    fun `next week is capped at today and absent from the current week`() {
        assertNull(WeekStrip.nextWeekTarget(selected = today, today = today))
        assertEquals(
            LocalDate.of(2026, 10, 13),
            WeekStrip.nextWeekTarget(selected = LocalDate.of(2026, 10, 9), today = today),
        )
        assertEquals(
            LocalDate.of(2026, 10, 7),
            WeekStrip.nextWeekTarget(selected = LocalDate.of(2026, 9, 30), today = today),
        )
    }
}

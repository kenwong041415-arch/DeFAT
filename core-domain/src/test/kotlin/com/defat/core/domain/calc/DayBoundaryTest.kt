package com.defat.core.domain.calc

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import org.junit.Test

class DayBoundaryTest {

    private val zone = ZoneId.of("Asia/Hong_Kong")
    private val dayStart = LocalTime.of(4, 0)

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()

    @Test
    fun `03-59 still belongs to the previous day`() {
        assertEquals(
            LocalDate.of(2026, 10, 12),
            DayBoundary.logicalDate(instant(2026, 10, 13, 3, 59), zone, dayStart),
        )
    }

    @Test
    fun `04-00 exactly starts the new day`() {
        assertEquals(
            LocalDate.of(2026, 10, 13),
            DayBoundary.logicalDate(instant(2026, 10, 13, 4, 0), zone, dayStart),
        )
    }

    @Test
    fun `04-01 belongs to the new day`() {
        assertEquals(
            LocalDate.of(2026, 10, 13),
            DayBoundary.logicalDate(instant(2026, 10, 13, 4, 1), zone, dayStart),
        )
    }

    @Test
    fun `23-59 belongs to the same calendar day`() {
        assertEquals(
            LocalDate.of(2026, 10, 13),
            DayBoundary.logicalDate(instant(2026, 10, 13, 23, 59), zone, dayStart),
        )
    }

    @Test
    fun `00-30 after midnight belongs to the previous day`() {
        assertEquals(
            LocalDate.of(2026, 10, 13),
            DayBoundary.logicalDate(instant(2026, 10, 14, 0, 30), zone, dayStart),
        )
    }

    @Test
    fun `a day start of 00-00 behaves as the plain local date`() {
        val midnight = LocalTime.MIDNIGHT
        assertEquals(
            LocalDate.of(2026, 10, 14),
            DayBoundary.logicalDate(instant(2026, 10, 14, 0, 0), zone, midnight),
        )
        assertEquals(
            LocalDate.of(2026, 10, 13),
            DayBoundary.logicalDate(instant(2026, 10, 13, 23, 59), zone, midnight),
        )
        assertEquals(
            instant(2026, 10, 13, 0, 0),
            DayBoundary.startOfLogicalDay(LocalDate.of(2026, 10, 13), zone, midnight),
        )
    }

    @Test
    fun `start and end of a logical day are exactly 24 hours apart`() {
        val date = LocalDate.of(2026, 10, 13)
        val start = DayBoundary.startOfLogicalDay(date, zone, dayStart)
        val end = DayBoundary.endOfLogicalDayExclusive(date, zone, dayStart)
        assertEquals(instant(2026, 10, 13, 4, 0), start)
        assertEquals(Duration.ofHours(24), Duration.between(start, end))
    }

    @Test
    fun `the window is half-open - an instant equal to the end belongs to the next day`() {
        val date = LocalDate.of(2026, 10, 13)
        val end = DayBoundary.endOfLogicalDayExclusive(date, zone, dayStart)
        assertEquals(date.plusDays(1), DayBoundary.logicalDate(end, zone, dayStart))
        assertEquals(date, DayBoundary.logicalDate(end.minusMillis(1), zone, dayStart))
    }
}

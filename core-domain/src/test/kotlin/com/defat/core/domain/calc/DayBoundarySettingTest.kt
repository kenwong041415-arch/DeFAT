package com.defat.core.domain.calc

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import org.junit.Test

/** The editor round-trip and the setting's 00:00..08:00 / 30-minute normalisation. */
class DayBoundarySettingTest {

    private val zone = ZoneId.of("Asia/Hong_Kong")
    private val dayStart = LocalTime.of(4, 0)

    @Test
    fun `instantAt round-trips through logicalDate for every half hour of the day`() {
        val date = LocalDate.of(2026, 10, 12)
        for (minuteOfDay in 0 until 24 * 60 step 30) {
            val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
            val instant = DayBoundary.instantAt(date, time, zone, dayStart)
            assertEquals(date, DayBoundary.logicalDate(instant, zone, dayStart), "time $time")
        }
    }

    @Test
    fun `a small-hours time on the 12th is stored on the calendar morning of the 13th`() {
        val instant = DayBoundary.instantAt(LocalDate.of(2026, 10, 12), LocalTime.of(1, 30), zone, dayStart)
        assertEquals(LocalDate.of(2026, 10, 13), instant.atZone(zone).toLocalDate())
        assertEquals(LocalTime.of(1, 30), instant.atZone(zone).toLocalTime())
    }

    @Test
    fun `day start minutes are clamped to 00-00 through 08-00`() {
        assertEquals(LocalTime.of(0, 0), DayBoundary.dayStartFromMinutes(-60))
        assertEquals(LocalTime.of(8, 0), DayBoundary.dayStartFromMinutes(9 * 60))
        assertEquals(LocalTime.of(8, 0), DayBoundary.dayStartFromMinutes(Int.MAX_VALUE))
    }

    @Test
    fun `day start minutes snap down to a 30 minute step`() {
        assertEquals(LocalTime.of(4, 0), DayBoundary.dayStartFromMinutes(240))
        assertEquals(LocalTime.of(4, 0), DayBoundary.dayStartFromMinutes(269))
        assertEquals(LocalTime.of(4, 30), DayBoundary.dayStartFromMinutes(270))
    }

    @Test
    fun `toMinutes is the inverse for valid values and the default is 04-00`() {
        assertEquals(240, DayBoundary.toMinutes(DayBoundary.DEFAULT_DAY_START))
        assertEquals(LocalTime.of(5, 30), DayBoundary.dayStartFromMinutes(DayBoundary.toMinutes(LocalTime.of(5, 30))))
    }
}

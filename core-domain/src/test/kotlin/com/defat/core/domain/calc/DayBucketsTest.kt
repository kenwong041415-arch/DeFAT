package com.defat.core.domain.calc

import com.defat.core.domain.fake.testMeal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import org.junit.Test

class DayBucketsTest {

    private val zone = ZoneId.of("Asia/Hong_Kong")

    private fun at(day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant()

    @Test
    fun `buckets by the instant, ignoring the stored date string`() {
        val meals = listOf(
            testMeal("late", at(13, 1, 30)),
            testMeal("noon", at(13, 12, 0)),
        )
        val buckets = DayBuckets.byLogicalDate(meals, zone, LocalTime.of(4, 0))

        assertEquals(setOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)), buckets.keys)
        assertEquals(listOf("late"), buckets.getValue(LocalDate.of(2026, 10, 12)).map { it.id })
    }

    @Test
    fun `logged dates follow the day start`() {
        val meals = listOf(testMeal("late", at(13, 1, 30)))

        assertEquals(setOf(LocalDate.of(2026, 10, 12)), DayBuckets.loggedDates(meals, zone, LocalTime.of(4, 0)))
        assertEquals(setOf(LocalDate.of(2026, 10, 13)), DayBuckets.loggedDates(meals, zone, LocalTime.MIDNIGHT))
    }
}

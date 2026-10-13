package com.defat.core.domain.calc

import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 10, 13)

    private fun days(vararg back: Long): Set<LocalDate> = back.map { today.minusDays(it) }.toSet()

    @Test
    fun `an empty set is 0`() = assertEquals(0, StreakCalculator.streak(emptySet(), today))

    @Test
    fun `today only is 1`() = assertEquals(1, StreakCalculator.streak(days(0), today))

    @Test
    fun `yesterday only is 1 - the streak does not read 0 before the first meal of the day`() =
        assertEquals(1, StreakCalculator.streak(days(1), today))

    @Test
    fun `today and yesterday is 2`() = assertEquals(2, StreakCalculator.streak(days(0, 1), today))

    @Test
    fun `a five-day run ending today is 5`() =
        assertEquals(5, StreakCalculator.streak(days(0, 1, 2, 3, 4), today))

    @Test
    fun `a run ending two days ago is 0`() =
        assertEquals(0, StreakCalculator.streak(days(2, 3, 4), today))

    @Test
    fun `a gap in the middle means only the tail counts`() =
        assertEquals(2, StreakCalculator.streak(days(0, 1, 3, 4, 5), today))

    @Test
    fun `a run ending yesterday counts back from yesterday`() =
        assertEquals(3, StreakCalculator.streak(days(1, 2, 3, 6), today))

    @Test
    fun `a day in the future does not extend or break anything`() {
        val withFuture = days(0, 1) + today.plusDays(1)
        assertEquals(2, StreakCalculator.streak(withFuture, today))
    }
}

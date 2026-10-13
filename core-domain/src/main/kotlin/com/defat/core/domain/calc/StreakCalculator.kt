package com.defat.core.domain.calc

import java.time.LocalDate

/**
 * 跟足計劃日數 (plan §12): a factual count of consecutive logical days that
 * have at least one meal. No freezes, no grace days — a gap of a day or more
 * resets it.
 */
object StreakCalculator {

    /**
     * The length of the run of consecutive dates in [loggedDates] that ends at
     * [today] or, if today has nothing yet, at yesterday. Counting from
     * yesterday means the number does not read 0 all morning before the first
     * meal. Returns 0 when neither today nor yesterday is logged.
     */
    fun streak(loggedDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = when {
            today in loggedDates -> today
            today.minusDays(1) in loggedDates -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (cursor in loggedDates) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}

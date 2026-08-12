package com.defat.core.domain.calc

import com.defat.core.domain.model.MealType
import java.time.LocalTime

/**
 * Maps a clock time to the meal type it most likely belongs to. Boundaries are
 * half-open — a meal at exactly 10:30 is LUNCH, not BREAKFAST.
 *
 * Owner-set boundaries (docs/plans/phase1.6-ui-revision-plan.md §2 E11).
 * Times before 10:30 are BREAKFAST, including the small hours: a 03:00 meal is
 * logged as 早餐. If the owner wants a different cut-off, change
 * [LUNCH_FROM_MINUTE] (or the other constants below) and nothing else.
 */
object MealTypeInference {

    const val BREAKFAST_FROM_MINUTE: Int = 0        // 00:00
    const val LUNCH_FROM_MINUTE: Int = 10 * 60 + 30 // 10:30
    const val AFTERNOON_TEA_FROM_MINUTE: Int = 14 * 60 + 30 // 14:30
    const val DINNER_FROM_MINUTE: Int = 17 * 60 + 30 // 17:30
    const val SNACK_FROM_MINUTE: Int = 21 * 60       // 21:00

    fun forTime(time: LocalTime): MealType =
        forMinuteOfDay(time.hour * 60 + time.minute)

    /**
     * The single source of truth for the rule. The Room v1 -> v2 migration
     * back-fills existing rows with the same boundaries, and
     * MealTypeMigrationSqlTest asserts SQL and Kotlin agree for all 1440
     * minutes of the day.
     */
    fun forMinuteOfDay(minuteOfDay: Int): MealType {
        require(minuteOfDay in 0..1439) {
            "minuteOfDay must be in 0..1439 but was $minuteOfDay"
        }
        return when {
            minuteOfDay < LUNCH_FROM_MINUTE -> MealType.BREAKFAST
            minuteOfDay < AFTERNOON_TEA_FROM_MINUTE -> MealType.LUNCH
            minuteOfDay < DINNER_FROM_MINUTE -> MealType.AFTERNOON_TEA
            minuteOfDay < SNACK_FROM_MINUTE -> MealType.DINNER
            else -> MealType.SNACK
        }
    }
}

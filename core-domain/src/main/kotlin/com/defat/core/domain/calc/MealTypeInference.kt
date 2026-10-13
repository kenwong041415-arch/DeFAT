package com.defat.core.domain.calc

import com.defat.core.domain.model.MealType
import java.time.LocalTime

/**
 * Maps a clock time to the meal type it most likely belongs to. Boundaries are
 * half-open — a meal at exactly 10:30 is LUNCH, not BREAKFAST.
 *
 * Owner-set boundaries (docs/plans/phase1.6-ui-revision-plan.md §2 E11).
 *
 * [forMinuteOfDay] and the no-argument [forTime] are frozen: the shipped Room
 * v1 -> v2 migration back-fills existing rows with exactly these boundaries
 * and `MealTypeMigrationSqlTest` asserts SQL and Kotlin agree for all 1440
 * minutes. Changing them would make the migration disagree with the code.
 *
 * New meals go through [forTime] with a day start instead — see its KDoc for
 * why the small hours need different treatment once a day can start at 04:00.
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
     * Infers the meal type for a new meal, honouring the user's day start.
     *
     * A time before [dayStart] belongs to the *previous* logical day
     * ([DayBoundary]), where it is the last thing eaten after that day's
     * dinner — so it is SNACK. Without this, a 01:30 bowl of noodles would be
     * inferred as BREAKFAST by the frozen boundaries above and then merged
     * into the previous day's 早餐 section next to the real 08:00 breakfast,
     * showing the student two breakfasts and no late-night snack.
     *
     * The Phase 1.7 plan §7.3 claimed [MealTypeInference] needed no change
     * because a 01:30 snack would already be "小食 on yesterday's list". That
     * was wrong — the frozen rule maps anything before 10:30 to BREAKFAST —
     * and the Sonnet implementation pass caught it. The plan was corrected
     * rather than the claim implemented.
     *
     * A [dayStart] of midnight reproduces [forTime] exactly, so nothing
     * changes for a user who never moves the setting.
     */
    fun forTime(time: LocalTime, dayStart: LocalTime): MealType =
        if (time < dayStart) MealType.SNACK else forTime(time)

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

package com.defat.app.ui.home

import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.ExerciseEntry
import com.defat.core.domain.model.MealTypeGroup
import com.defat.core.domain.model.WeekDayCell
import java.time.LocalDate

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NeedsOnboarding : HomeUiState

    data class Content(
        val dayRollup: DayRollup,
        /** All five types, always, in enum order (MealGrouping.groupByType). */
        val mealGroups: List<MealTypeGroup>,
        /** The logical day everything on Home is showing (plan §6.3). */
        val selectedDate: LocalDate,
        /** The current logical day — differs from [selectedDate] when viewing the past. */
        val today: LocalDate,
        /** Monday-first, always seven cells, for the week that contains [selectedDate]. */
        val weekDays: List<WeekDayCell>,
        /** Where a swipe to the previous week lands; null = nothing further back. */
        val previousWeekTarget: LocalDate?,
        /** Where a swipe to the next week lands; null = already in the current week. */
        val nextWeekTarget: LocalDate?,
        /** The selected day's logged exercise, ascending by time (plan §10). */
        val exerciseEntries: List<ExerciseEntry>,
        /** The selected day's water total in ml (plan §11). */
        val waterMl: Int,
        /** 35 ml x bodyweight, a coaching rule of thumb (WaterGoalCalculator). */
        val waterGoalMl: Int,
        /**
         * 跟足計劃日數: consecutive logical days with a meal, counted back from
         * *today* whichever day is selected (plan §12). A factual count.
         */
        val streakDays: Int,
    ) : HomeUiState {
        val isToday: Boolean get() = selectedDate == today
    }
}

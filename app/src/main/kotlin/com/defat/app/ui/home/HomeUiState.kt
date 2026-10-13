package com.defat.app.ui.home

import com.defat.core.domain.model.DayRollup
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
    ) : HomeUiState {
        val isToday: Boolean get() = selectedDate == today
    }
}

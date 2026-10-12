package com.defat.app.ui.history

import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.MealTypeGroup
import java.time.LocalDate

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object NeedsOnboarding : HistoryUiState

    data class Content(
        val date: LocalDate,
        val dayRollup: DayRollup,
        val mealGroups: List<MealTypeGroup>,
        /** false when [date] == today — History never steps into the future. */
        val canGoForward: Boolean,
    ) : HistoryUiState
}

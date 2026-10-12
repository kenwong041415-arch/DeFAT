package com.defat.app.ui.home

import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.MealTypeGroup

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NeedsOnboarding : HomeUiState

    data class Content(
        val dayRollup: DayRollup,
        /** All five types, always, in enum order (MealGrouping.groupByType). */
        val mealGroups: List<MealTypeGroup>,
    ) : HomeUiState
}

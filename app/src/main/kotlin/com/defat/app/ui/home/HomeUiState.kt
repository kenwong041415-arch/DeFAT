package com.defat.app.ui.home

import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.WeightEntry

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NeedsOnboarding : HomeUiState

    /**
     * [meals] is the raw list for today (for the row-by-row list on Home);
     * [dayRollup] carries the aggregated totals from the domain layer
     * (plan §6.7 — DayRollup deliberately has no per-meal detail).
     */
    data class Content(
        val dayRollup: DayRollup,
        val meals: List<Meal>,
        val latestWeight: WeightEntry?,
    ) : HomeUiState
}

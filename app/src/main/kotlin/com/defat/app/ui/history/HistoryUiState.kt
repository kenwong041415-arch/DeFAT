package com.defat.app.ui.history

import com.defat.core.domain.model.DaySummary

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object NeedsOnboarding : HistoryUiState

    data class Content(
        /** Days that have at least one meal, newest first. Empty = nothing logged yet. */
        val days: List<DaySummary>,
    ) : HistoryUiState
}

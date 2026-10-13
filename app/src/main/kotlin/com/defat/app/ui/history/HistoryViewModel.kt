package com.defat.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.app.ui.SelectedDayHolder
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.usecase.ObserveDaySummariesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * The 歷史 tab (plan §6.5): a list of every logged day with its total against
 * the *current* target (Phase 1.6 plan §2 E18 — no per-day snapshot yet).
 * Tapping a day does not render it here; it selects that day on Home, so one
 * place renders a day, not two.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeDaySummaries: ObserveDaySummariesUseCase,
    profileRepository: ProfileRepository,
    private val selectedDay: SelectedDayHolder,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        profileRepository.profile,
        observeDaySummaries(),
    ) { profile, days ->
        if (profile == null) HistoryUiState.NeedsOnboarding else HistoryUiState.Content(days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    /**
     * Makes [date] the day Home shows; the caller then switches to the Today
     * tab. Home clamps a future date (the date picker is permissive) to today.
     */
    fun openDay(date: LocalDate) {
        selectedDay.select(date)
    }
}

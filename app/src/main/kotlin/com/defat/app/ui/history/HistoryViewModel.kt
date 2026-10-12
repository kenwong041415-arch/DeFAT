package com.defat.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.usecase.ObserveTodayDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Picks a date, then shows that day's meals grouped by type and that day's
 * totals against the *current* target (plan §2 E18 — no per-day target
 * snapshot yet, that is Phase 4 trend-engine work).
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeDashboard: ObserveTodayDashboardUseCase,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val selectedDate = MutableStateFlow(today)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = selectedDate.flatMapLatest { date ->
        combine(observeDashboard(date), mealRepository.mealsOn(date)) { rollup, meals ->
            if (rollup == null) {
                HistoryUiState.NeedsOnboarding
            } else {
                HistoryUiState.Content(
                    date = date,
                    dayRollup = rollup,
                    mealGroups = MealGrouping.groupByType(meals),
                    canGoForward = date.isBefore(today),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun selectDate(date: LocalDate) {
        selectedDate.value = minOf(date, today)
    }

    fun previousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        if (selectedDate.value.isBefore(today)) selectedDate.value = selectedDate.value.plusDays(1)
    }
}

package com.defat.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.usecase.ObserveTodayDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeTodayDashboard: ObserveTodayDashboardUseCase,
    mealRepository: MealRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = combine(
        observeTodayDashboard(today),
        mealRepository.mealsOn(today),
    ) { rollup, meals ->
        if (rollup == null) {
            HomeUiState.NeedsOnboarding
        } else {
            HomeUiState.Content(dayRollup = rollup, mealGroups = MealGrouping.groupByType(meals))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}

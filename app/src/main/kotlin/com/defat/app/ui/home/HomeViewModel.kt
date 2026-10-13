package com.defat.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.app.ui.SelectedDayHolder
import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.calc.DayBuckets
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.calc.WeekStrip
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.SettingsRepository
import com.defat.core.domain.repository.mealsOnLogicalDay
import com.defat.core.domain.usecase.ObserveTodayDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Home renders one logical day — today by default, or any earlier day the
 * student picks in the week strip or the 歷史 tab (plan §6.3). Ring, macro
 * tiles and meal sections all follow [HomeUiState.Content.selectedDate].
 *
 * "Today" is the logical date for the user's day-start time, so at 01:30 with
 * the default 04:00 start Home is still showing yesterday.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeDashboard: ObserveTodayDashboardUseCase,
    private val mealRepository: MealRepository,
    settingsRepository: SettingsRepository,
    private val selectedDay: SelectedDayHolder,
    private val clock: Clock,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.dayStart,
        selectedDay.selected,
    ) { dayStart, selected -> dayStart to selected }
        .flatMapLatest { (dayStart, selected) ->
            val zone = clock.zone
            val today = DayBoundary.logicalDate(clock.instant(), zone, dayStart)
            val date = minOf(selected ?: today, today)
            combine(
                observeDashboard(date),
                mealRepository.mealsOnLogicalDay(date, zone, dayStart),
                mealRepository.allMeals(),
            ) { rollup, meals, allMeals ->
                if (rollup == null) {
                    HomeUiState.NeedsOnboarding
                } else {
                    val loggedDates = DayBuckets.loggedDates(allMeals, zone, dayStart)
                    HomeUiState.Content(
                        dayRollup = rollup,
                        mealGroups = MealGrouping.groupByType(meals),
                        selectedDate = date,
                        today = today,
                        weekDays = WeekStrip.cells(date, today, loggedDates),
                        previousWeekTarget = WeekStrip.previousWeekTarget(date, loggedDates.minOrNull(), today),
                        nextWeekTarget = WeekStrip.nextWeekTarget(date, today),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Future days are ignored. Picking today goes back to "follow today". */
    fun selectDay(date: LocalDate) {
        val content = uiState.value as? HomeUiState.Content ?: return
        if (date.isAfter(content.today)) return
        selectedDay.select(if (date == content.today) null else date)
    }

    fun previousWeek() {
        val content = uiState.value as? HomeUiState.Content ?: return
        content.previousWeekTarget?.let { selectDay(it) }
    }

    fun nextWeek() {
        val content = uiState.value as? HomeUiState.Content ?: return
        content.nextWeekTarget?.let { selectDay(it) }
    }
}

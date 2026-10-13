package com.defat.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.app.ui.SelectedDayHolder
import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.calc.DayBuckets
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.calc.StreakCalculator
import com.defat.core.domain.calc.WaterCups
import com.defat.core.domain.calc.WaterGoalCalculator
import com.defat.core.domain.calc.WeekStrip
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.ExerciseEntry
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.repository.ExerciseRepository
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import com.defat.core.domain.repository.WaterRepository
import com.defat.core.domain.repository.exerciseOnLogicalDay
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
import kotlinx.coroutines.launch

/**
 * Home renders one logical day — today by default, or any earlier day the
 * student picks in the week strip or the 歷史 tab (plan §6.3). Ring, macro
 * tiles, meal sections, exercise and water all follow
 * [HomeUiState.Content.selectedDate]; only the streak counts from today.
 *
 * "Today" is the logical date for the user's day-start time, so at 01:30 with
 * the default 04:00 start Home is still showing yesterday.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeDashboard: ObserveTodayDashboardUseCase,
    private val mealRepository: MealRepository,
    private val exerciseRepository: ExerciseRepository,
    private val waterRepository: WaterRepository,
    private val profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
    private val selectedDay: SelectedDayHolder,
    private val clock: Clock,
) : ViewModel() {

    private data class DayData(val rollup: DayRollup?, val meals: List<Meal>, val exercise: List<ExerciseEntry>)

    private data class Extras(val allMeals: List<Meal>, val waterMl: Int, val profile: UserProfile?)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.dayStart,
        selectedDay.selected,
    ) { dayStart, selected -> dayStart to selected }
        .flatMapLatest { (dayStart, selected) ->
            val zone = clock.zone
            val today = DayBoundary.logicalDate(clock.instant(), zone, dayStart)
            val date = minOf(selected ?: today, today)
            val dayData = combine(
                observeDashboard(date),
                mealRepository.mealsOnLogicalDay(date, zone, dayStart),
                exerciseRepository.exerciseOnLogicalDay(date, zone, dayStart),
            ) { rollup, meals, exercise -> DayData(rollup, meals, exercise) }
            val extras = combine(
                mealRepository.allMeals(),
                waterRepository.waterMl(date),
                profileRepository.profile,
            ) { allMeals, waterMl, profile -> Extras(allMeals, waterMl, profile) }

            combine(dayData, extras) { day, more ->
                val rollup = day.rollup
                if (rollup == null) {
                    HomeUiState.NeedsOnboarding
                } else {
                    val loggedDates = DayBuckets.loggedDates(more.allMeals, zone, dayStart)
                    HomeUiState.Content(
                        dayRollup = rollup,
                        mealGroups = MealGrouping.groupByType(day.meals),
                        selectedDate = date,
                        today = today,
                        weekDays = WeekStrip.cells(date, today, loggedDates),
                        previousWeekTarget = WeekStrip.previousWeekTarget(date, loggedDates.minOrNull(), today),
                        nextWeekTarget = WeekStrip.nextWeekTarget(date, today),
                        exerciseEntries = day.exercise,
                        waterMl = more.waterMl,
                        waterGoalMl = more.profile?.weightKg?.takeIf { it > 0.0 }?.let(WaterGoalCalculator::goalMl) ?: 0,
                        streakDays = StreakCalculator.streak(loggedDates, today),
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

    fun deleteExercise(id: String) {
        viewModelScope.launch { exerciseRepository.delete(id) }
    }

    /** Taps cup [cupNumber] (1..8) on the selected day: fills up to it, or removes the last one. */
    fun tapWaterCup(cupNumber: Int) {
        val content = uiState.value as? HomeUiState.Content ?: return
        val ml = WaterCups.tap(content.waterMl, cupNumber)
        viewModelScope.launch { waterRepository.setWaterMl(content.selectedDate, ml) }
    }

    /**
     * The 自訂 field: adds a typed amount. Returns false, changing nothing, when
     * the text is not a positive whole number, so the field can keep what was typed.
     */
    fun addWater(text: String): Boolean {
        val content = uiState.value as? HomeUiState.Content ?: return false
        val amount = text.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
        val ml = WaterCups.add(content.waterMl, amount)
        viewModelScope.launch { waterRepository.setWaterMl(content.selectedDate, ml) }
        return true
    }
}

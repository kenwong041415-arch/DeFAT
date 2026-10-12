package com.defat.app.ui

import com.defat.app.fake.FakeMealRepository
import com.defat.app.fake.FakeProfileRepository
import com.defat.app.ui.history.HistoryUiState
import com.defat.app.ui.history.HistoryViewModel
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.usecase.ComputeDailyTargetUseCase
import com.defat.core.domain.usecase.ObserveTodayDashboardUseCase
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun goldenProfile() = UserProfile(
        sex = Sex.MALE,
        birthDate = LocalDate.now().minusYears(30),
        heightCm = 175.0,
        weightKg = 80.0,
        bodyFatPct = 25.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = Instant.now(),
    )

    private fun meal(date: LocalDate, kcal: Double, mealType: MealType) = Meal(
        id = "$date-$kcal-$mealType",
        loggedAt = date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
        date = date,
        name = "meal",
        kcal = kcal,
        proteinG = 10.0,
        carbsG = 10.0,
        fatG = 10.0,
        source = MealSource.MANUAL,
        mealType = mealType,
    )

    private fun newViewModel(mealRepository: FakeMealRepository): HistoryViewModel {
        val profileRepository = FakeProfileRepository(goldenProfile())
        val observeDashboard = ObserveTodayDashboardUseCase(
            profileRepository,
            mealRepository,
            ComputeDailyTargetUseCase(),
        )
        return HistoryViewModel(observeDashboard, mealRepository)
    }

    @Test
    fun `shows the selected day's meals grouped by type`() = runTest(testDispatcher) {
        val yesterday = LocalDate.now().minusDays(1)
        val mealRepository = FakeMealRepository(
            listOf(
                meal(yesterday, 300.0, MealType.BREAKFAST),
                meal(yesterday, 450.0, MealType.LUNCH),
            ),
        )
        val viewModel = newViewModel(mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.previousDay()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Content)
        state as HistoryUiState.Content
        assertEquals(yesterday, state.date)

        val breakfast = state.mealGroups.single { it.type == MealType.BREAKFAST }
        assertEquals(300.0, breakfast.kcal)
        val lunch = state.mealGroups.single { it.type == MealType.LUNCH }
        assertEquals(450.0, lunch.kcal)
    }

    @Test
    fun `cannot go past today`() = runTest(testDispatcher) {
        val mealRepository = FakeMealRepository()
        val viewModel = newViewModel(mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val initial = viewModel.uiState.value
        assertTrue(initial is HistoryUiState.Content)
        assertFalse((initial as HistoryUiState.Content).canGoForward)

        viewModel.nextDay()
        runCurrent()

        val after = viewModel.uiState.value
        assertTrue(after is HistoryUiState.Content)
        assertEquals(LocalDate.now(), (after as HistoryUiState.Content).date)
    }

    @Test
    fun `an empty past day still shows five groups`() = runTest(testDispatcher) {
        val mealRepository = FakeMealRepository()
        val viewModel = newViewModel(mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.previousDay()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Content)
        state as HistoryUiState.Content
        assertEquals(5, state.mealGroups.size)
        assertTrue(state.mealGroups.all { it.isEmpty })
        assertEquals(0.0, state.dayRollup.intakeKcal)
    }
}

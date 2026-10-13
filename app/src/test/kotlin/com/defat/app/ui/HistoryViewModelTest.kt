package com.defat.app.ui

import com.defat.app.fake.FakeMealRepository
import com.defat.app.fake.FakeProfileRepository
import com.defat.app.fake.FakeSettingsRepository
import com.defat.app.fake.TestClock
import com.defat.app.ui.history.HistoryUiState
import com.defat.app.ui.history.HistoryViewModel
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.usecase.ComputeDailyTargetUseCase
import com.defat.core.domain.usecase.ObserveDaySummariesUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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
        birthDate = TestClock.today.minusYears(30),
        heightCm = 175.0,
        weightKg = 80.0,
        bodyFatPct = 25.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun meal(date: LocalDate, hour: Int, minute: Int, kcal: Double) = Meal(
        id = "$date-$hour:$minute-$kcal",
        loggedAt = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(TestClock.zone).toInstant(),
        date = date,
        name = "meal",
        kcal = kcal,
        proteinG = 10.0,
        carbsG = 10.0,
        fatG = 10.0,
        source = MealSource.MANUAL,
    )

    private fun newViewModel(
        mealRepository: FakeMealRepository,
        profileRepository: FakeProfileRepository = FakeProfileRepository(goldenProfile()),
        selectedDay: SelectedDayHolder = SelectedDayHolder(),
    ): HistoryViewModel {
        val summaries = ObserveDaySummariesUseCase(
            profileRepository,
            mealRepository,
            FakeSettingsRepository(),
            ComputeDailyTargetUseCase(),
            TestClock.clock,
        )
        return HistoryViewModel(summaries, profileRepository, selectedDay)
    }

    @Test
    fun `lists only days with meals, newest first`() = runTest(testDispatcher) {
        val today = TestClock.today
        val mealRepository = FakeMealRepository(
            listOf(
                meal(today.minusDays(3), 12, 0, kcal = 500.0),
                meal(today, 12, 0, kcal = 700.0),
                meal(today.minusDays(3), 19, 0, kcal = 400.0),
            ),
        )
        val viewModel = newViewModel(mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Content)
        state as HistoryUiState.Content
        assertEquals(listOf(today, today.minusDays(3)), state.days.map { it.date })
        assertEquals(900.0, state.days[1].intakeKcal, 1e-9)
        assertEquals(2, state.days[1].mealCount)
    }

    @Test
    fun `a day is on target until it goes over`() = runTest(testDispatcher) {
        val today = TestClock.today
        val mealRepository = FakeMealRepository(
            listOf(
                meal(today.minusDays(1), 12, 0, kcal = 500.0),
                meal(today, 12, 0, kcal = 9000.0),
            ),
        )
        val viewModel = newViewModel(mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val days = (viewModel.uiState.value as HistoryUiState.Content).days
        assertTrue(days.single { it.date == today }.isOverTarget)
        assertFalse(days.single { it.date == today.minusDays(1) }.isOverTarget)
    }

    @Test
    fun `an empty log is an empty list, not an error`() = runTest(testDispatcher) {
        val viewModel = newViewModel(FakeMealRepository())

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Content)
        assertTrue((state as HistoryUiState.Content).days.isEmpty())
    }

    @Test
    fun `without a profile it needs onboarding`() = runTest(testDispatcher) {
        val viewModel = newViewModel(FakeMealRepository(), profileRepository = FakeProfileRepository(null))

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertEquals(HistoryUiState.NeedsOnboarding, viewModel.uiState.value)
    }

    @Test
    fun `opening a day selects it for Home`() = runTest(testDispatcher) {
        val selectedDay = SelectedDayHolder()
        val viewModel = newViewModel(FakeMealRepository(), selectedDay = selectedDay)
        val target = TestClock.today.minusDays(4)

        viewModel.openDay(target)

        assertEquals(target, selectedDay.selected.value)
    }
}

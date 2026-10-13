package com.defat.app.ui

import com.defat.app.fake.FakeExerciseRepository
import com.defat.app.fake.FakeMealRepository
import com.defat.app.fake.FakeProfileRepository
import com.defat.app.fake.FakeSettingsRepository
import com.defat.app.fake.FakeWaterRepository
import com.defat.app.fake.TestClock
import com.defat.app.ui.SelectedDayHolder
import com.defat.app.ui.home.HomeUiState
import com.defat.app.ui.home.HomeViewModel
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.ExerciseEntry
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.TdeeSource
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.usecase.ObserveTodayDashboardUseCase
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
        disclaimerAcceptedAt = Instant.now(),
    )

    private fun meal(
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        mealType: MealType = MealType.SNACK,
    ) = Meal(
        id = "$kcal-$protein-$mealType",
        loggedAt = TestClock.clock.instant(),
        date = TestClock.today,
        name = "meal",
        kcal = kcal,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        source = MealSource.MANUAL,
        mealType = mealType,
    )

    private fun newViewModel(
        profileRepository: FakeProfileRepository,
        mealRepository: FakeMealRepository,
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
        selectedDay: SelectedDayHolder = SelectedDayHolder(),
        exerciseRepository: FakeExerciseRepository = FakeExerciseRepository(),
        waterRepository: FakeWaterRepository = FakeWaterRepository(),
    ): HomeViewModel {
        val observeTodayDashboard = ObserveTodayDashboardUseCase(
            profileRepository,
            mealRepository,
            exerciseRepository,
            settingsRepository,
            com.defat.core.domain.usecase.ComputeDailyTargetUseCase(),
            TestClock.clock,
        )
        return HomeViewModel(
            observeTodayDashboard,
            mealRepository,
            exerciseRepository,
            waterRepository,
            profileRepository,
            settingsRepository,
            selectedDay,
            TestClock.clock,
        )
    }

    private fun exerciseAt(date: LocalDate, hour: Int, kcal: Double) = ExerciseEntry(
        id = "ex-$date-$hour",
        loggedAt = LocalDateTime.of(date, LocalTime.of(hour, 0)).atZone(TestClock.zone).toInstant(),
        activityKey = "weight_training",
        met = 5.0,
        minutes = 60,
        weightKgAtTime = 80.0,
        kcal = kcal,
    )

    /** A meal at [hour]:[minute] local time on calendar [date], in the test zone. */
    private fun mealAt(date: LocalDate, hour: Int, minute: Int, kcal: Double) = Meal(
        id = "$date-$hour:$minute-$kcal",
        loggedAt = LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(TestClock.zone).toInstant(),
        date = date,
        name = "meal",
        kcal = kcal,
        proteinG = 10.0,
        carbsG = 10.0,
        fatG = 10.0,
        source = MealSource.MANUAL,
    )

    @Test
    fun `emits intake, remaining and protein for two fake meals`() = runTest(testDispatcher) {
        val profileRepository = FakeProfileRepository(goldenProfile())
        val mealRepository = FakeMealRepository(
            listOf(
                meal(kcal = 600.0, protein = 30.0, carbs = 80.0, fat = 15.0),
                meal(kcal = 450.0, protein = 40.0, carbs = 30.0, fat = 12.0),
            ),
        )
        val viewModel = newViewModel(profileRepository, mealRepository)

        // uiState is stateIn(WhileSubscribed): the upstream only runs while
        // something collects, so subscribe before reading the value.
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content

        assertEquals(1050, state.dayRollup.intakeKcal.roundKcal())
        assertEquals(1016, state.dayRollup.remainingKcal.roundKcal())
        assertEquals(70, state.dayRollup.proteinG.roundGrams())
        assertEquals(132, state.dayRollup.target.macros.proteinG.roundGrams())
        assertEquals(5, state.mealGroups.size)
    }

    @Test
    fun `groups today's meals by type`() = runTest(testDispatcher) {
        val profileRepository = FakeProfileRepository(goldenProfile())
        val mealRepository = FakeMealRepository(
            listOf(
                meal(kcal = 300.0, protein = 20.0, carbs = 40.0, fat = 8.0, mealType = MealType.BREAKFAST),
                meal(kcal = 600.0, protein = 30.0, carbs = 80.0, fat = 15.0, mealType = MealType.LUNCH),
                meal(kcal = 450.0, protein = 40.0, carbs = 30.0, fat = 12.0, mealType = MealType.LUNCH),
            ),
        )
        val viewModel = newViewModel(profileRepository, mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content

        val lunch = state.mealGroups.single { it.type == MealType.LUNCH }
        assertEquals(1050, lunch.kcal.roundKcal())
        assertEquals(2, lunch.mealCount)

        val dinner = state.mealGroups.single { it.type == MealType.DINNER }
        assertTrue(dinner.isEmpty)
    }

    @Test
    fun `with no profile it needs onboarding`() = runTest(testDispatcher) {
        val profileRepository = FakeProfileRepository(initial = null)
        val mealRepository = FakeMealRepository()
        val viewModel = newViewModel(profileRepository, mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertEquals(HomeUiState.NeedsOnboarding, viewModel.uiState.value)
    }

    @Test
    fun `the week strip has seven cells, Monday first, and today's week`() = runTest(testDispatcher) {
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), FakeMealRepository())

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals(7, state.weekDays.size)
        assertEquals(DayOfWeek.MONDAY, state.weekDays.first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 12), state.weekDays.first().date)
        assertEquals(LocalDate.of(2026, 10, 13), state.selectedDate)
        assertTrue(state.weekDays.single { it.isToday }.isSelected)
    }

    @Test
    fun `days after today are disabled in the strip`() = runTest(testDispatcher) {
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), FakeMealRepository())

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value as HomeUiState.Content
        // Tue 13 is today; Wed 14 .. Sun 18 are future.
        assertEquals(
            listOf(false, false, true, true, true, true, true),
            state.weekDays.map { it.isFuture },
        )

        viewModel.selectDay(LocalDate.of(2026, 10, 15))
        runCurrent()
        assertEquals(LocalDate.of(2026, 10, 13), (viewModel.uiState.value as HomeUiState.Content).selectedDate)
    }

    @Test
    fun `selecting a day re-queries and Home follows it, not today`() = runTest(testDispatcher) {
        val yesterday = TestClock.today.minusDays(1)
        val mealRepository = FakeMealRepository(
            listOf(
                mealAt(TestClock.today, 12, 0, kcal = 700.0),
                mealAt(yesterday, 12, 0, kcal = 400.0),
            ),
        )
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(700, (viewModel.uiState.value as HomeUiState.Content).dayRollup.intakeKcal.roundKcal())

        viewModel.selectDay(yesterday)
        runCurrent()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals(yesterday, state.selectedDate)
        assertEquals(yesterday, state.dayRollup.date)
        assertEquals(400, state.dayRollup.intakeKcal.roundKcal())
        assertFalse(state.isToday)
        assertTrue(state.weekDays.single { it.date == yesterday }.isSelected)
    }

    @Test
    fun `a day with a logged meal gets a dot`() = runTest(testDispatcher) {
        val yesterday = TestClock.today.minusDays(1)
        val mealRepository = FakeMealRepository(listOf(mealAt(yesterday, 12, 0, kcal = 400.0)))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertTrue(state.weekDays.single { it.date == yesterday }.hasEntries)
        assertFalse(state.weekDays.single { it.isToday }.hasEntries)
    }

    @Test
    fun `a 01-30 snack counts toward the previous day on Home`() = runTest(testDispatcher) {
        // Calendar 2026-10-13 01:30 is logical 2026-10-12 with the default 04:00 start.
        val mealRepository = FakeMealRepository(listOf(mealAt(TestClock.today, 1, 30, kcal = 300.0)))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(0, (viewModel.uiState.value as HomeUiState.Content).dayRollup.intakeKcal.roundKcal())

        viewModel.selectDay(TestClock.today.minusDays(1))
        runCurrent()
        assertEquals(300, (viewModel.uiState.value as HomeUiState.Content).dayRollup.intakeKcal.roundKcal())
    }

    @Test
    fun `swiping weeks moves a week and stops at the earliest logged day`() = runTest(testDispatcher) {
        val earliest = LocalDate.of(2026, 10, 8) // Thursday of the previous week
        val mealRepository = FakeMealRepository(listOf(mealAt(earliest, 12, 0, kcal = 400.0)))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), mealRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.previousWeek()
        runCurrent()
        // Tue 13 minus a week is Tue 6, before the earliest logged day -> clamped to it.
        assertEquals(earliest, (viewModel.uiState.value as HomeUiState.Content).selectedDate)

        viewModel.previousWeek()
        runCurrent()
        assertEquals(earliest, (viewModel.uiState.value as HomeUiState.Content).selectedDate)

        viewModel.nextWeek()
        runCurrent()
        assertEquals(TestClock.today, (viewModel.uiState.value as HomeUiState.Content).selectedDate)
    }

    // ---- Phase 1.7 PR B: exercise, water, streak (plan §15.8) ----

    @Test
    fun `exercise lands on the selected day, not today`() = runTest(testDispatcher) {
        val yesterday = TestClock.today.minusDays(1)
        val exercise = FakeExerciseRepository(listOf(exerciseAt(yesterday, 18, kcal = 1500.0)))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), FakeMealRepository(), exerciseRepository = exercise)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val today = viewModel.uiState.value as HomeUiState.Content
        assertTrue(today.exerciseEntries.isEmpty())
        assertEquals(0, today.dayRollup.exerciseKcal.roundKcal())
        // No exercise today: the target is exactly the multiplier target (plan §10.1).
        assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, today.dayRollup.target.tdeeSource)
        assertEquals(2582.3, today.dayRollup.target.tdeeKcal, 1e-6)

        viewModel.selectDay(yesterday)
        runCurrent()

        val past = viewModel.uiState.value as HomeUiState.Content
        assertEquals(1, past.exerciseEntries.size)
        assertEquals(1500, past.dayRollup.exerciseKcal.roundKcal())
        // 1666 x 1.1 + 1500 beats 1666 x 1.55.
        assertEquals(TdeeSource.MANUAL_EXERCISE, past.dayRollup.target.tdeeSource)
        assertEquals(1666.0 * 1.1 + 1500.0, past.dayRollup.target.tdeeKcal, 1e-6)
    }

    @Test
    fun `water lands on the selected day, with a weight-based goal`() = runTest(testDispatcher) {
        val yesterday = TestClock.today.minusDays(1)
        val water = FakeWaterRepository(mapOf(TestClock.today to 500, yesterday to 1750))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), FakeMealRepository(), waterRepository = water)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        val today = viewModel.uiState.value as HomeUiState.Content
        assertEquals(500, today.waterMl)
        assertEquals(2800, today.waterGoalMl) // 80 kg x 35 ml

        viewModel.selectDay(yesterday)
        runCurrent()
        assertEquals(1750, (viewModel.uiState.value as HomeUiState.Content).waterMl)
    }

    @Test
    fun `tapping a cup writes to the selected day`() = runTest(testDispatcher) {
        val yesterday = TestClock.today.minusDays(1)
        val water = FakeWaterRepository(mapOf(yesterday to 500))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), FakeMealRepository(), waterRepository = water)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        viewModel.selectDay(yesterday)
        runCurrent()

        viewModel.tapWaterCup(4)
        runCurrent()
        assertEquals(1000, (viewModel.uiState.value as HomeUiState.Content).waterMl)

        // Tapping the last filled cup removes one.
        viewModel.tapWaterCup(4)
        runCurrent()
        assertEquals(750, (viewModel.uiState.value as HomeUiState.Content).waterMl)

        // Today is untouched.
        viewModel.selectDay(TestClock.today)
        runCurrent()
        assertEquals(0, (viewModel.uiState.value as HomeUiState.Content).waterMl)
    }

    @Test
    fun `a typed amount is added, and invalid text is refused`() = runTest(testDispatcher) {
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), FakeMealRepository())

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertTrue(viewModel.addWater("330"))
        runCurrent()
        assertEquals(330, (viewModel.uiState.value as HomeUiState.Content).waterMl)

        assertFalse(viewModel.addWater(""))
        assertFalse(viewModel.addWater("abc"))
        assertFalse(viewModel.addWater("0"))
        assertFalse(viewModel.addWater("-5"))
        runCurrent()
        assertEquals(330, (viewModel.uiState.value as HomeUiState.Content).waterMl)
    }

    @Test
    fun `the streak counts consecutive logged days back from today`() = runTest(testDispatcher) {
        val meals = FakeMealRepository(
            listOf(
                mealAt(TestClock.today, 12, 0, kcal = 500.0),
                mealAt(TestClock.today.minusDays(1), 12, 0, kcal = 500.0),
                mealAt(TestClock.today.minusDays(2), 12, 0, kcal = 500.0),
                // a gap on day -3
                mealAt(TestClock.today.minusDays(4), 12, 0, kcal = 500.0),
            ),
        )
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), meals)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(3, (viewModel.uiState.value as HomeUiState.Content).streakDays)

        // The streak is about today, whichever day is being looked at.
        viewModel.selectDay(TestClock.today.minusDays(4))
        runCurrent()
        assertEquals(3, (viewModel.uiState.value as HomeUiState.Content).streakDays)
    }

    @Test
    fun `yesterday still counts before the first meal of today`() = runTest(testDispatcher) {
        val meals = FakeMealRepository(listOf(mealAt(TestClock.today.minusDays(1), 12, 0, kcal = 500.0)))
        val viewModel = newViewModel(FakeProfileRepository(goldenProfile()), meals)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertEquals(1, (viewModel.uiState.value as HomeUiState.Content).streakDays)
    }

    @Test
    fun `a deleted exercise entry disappears`() = runTest(testDispatcher) {
        val entry = exerciseAt(TestClock.today, 9, kcal = 300.0)
        val viewModel = newViewModel(
            FakeProfileRepository(goldenProfile()),
            FakeMealRepository(),
            exerciseRepository = FakeExerciseRepository(listOf(entry)),
        )

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1, (viewModel.uiState.value as HomeUiState.Content).exerciseEntries.size)

        viewModel.deleteExercise(entry.id)
        runCurrent()
        assertTrue((viewModel.uiState.value as HomeUiState.Content).exerciseEntries.isEmpty())
    }
}

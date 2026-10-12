package com.defat.app.ui

import com.defat.app.fake.FakeMealRepository
import com.defat.app.fake.FakeProfileRepository
import com.defat.app.ui.home.HomeUiState
import com.defat.app.ui.home.HomeViewModel
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.usecase.ObserveTodayDashboardUseCase
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
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
        birthDate = LocalDate.now().minusYears(30),
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
        loggedAt = Instant.now(),
        date = LocalDate.now(),
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
    ): HomeViewModel {
        val observeTodayDashboard = ObserveTodayDashboardUseCase(
            profileRepository,
            mealRepository,
            com.defat.core.domain.usecase.ComputeDailyTargetUseCase(),
        )
        return HomeViewModel(observeTodayDashboard, mealRepository)
    }

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
}

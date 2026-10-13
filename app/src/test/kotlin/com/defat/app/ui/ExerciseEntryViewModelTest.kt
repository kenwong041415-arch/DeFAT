package com.defat.app.ui

import com.defat.app.fake.FakeExerciseRepository
import com.defat.app.fake.FakeProfileRepository
import com.defat.app.fake.FakeSettingsRepository
import com.defat.app.fake.TestClock
import com.defat.app.ui.exercise.ExerciseEntryViewModel
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import java.time.Instant
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEntryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun profile(weightKg: Double = 80.0) = UserProfile(
        sex = Sex.MALE,
        birthDate = TestClock.today.minusYears(30),
        heightCm = 175.0,
        weightKg = weightKg,
        bodyFatPct = 25.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = Instant.now(),
    )

    private fun newViewModel(
        exercise: FakeExerciseRepository = FakeExerciseRepository(),
        selectedDay: SelectedDayHolder = SelectedDayHolder(),
        weightKg: Double = 80.0,
    ) = ExerciseEntryViewModel(
        exercise,
        FakeProfileRepository(profile(weightKg)),
        FakeSettingsRepository(),
        selectedDay,
        TestClock.clock,
    )

    private fun TestScope.collect(viewModel: ExerciseEntryViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
    }

    @Test
    fun `the preview is MET times weight times hours, for light medium and heavy`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        collect(viewModel)

        viewModel.selectActivity("weight_training")
        viewModel.setMinutes("45")
        runCurrent()

        val medium = assertNotNull(viewModel.uiState.value.preview)
        assertEquals(5.0, medium.met, 1e-9)
        assertEquals(80.0, medium.weightKg, 1e-9)
        assertEquals(0.75, medium.hours, 1e-9)
        assertEquals(300.0, medium.kcal, 1e-9) // 5 x 80 x 0.75

        viewModel.selectWeightTrainingLevel("weight_training_heavy")
        runCurrent()
        assertEquals(360.0, assertNotNull(viewModel.uiState.value.preview).kcal, 1e-9) // 6 x 80 x 0.75

        viewModel.selectWeightTrainingLevel("weight_training_light")
        runCurrent()
        assertEquals(210.0, assertNotNull(viewModel.uiState.value.preview).kcal, 1e-9) // 3.5 x 80 x 0.75
    }

    @Test
    fun `minutes that are blank, zero or too large cannot be saved`() = runTest(testDispatcher) {
        val repository = FakeExerciseRepository()
        val viewModel = newViewModel(repository)
        collect(viewModel)

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setMinutes("0")
        runCurrent()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setMinutes("1441")
        runCurrent()
        assertFalse(viewModel.uiState.value.canSave)

        // Anything that is not a digit is dropped as it is typed.
        viewModel.setMinutes("4a5")
        runCurrent()
        assertEquals("45", viewModel.uiState.value.minutesText)
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.setMinutes("")
        runCurrent()
        viewModel.save()
        runCurrent()
        assertNull(repository.lastUpserted)
    }

    @Test
    fun `saving stores the computed kcal, the weight used and the activity key`() = runTest(testDispatcher) {
        val repository = FakeExerciseRepository()
        val viewModel = newViewModel(repository)
        collect(viewModel)

        viewModel.selectActivity("walking")
        viewModel.setMinutes("60")
        runCurrent()
        viewModel.save()
        runCurrent()

        val saved = assertNotNull(repository.lastUpserted)
        assertEquals("walking", saved.activityKey)
        assertEquals(3.5, saved.met, 1e-9)
        assertEquals(60, saved.minutes)
        assertEquals(80.0, saved.weightKgAtTime, 1e-9)
        assertEquals(280.0, saved.kcal, 1e-9) // 3.5 x 80 x 1
        assertEquals(TestClock.clock.instant(), saved.loggedAt)
    }

    @Test
    fun `weight training is stored under its intensity key`() = runTest(testDispatcher) {
        val repository = FakeExerciseRepository()
        val viewModel = newViewModel(repository)
        collect(viewModel)

        viewModel.selectActivity("weight_training")
        viewModel.selectWeightTrainingLevel("weight_training_light")
        viewModel.setMinutes("30")
        runCurrent()
        viewModel.save()
        runCurrent()

        val saved = assertNotNull(repository.lastUpserted)
        assertEquals("weight_training_light", saved.activityKey)
        assertEquals(3.5, saved.met, 1e-9)
        assertEquals(140.0, saved.kcal, 1e-9) // 3.5 x 80 x 0.5
    }

    @Test
    fun `an entry logged while viewing an earlier day lands on that day`() = runTest(testDispatcher) {
        val repository = FakeExerciseRepository()
        val selectedDay = SelectedDayHolder()
        val yesterday = TestClock.today.minusDays(1)
        selectedDay.select(yesterday)
        val viewModel = newViewModel(repository, selectedDay)
        collect(viewModel)

        viewModel.selectActivity("yoga")
        viewModel.setMinutes("30")
        runCurrent()
        viewModel.save()
        runCurrent()

        val local = assertNotNull(repository.lastUpserted).loggedAt.atZone(TestClock.zone)
        assertEquals(yesterday, local.toLocalDate())
        assertEquals(LocalTime.NOON, local.toLocalTime())
    }

    @Test
    fun `saving tells the sheet to close`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        collect(viewModel)
        var closed = 0
        backgroundScope.launch { viewModel.saved.collect { closed++ } }

        viewModel.setMinutes("20")
        runCurrent()
        viewModel.save()
        runCurrent()

        assertEquals(1, closed)
    }
}

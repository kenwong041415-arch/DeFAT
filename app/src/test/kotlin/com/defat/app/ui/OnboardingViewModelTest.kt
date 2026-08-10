package com.defat.app.ui

import com.defat.app.fake.FakeProfileRepository
import com.defat.app.ui.onboarding.OnboardingViewModel
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Sex
import com.defat.core.domain.usecase.ComputeDailyTargetUseCase
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
        viewModel = OnboardingViewModel(profileRepository, ComputeDailyTargetUseCase())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `next is disabled until the basics step is valid`() {
        assertFalse(viewModel.uiState.value.isBasicsValid)
        viewModel.setSex(Sex.MALE)
        viewModel.setBirthDate(LocalDate.now().minusYears(30))
        viewModel.setHeightCmText("175")
        assertTrue(viewModel.uiState.value.isBasicsValid)
    }

    @Test
    fun `I don't know my body fat clears the value`() {
        viewModel.setBodyFatPctText("25")
        assertEquals("25", viewModel.uiState.value.bodyFatPctText)

        viewModel.setBodyFatUnknown(true)
        assertEquals("", viewModel.uiState.value.bodyFatPctText)
        assertTrue(viewModel.uiState.value.bodyFatUnknown)
    }

    @Test
    fun `completing the wizard calls save exactly once with the expected profile`() = runTest {
        fillGoldenProfile()
        viewModel.completeOnboarding()

        assertEquals(1, profileRepository.saveCallCount)
        val saved = profileRepository.lastSaved!!
        assertEquals(Sex.MALE, saved.sex)
        assertEquals(175.0, saved.heightCm, 1e-6)
        assertEquals(80.0, saved.weightKg, 1e-6)
        assertEquals(25.0, saved.bodyFatPct!!, 1e-6)
        assertEquals(ActivityLevel.MODERATE, saved.activityLevel)
        assertEquals(75.0, saved.goal.targetWeightKg, 1e-6)
    }

    @Test
    fun `the summary state exposes the golden-profile numbers`() {
        fillGoldenProfile()
        val target = viewModel.uiState.value.dailyTarget!!
        assertEquals(1666.0, target.bmrKcal, 1e-6)
        assertEquals(2582.3, target.tdeeKcal, 1e-6)
        assertEquals(2065.84, target.targetKcal, 1e-6)
        assertEquals(132.0, target.macros.proteinG, 1e-6)
        assertEquals(48.0, target.macros.fatG, 1e-6)
        assertEquals(276.46, target.macros.carbsG, 1e-6)
    }

    private fun fillGoldenProfile() {
        viewModel.acceptDisclaimer()
        viewModel.setSex(Sex.MALE)
        viewModel.setBirthDate(LocalDate.now().minusYears(30))
        viewModel.setHeightCmText("175")
        viewModel.setWeightKgText("80")
        viewModel.setBodyFatPctText("25")
        viewModel.setActivityLevel(ActivityLevel.MODERATE)
        viewModel.setGoalTargetWeightText("75")
    }
}

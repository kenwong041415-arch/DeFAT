package com.defat.app.ui

import androidx.lifecycle.SavedStateHandle
import com.defat.app.fake.FakeMealRepository
import com.defat.app.ui.meal.MealEditorViewModel
import com.defat.core.domain.model.FrequentFood
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import com.defat.core.domain.usecase.SaveMealUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
class MealEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        mealRepository: FakeMealRepository,
        mealId: String? = null,
    ): MealEditorViewModel {
        val handle = if (mealId != null) SavedStateHandle(mapOf("mealId" to mealId)) else SavedStateHandle()
        return MealEditorViewModel(handle, mealRepository, SaveMealUseCase(mealRepository))
    }

    @Test
    fun `calculate kcal from macros fills 4p plus 4c plus 9f`() {
        val vm = newViewModel(FakeMealRepository())
        vm.setProtein("30")
        vm.setCarbs("80")
        vm.setFat("15")
        vm.calculateKcalFromMacros()

        val expectedKcal = 4.0 * 30.0 + 4.0 * 80.0 + 9.0 * 15.0
        assertEquals(expectedKcal.toString(), vm.uiState.value.kcalText)
    }

    @Test
    fun `mismatch warning triggers past 15 percent and clears when back in range`() {
        val vm = newViewModel(FakeMealRepository())
        vm.setProtein("30")
        vm.setCarbs("80")
        vm.setFat("15")
        // macro sum = 4*30 + 4*80 + 9*15 = 575
        vm.setKcal("1000")
        assertTrue(vm.uiState.value.showMacroMismatchWarning)

        vm.setKcal("575")
        assertFalse(vm.uiState.value.showMacroMismatchWarning)
    }

    @Test
    fun `blank name blocks save`() = runTest {
        val mealRepository = FakeMealRepository()
        val vm = newViewModel(mealRepository)
        vm.setKcal("500")
        vm.setName("")

        assertFalse(vm.uiState.value.isSaveEnabled)
        vm.save()
        assertEquals(0, mealRepository.upsertCallCount)
    }

    @Test
    fun `edit mode loads the existing meal and saves with the same id`() = runTest {
        val existing = Meal(
            id = "meal-42",
            loggedAt = Instant.parse("2026-03-15T08:00:00Z"),
            date = LocalDate.of(2026, 3, 15),
            name = "Oats",
            kcal = 300.0,
            proteinG = 10.0,
            carbsG = 50.0,
            fatG = 5.0,
            source = MealSource.MANUAL,
            mealType = MealType.BREAKFAST,
        )
        val mealRepository = FakeMealRepository(listOf(existing))
        val vm = newViewModel(mealRepository, mealId = "meal-42")

        assertEquals("Oats", vm.uiState.value.nameText)
        assertEquals("300.0", vm.uiState.value.kcalText)

        vm.setKcal("350")
        vm.save()

        assertEquals(1, mealRepository.upsertCallCount)
        assertEquals("meal-42", mealRepository.lastUpserted?.id)
        assertEquals(350.0, mealRepository.lastUpserted?.kcal)
    }

    @Test
    fun `zero or blank calories block save`() {
        val vm = newViewModel(FakeMealRepository())
        vm.setName("雞胸飯")

        vm.setKcal("0")
        assertFalse(vm.uiState.value.isSaveEnabled)
        assertTrue(vm.uiState.value.showKcalError)

        vm.setKcal("")
        assertFalse(vm.uiState.value.isSaveEnabled)

        vm.setKcal("520")
        assertTrue(vm.uiState.value.isSaveEnabled)
        assertFalse(vm.uiState.value.showKcalError)
    }

    @Test
    fun `the kcal error only shows after the field is touched`() {
        val vm = newViewModel(FakeMealRepository())
        assertFalse(vm.uiState.value.isKcalValid)
        assertFalse(vm.uiState.value.showKcalError)
    }

    @Test
    fun `saves the meal type the user picked`() = runTest {
        val mealRepository = FakeMealRepository()
        val vm = newViewModel(mealRepository)
        vm.setName("雞胸飯")
        vm.setKcal("520")
        vm.setMealType(MealType.DINNER)
        vm.save()

        assertEquals(MealType.DINNER, mealRepository.lastUpserted?.mealType)
    }

    @Test
    fun `the meal type follows the time until the user picks one`() {
        val vm = newViewModel(FakeMealRepository())

        vm.setTime(LocalTime.of(19, 0))
        assertEquals(MealType.DINNER, vm.uiState.value.mealType)

        vm.setMealType(MealType.SNACK)
        vm.setTime(LocalTime.of(8, 0))
        assertEquals(MealType.SNACK, vm.uiState.value.mealType)
    }

    @Test
    fun `a frequent food fills the name and the numbers but not the meal type`() {
        val vm = newViewModel(FakeMealRepository())
        vm.setMealType(MealType.BREAKFAST)
        vm.applyFrequentFood(FrequentFood("雞胸飯", 3, 520.0, 48.0, 52.0, 9.0))

        val state = vm.uiState.value
        assertEquals("雞胸飯", state.nameText)
        assertEquals("520", state.kcalText)
        assertEquals("48", state.proteinText)
        assertEquals(MealType.BREAKFAST, state.mealType)
    }
}

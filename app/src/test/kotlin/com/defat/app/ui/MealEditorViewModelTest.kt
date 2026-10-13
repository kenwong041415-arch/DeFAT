package com.defat.app.ui

import androidx.lifecycle.SavedStateHandle
import com.defat.app.fake.FakeMealRepository
import com.defat.app.fake.FakeSettingsRepository
import com.defat.app.fake.chaSiuModifiers
import com.defat.app.fake.chaSiuRice
import com.defat.app.fake.TestClock
import com.defat.app.ui.SelectedDayHolder
import com.defat.app.ui.food.FoodSelection
import com.defat.app.ui.meal.MealEditorViewModel
import com.defat.core.domain.calc.ModifiedFoodCalculator
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
        selectedDay: SelectedDayHolder = SelectedDayHolder(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ): MealEditorViewModel {
        val handle = if (mealId != null) SavedStateHandle(mapOf("mealId" to mealId)) else SavedStateHandle()
        return MealEditorViewModel(
            handle,
            mealRepository,
            SaveMealUseCase(mealRepository),
            settingsRepository,
            selectedDay,
            TestClock.clock,
        )
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

    @Test
    fun `a new meal opens on today's logical date at the current time`() {
        val vm = newViewModel(FakeMealRepository())

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(TestClock.today, vm.uiState.value.date)
        assertEquals(LocalTime.of(14, 0), vm.uiState.value.time)
    }

    @Test
    fun `a new meal opens on the day selected on Home`() {
        val selectedDay = SelectedDayHolder()
        val earlier = TestClock.today.minusDays(3)
        selectedDay.select(earlier)

        val vm = newViewModel(FakeMealRepository(), selectedDay = selectedDay)

        assertEquals(earlier, vm.uiState.value.date)
    }

    @Test
    fun `a small-hours time is stored on the next calendar morning but stays on the logical day`() = runTest {
        val mealRepository = FakeMealRepository()
        val vm = newViewModel(mealRepository)
        vm.setName("宵夜")
        vm.setKcal("300")
        vm.setDate(LocalDate.of(2026, 10, 12))
        vm.setTime(LocalTime.of(1, 30))
        vm.save()

        val saved = mealRepository.lastUpserted!!
        assertEquals(LocalDate.of(2026, 10, 12), saved.date)
        val local = saved.loggedAt.atZone(TestClock.zone)
        assertEquals(LocalDate.of(2026, 10, 13), local.toLocalDate())
        assertEquals(LocalTime.of(1, 30), local.toLocalTime())
    }

    @Test
    fun `editing a late meal shows the logical date it counts toward`() = runTest {
        val late = Meal(
            id = "late-1",
            // Calendar 2026-10-13 01:30 in Hong Kong -> logical day 2026-10-12.
            loggedAt = Instant.parse("2026-10-12T17:30:00Z"),
            date = LocalDate.of(2026, 10, 13), // pre-1.7 row: calendar date, not logical
            name = "Noodles",
            kcal = 400.0,
            proteinG = 10.0,
            carbsG = 50.0,
            fatG = 10.0,
            source = MealSource.MANUAL,
            mealType = MealType.SNACK,
        )
        val mealRepository = FakeMealRepository(listOf(late))
        val vm = newViewModel(mealRepository, mealId = "late-1")

        assertEquals(LocalDate.of(2026, 10, 12), vm.uiState.value.date)
        assertEquals(LocalTime.of(1, 30), vm.uiState.value.time)

        // Saving without touching the date or time must not move the meal.
        vm.save()
        assertEquals(late.loggedAt, mealRepository.lastUpserted?.loggedAt)
    }

    // ---- Phase 1.7 PR B: 搵食物 / 點食法 (plan §8.6, §8.7) ----

    private fun leanNoSauceSelection(): FoodSelection {
        val modifiers = chaSiuModifiers().filter { it.id == "meat_lean" || it.id == "sauce_none" }
        return FoodSelection(
            item = chaSiuRice(),
            modifiers = modifiers,
            quantity = 1,
            totals = ModifiedFoodCalculator.apply(chaSiuRice(), modifiers),
        )
    }

    @Test
    fun `a library choice fills the form, leaves unpublished macros blank, and remembers its ids`() {
        val vm = newViewModel(FakeMealRepository())
        vm.setMealType(MealType.DINNER)

        vm.applyFoodSelection(leanNoSauceSelection(), english = false)

        val state = vm.uiState.value
        assertEquals("叉燒飯（瘦、走汁）", state.nameText)
        assertEquals("719", state.kcalText) // 939 - 120 - 100
        assertEquals("", state.proteinText) // not "0": no source published it
        assertEquals("", state.carbsText)
        assertEquals("", state.fatText)
        assertEquals("cha_siu_rice", state.baseFoodId)
        assertEquals(setOf("meat_lean", "sauce_none"), state.modifierIds.toSet())
        assertEquals(MealType.DINNER, state.mealType)
        assertTrue(state.macrosIncomplete)
        // Calories against missing macros is not a mismatch.
        assertFalse(state.showMacroMismatchWarning)
    }

    @Test
    fun `typing a macro after a library choice turns the mismatch check back on`() {
        val vm = newViewModel(FakeMealRepository())
        vm.applyFoodSelection(leanNoSauceSelection(), english = true)
        assertEquals("Barbecued pork rice (Lean meat, No sauce)", vm.uiState.value.nameText)

        vm.setProtein("30")
        vm.setCarbs("80")
        vm.setFat("15") // macro sum 575 against 719 kcal

        assertFalse(vm.uiState.value.macrosIncomplete)
        assertTrue(vm.uiState.value.showMacroMismatchWarning)
    }

    @Test
    fun `saving a library meal stores its own numbers and the ids`() = runTest {
        val mealRepository = FakeMealRepository()
        val vm = newViewModel(mealRepository)
        vm.applyFoodSelection(leanNoSauceSelection(), english = false)
        vm.save()

        val saved = mealRepository.lastUpserted!!
        assertEquals(719.0, saved.kcal, 0.0)
        assertEquals("cha_siu_rice", saved.baseFoodId)
        assertEquals(setOf("meat_lean", "sauce_none"), saved.modifierIds.toSet())
    }

    @Test
    fun `re-opening a library meal keeps its ids, and a later change to the library cannot touch its numbers`() = runTest {
        val existing = Meal(
            id = "meal-lib",
            loggedAt = Instant.parse("2026-03-15T08:00:00Z"),
            date = LocalDate.of(2026, 3, 15),
            name = "叉燒飯（瘦）",
            kcal = 819.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0,
            source = MealSource.MANUAL,
            mealType = MealType.LUNCH,
            baseFoodId = "cha_siu_rice",
            modifierIds = listOf("meat_lean"),
        )
        val mealRepository = FakeMealRepository(listOf(existing))
        val vm = newViewModel(mealRepository, mealId = "meal-lib")

        assertEquals("cha_siu_rice", vm.uiState.value.baseFoodId)
        assertEquals(listOf("meat_lean"), vm.uiState.value.modifierIds)

        vm.setName("叉燒飯（瘦）改")
        vm.save()

        val saved = mealRepository.lastUpserted!!
        assertEquals(819.0, saved.kcal, 0.0) // the meal's own number, as stored
        assertEquals("cha_siu_rice", saved.baseFoodId)
        assertEquals(listOf("meat_lean"), saved.modifierIds)
    }

    @Test
    fun `a typed meal has no base food`() = runTest {
        val mealRepository = FakeMealRepository()
        val vm = newViewModel(mealRepository)
        vm.setName("Oats")
        vm.setKcal("300")
        vm.save()

        assertEquals(null, mealRepository.lastUpserted!!.baseFoodId)
        assertEquals(emptyList(), mealRepository.lastUpserted!!.modifierIds)
    }

    @Test
    fun `a frequent food replaces a library choice and drops its ids`() {
        val vm = newViewModel(FakeMealRepository())
        vm.applyFoodSelection(leanNoSauceSelection(), english = false)
        vm.applyFrequentFood(FrequentFood("雞胸飯", 3, 520.0, 48.0, 52.0, 9.0))

        assertEquals(null, vm.uiState.value.baseFoodId)
        assertEquals(emptyList(), vm.uiState.value.modifierIds)
        assertFalse(vm.uiState.value.macrosIncomplete)
    }
}

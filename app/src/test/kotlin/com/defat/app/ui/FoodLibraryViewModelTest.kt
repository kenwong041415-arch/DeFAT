package com.defat.app.ui

import com.defat.app.fake.FakeFoodRepository
import com.defat.app.fake.chaSiuModifiers
import com.defat.app.fake.chaSiuRice
import com.defat.app.fake.foodItem
import com.defat.app.ui.food.FoodLibraryUiState
import com.defat.app.ui.food.FoodLibraryViewModel
import com.defat.app.ui.food.FoodSelection
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodSource
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
class FoodLibraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val milkTea = foodItem("milk_tea", "港式奶茶", "Hong Kong-style milk tea", 253.0, FoodCategory.DRINK)
    private val bigMac = foodItem(
        "big_mac", "巨無霸", "Big Mac", 496.0, FoodCategory.FAST_FOOD,
        proteinG = 25.6, source = FoodSource.OFFICIAL,
    )
    private val beefHorFun = foodItem(
        "dry_fried_beef_hor_fun", "乾炒牛河", "Dry-fried beef hor fun", 910.0, FoodCategory.NOODLE,
        kcalLow = 850.0, kcalHigh = 970.0,
    )
    private val harGow = foodItem("har_gow", "蝦餃", "Shrimp dumpling", 40.0, FoodCategory.DIM_SUM, portionLabel = "1 件")

    private fun newViewModel() = FoodLibraryViewModel(
        FakeFoodRepository(
            items = listOf(chaSiuRice(), milkTea, bigMac, beefHorFun, harGow),
            modifiers = chaSiuModifiers(),
        ),
    )

    private fun TestScope.collect(viewModel: FoodLibraryViewModel): MutableList<FoodSelection> {
        val received = mutableListOf<FoodSelection>()
        backgroundScope.launch { viewModel.uiState.collect {} }
        backgroundScope.launch { viewModel.selections.collect { received.add(it) } }
        runCurrent()
        return received
    }

    private fun FoodLibraryViewModel.state(): FoodLibraryUiState = uiState.value

    @Test
    fun `search matches Chinese by substring and English ignoring case`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        collect(viewModel)
        assertEquals(5, viewModel.state().results.size)

        viewModel.setQuery("奶茶")
        runCurrent()
        assertEquals(listOf("milk_tea"), viewModel.state().results.map { it.id })

        viewModel.setQuery("BIG mac")
        runCurrent()
        assertEquals(listOf("big_mac"), viewModel.state().results.map { it.id })

        viewModel.setQuery("no such food")
        runCurrent()
        assertTrue(viewModel.state().results.isEmpty())
    }

    @Test
    fun `a category chip narrows the list and tapping it again clears it`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        collect(viewModel)

        viewModel.setCategory(FoodCategory.DRINK)
        runCurrent()
        assertEquals(listOf("milk_tea"), viewModel.state().results.map { it.id })

        viewModel.setCategory(FoodCategory.DRINK)
        runCurrent()
        assertNull(viewModel.state().category)
        assertEquals(5, viewModel.state().results.size)
    }

    @Test
    fun `a row with modifier groups opens the adjust step and the arithmetic is the calculator's`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        collect(viewModel)

        viewModel.onItemTapped(chaSiuRice())
        runCurrent()

        val adjust = assertNotNull(viewModel.state().adjust)
        // docs/07 §6.3 order: 飯量, 肉款, 醬汁 (走糖 is a drink modifier and is not offered).
        assertEquals(listOf("rice_amount", "meat_cut", "sauce"), adjust.groups.map { it.groupId })
        assertEquals(939.0, adjust.breakdown.totals.kcal, 1e-9)
        assertFalse(adjust.showQuantity)

        viewModel.selectModifier("meat_cut", "meat_lean")
        viewModel.selectModifier("rice_amount", "rice_less")
        viewModel.selectModifier("sauce", "sauce_none")
        runCurrent()

        val adjusted = assertNotNull(viewModel.state().adjust)
        assertEquals(589.0, adjusted.breakdown.totals.kcal, 1e-9)
        // The lines come out in group order, which is the order the chips are in.
        assertEquals(listOf("rice_less", "meat_lean", "sauce_none"), adjusted.breakdown.modifiers.map { it.id })
        assertEquals(listOf(-130.0, -120.0, -100.0), adjusted.breakdown.modifiers.map { it.kcalDelta })

        // Back to the default option of one group.
        viewModel.selectModifier("sauce", null)
        runCurrent()
        assertEquals(689.0, assertNotNull(viewModel.state().adjust).breakdown.totals.kcal, 1e-9)
    }

    @Test
    fun `confirming hands over the computed numbers and the modifier ids`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        val received = collect(viewModel)

        viewModel.onItemTapped(chaSiuRice())
        viewModel.selectModifier("meat_cut", "meat_lean")
        viewModel.selectModifier("sauce", "sauce_none")
        runCurrent()
        viewModel.confirmAdjust()
        runCurrent()

        val selection = received.single()
        assertEquals("cha_siu_rice", selection.item.id)
        assertEquals(719.0, selection.totals.kcal, 1e-9) // 939 - 120 - 100
        assertNull(selection.totals.proteinG) // docs/07 publishes no protein for this row
        assertEquals(setOf("meat_lean", "sauce_none"), selection.modifiers.map { it.id }.toSet())
        assertNull(viewModel.state().adjust, "the step closes once the choice is handed over")
    }

    @Test
    fun `a row with nothing to adjust is chosen directly, and a range pre-fills its rounded midpoint`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        val received = collect(viewModel)

        viewModel.onItemTapped(bigMac)
        viewModel.onItemTapped(beefHorFun)
        runCurrent()

        assertNull(viewModel.state().adjust)
        assertEquals(2, received.size)
        assertEquals(496.0, received[0].totals.kcal, 1e-9)
        assertEquals(25.6, received[0].totals.proteinG!!, 1e-9)
        assertNull(received[0].totals.carbsG)
        assertEquals(910.0, received[1].totals.kcal, 1e-9) // midpoint of 850-970
    }

    @Test
    fun `dim sum is counted in pieces and the quantity multiplies after the modifiers`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        val received = collect(viewModel)

        viewModel.onItemTapped(harGow)
        runCurrent()
        val adjust = assertNotNull(viewModel.state().adjust)
        assertTrue(adjust.showQuantity)
        assertTrue(adjust.groups.isEmpty())

        viewModel.setQuantity(3)
        runCurrent()
        assertEquals(120.0, assertNotNull(viewModel.state().adjust).breakdown.totals.kcal, 1e-9)

        viewModel.setQuantity(0)
        runCurrent()
        assertEquals(1, assertNotNull(viewModel.state().adjust).quantity)
        viewModel.setQuantity(1000)
        runCurrent()
        assertEquals(FoodLibraryViewModel.MAX_QUANTITY, assertNotNull(viewModel.state().adjust).quantity)

        viewModel.setQuantity(3)
        runCurrent()
        viewModel.confirmAdjust()
        runCurrent()
        assertEquals(3, received.single().quantity)
        assertEquals("蝦餃 × 3", received.single().displayName(english = false))
    }

    @Test
    fun `a logged meal re-opens with its modifiers pre-selected, ignoring stale ids`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        collect(viewModel)

        viewModel.startAdjust("cha_siu_rice", listOf("meat_lean", "sauce_none", "long_gone", "sugar_none"))
        runCurrent()

        val adjust = assertNotNull(viewModel.state().adjust)
        assertEquals(mapOf("meat_cut" to "meat_lean", "sauce" to "sauce_none"), adjust.selected)
        assertEquals(719.0, adjust.breakdown.totals.kcal, 1e-9)

        // A row that no longer exists opens nothing.
        viewModel.closeAdjust()
        viewModel.startAdjust("no_such_food", emptyList())
        runCurrent()
        assertNull(viewModel.state().adjust)
    }

    @Test
    fun `the meal name lists the modifiers in the student's language`() {
        val selection = FoodSelection(
            item = chaSiuRice(),
            modifiers = chaSiuModifiers().filter { it.id in setOf("meat_lean", "rice_less") },
            quantity = 1,
            totals = com.defat.core.domain.model.MacroTotals(689.0, null, null, null),
        )
        assertEquals("叉燒飯（瘦、少飯）", selection.displayName(english = false))
        assertEquals("Barbecued pork rice (Lean meat, Less rice)", selection.displayName(english = true))
    }
}

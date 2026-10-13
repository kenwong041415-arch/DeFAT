package com.defat.app.ui

import com.defat.app.fake.FakeFoodRepository
import com.defat.app.fake.chaSiuModifiers
import com.defat.app.fake.chaSiuRice
import com.defat.app.ui.food.FoodLibraryEditorViewModel
import com.defat.core.domain.model.FoodConfidence
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
class FoodLibraryEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.newViewModel(repository: FakeFoodRepository): FoodLibraryEditorViewModel {
        val viewModel = FoodLibraryEditorViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun repository() = FakeFoodRepository(listOf(chaSiuRice()), chaSiuModifiers())

    @Test
    fun `editing a food row makes it the owner's, permanently`() = runTest(testDispatcher) {
        val repository = repository()
        val viewModel = newViewModel(repository)

        viewModel.startEditItem(chaSiuRice())
        runCurrent()
        assertEquals("939", viewModel.uiState.value.editing!!.kcalText)

        viewModel.setKcal("880")
        viewModel.setProtein("45.5")
        runCurrent()
        viewModel.save()
        runCurrent()

        val saved = assertNotNull(repository.lastUpsertedItem)
        assertEquals(880.0, saved.kcal, 0.0)
        assertEquals(45.5, saved.proteinG!!, 0.0)
        assertNull(saved.carbsG, "a blank macro stays unknown rather than becoming 0")
        assertEquals(FoodSource.TRAINER, saved.source)
        assertEquals(FoodConfidence.HIGH, saved.confidence)
        assertTrue(saved.editedByOwner)
        assertNull(saved.kcalLow)
        assertNull(viewModel.uiState.value.editing, "the dialog closes after saving")
    }

    @Test
    fun `a bad number blocks the save and nothing is written`() = runTest(testDispatcher) {
        val repository = repository()
        val viewModel = newViewModel(repository)

        viewModel.startEditItem(chaSiuRice())
        viewModel.setKcal("0")
        runCurrent()
        assertFalse(viewModel.uiState.value.editing!!.isValid)
        viewModel.save()
        runCurrent()
        assertNull(repository.lastUpsertedItem)

        viewModel.setKcal("900")
        viewModel.setFat("lots")
        runCurrent()
        assertFalse(viewModel.uiState.value.editing!!.isValid)
        viewModel.save()
        runCurrent()
        assertNull(repository.lastUpsertedItem)
    }

    @Test
    fun `a modifier delta can be edited and may be negative`() = runTest(testDispatcher) {
        val repository = repository()
        val viewModel = newViewModel(repository)
        val noSauce = chaSiuModifiers().first { it.id == "sauce_none" }

        viewModel.startEditModifier(noSauce)
        runCurrent()
        assertEquals("-100", viewModel.uiState.value.editing!!.kcalText)

        viewModel.setKcal("-85")
        runCurrent()
        viewModel.save()
        runCurrent()

        val saved = assertNotNull(repository.lastUpsertedModifier)
        assertEquals(-85.0, saved.kcalDelta, 0.0)
        assertEquals("sauce", saved.groupId)
        assertTrue(saved.editedByOwner)
        assertEquals(FoodSource.TRAINER, saved.source)
    }

    @Test
    fun `cancel closes the dialog without writing`() = runTest(testDispatcher) {
        val repository = repository()
        val viewModel = newViewModel(repository)

        viewModel.startEditItem(chaSiuRice())
        viewModel.setKcal("500")
        runCurrent()
        viewModel.cancel()
        runCurrent()

        assertNull(viewModel.uiState.value.editing)
        assertNull(repository.lastUpsertedItem)
    }
}

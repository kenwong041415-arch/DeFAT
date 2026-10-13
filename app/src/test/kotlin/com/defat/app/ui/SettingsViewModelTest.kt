package com.defat.app.ui

import com.defat.app.fake.FakeSettingsRepository
import com.defat.app.ui.settings.SettingsViewModel
import java.time.LocalTime
import kotlin.test.assertEquals
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
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the day start defaults to 04-00 and a change is persisted`() = runTest(testDispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch { viewModel.dayStart.collect {} }
        runCurrent()

        assertEquals(LocalTime.of(4, 0), viewModel.dayStart.value)

        viewModel.setDayStart(LocalTime.of(5, 30))
        runCurrent()

        assertEquals(LocalTime.of(5, 30), repository.lastSet)
        assertEquals(LocalTime.of(5, 30), viewModel.dayStart.value)
    }

    @Test
    fun `values outside 00-00 to 08-00 are clamped`() = runTest(testDispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setDayStart(LocalTime.of(11, 0))
        runCurrent()
        assertEquals(LocalTime.of(8, 0), repository.lastSet)

        viewModel.setDayStart(LocalTime.MIDNIGHT)
        runCurrent()
        assertEquals(LocalTime.of(0, 0), repository.lastSet)
    }

    @Test
    fun `the offered options are 00-00 to 08-00 in 30 minute steps`() {
        val viewModel = SettingsViewModel(FakeSettingsRepository())

        assertEquals(17, viewModel.dayStartOptions.size)
        assertEquals(LocalTime.of(0, 0), viewModel.dayStartOptions.first())
        assertEquals(LocalTime.of(8, 0), viewModel.dayStartOptions.last())
        assertEquals(LocalTime.of(4, 30), viewModel.dayStartOptions[9])
    }
}

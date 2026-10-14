package com.defat.app.ui

import com.defat.app.fake.FakeAuthRepository
import com.defat.app.fake.FakeSyncCoordinator
import com.defat.app.ui.profile.AccountViewModel
import com.defat.app.ui.profile.DeleteError
import com.defat.app.ui.profile.DeleteUiState
import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthUser
import com.defat.core.domain.sync.DeleteAccountResult
import com.defat.core.domain.sync.ExportMyDataUseCase
import com.defat.core.domain.sync.SyncStatus
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
class AccountViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var auth: FakeAuthRepository
    private lateinit var coordinator: FakeSyncCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        auth = FakeAuthRepository(initial = AuthUser("uid-1", "owner@example.com"))
        coordinator = FakeSyncCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AccountViewModel(
        auth = auth,
        coordinator = coordinator,
        exportMyData = ExportMyDataUseCase(sources = emptyList(), auth = auth, now = { Instant.parse("2026-10-13T06:00:00Z") }),
    )

    @Test
    fun `the state carries the email and the sync status`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals("owner@example.com", vm.uiState.value.email)

        coordinator.statusState.value = SyncStatus(lastSyncAt = 1_000L, running = false, lastFailed = true)
        runCurrent()
        assertEquals(1_000L, vm.uiState.value.sync.lastSyncAt)
        assertTrue(vm.uiState.value.sync.lastFailed)
    }

    @Test
    fun `signing out clears the session and never deletes anything`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.signOut()
        runCurrent()

        assertNull(auth.currentUserNow())
        assertEquals(listOf("signOut"), auth.calls)
        assertTrue(coordinator.calls.none { it == "deleteAccount" })
    }

    @Test
    fun `a wrong password on delete is reported and the dialog can be cleared`() = runTest(testDispatcher) {
        coordinator.deleteResult = DeleteAccountResult.AuthFailed(AuthError.WRONG_CREDENTIALS)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.deleteAccount("nope")
        runCurrent()

        assertEquals(DeleteUiState.Failed(DeleteError.WRONG_PASSWORD), vm.uiState.value.delete)

        vm.dismissDeleteError()
        runCurrent()
        assertEquals(DeleteUiState.Idle, vm.uiState.value.delete)
    }

    @Test
    fun `a cloud failure is distinguished from a password failure`() = runTest(testDispatcher) {
        coordinator.deleteResult = DeleteAccountResult.CloudDeleteFailed
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.deleteAccount("pw")
        runCurrent()

        assertEquals(DeleteUiState.Failed(DeleteError.CLOUD), vm.uiState.value.delete)
    }

    @Test
    fun `a successful delete returns to idle`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.deleteAccount("pw")
        runCurrent()

        assertEquals(DeleteUiState.Idle, vm.uiState.value.delete)
        assertEquals(listOf("deleteAccount"), coordinator.calls)
    }

    @Test
    fun `export is produced without any sync call`() = runTest(testDispatcher) {
        val vm = viewModel()
        val json = vm.exportJson("0.4.0")

        assertTrue("\"format\": \"defat-export\"" in json)
        assertTrue("owner@example.com" in json)
        assertTrue(coordinator.calls.isEmpty())
    }
}

package com.defat.app.ui

import com.defat.app.fake.FakeAuthRepository
import com.defat.app.fake.FakeProfileRepository
import com.defat.app.fake.FakeSyncCoordinator
import com.defat.app.ui.auth.AuthCompletion
import com.defat.app.ui.auth.AuthUiError
import com.defat.app.ui.auth.AuthViewModel
import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthOutcome
import com.defat.core.domain.auth.AuthUser
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.sync.SyncOutcome
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
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
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val user = AuthUser(uid = "uid-1", email = "owner@example.com")

    private lateinit var auth: FakeAuthRepository
    private lateinit var coordinator: FakeSyncCoordinator
    private lateinit var profiles: FakeProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        auth = FakeAuthRepository()
        coordinator = FakeSyncCoordinator()
        profiles = FakeProfileRepository(initial = null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AuthViewModel(auth, coordinator, profiles)

    private fun existingProfile() = UserProfile(
        sex = Sex.MALE,
        birthDate = LocalDate.of(1990, 1, 1),
        heightCm = 175.0,
        weightKg = 80.0,
        bodyFatPct = 25.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = null,
    )

    private fun AuthViewModel.fill(email: String = "owner@example.com", password: String = "secret1") {
        setEmail(email)
        setPassword(password)
    }

    // ---- validation: nothing is sent for an obviously bad input ----

    @Test
    fun `a malformed email is rejected without calling Firebase`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.fill(email = "not-an-email")
        vm.signIn()
        runCurrent()

        assertEquals(AuthUiError.INVALID_EMAIL, vm.uiState.value.error)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `sign-up with a password under six characters is rejected locally`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.fill(password = "12345")
        vm.signUp()
        runCurrent()

        assertEquals(AuthUiError.WEAK_PASSWORD, vm.uiState.value.error)
        assertTrue(auth.calls.isEmpty())
    }

    // ---- sign-in outcomes ----

    @Test
    fun `a wrong password shows one generic message and never starts a sync`() = runTest(testDispatcher) {
        auth.signInResult = AuthOutcome.Failure(AuthError.WRONG_CREDENTIALS)
        val vm = viewModel()
        vm.fill()
        vm.signIn()
        runCurrent()

        assertEquals(AuthUiError.WRONG_CREDENTIALS, vm.uiState.value.error)
        assertFalse(vm.uiState.value.busy)
        assertTrue(coordinator.calls.isEmpty())
    }

    @Test
    fun `the owner with an existing local profile goes straight to Home and the password is cleared`() = runTest(testDispatcher) {
        profiles = FakeProfileRepository(initial = existingProfile())
        auth.signUpResult = AuthOutcome.Success(user)
        val vm = viewModel()
        vm.fill()
        vm.signUp()
        runCurrent()

        assertEquals(listOf("syncNow"), coordinator.calls)
        assertEquals(AuthCompletion.HOME, vm.uiState.value.completion)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.busy)
    }

    @Test
    fun `a brand new account with no profile goes to onboarding`() = runTest(testDispatcher) {
        auth.signUpResult = AuthOutcome.Success(user)
        val vm = viewModel()
        vm.fill()
        vm.signUp()
        runCurrent()

        assertEquals(AuthCompletion.ONBOARDING, vm.uiState.value.completion)
    }

    @Test
    fun `the completion is consumed once`() = runTest(testDispatcher) {
        auth.signInResult = AuthOutcome.Success(user)
        val vm = viewModel()
        vm.fill()
        vm.signIn()
        runCurrent()
        vm.consumeCompletion()

        assertNull(vm.uiState.value.completion)
    }

    // ---- Case C ----

    @Test
    fun `case C asks first and changes nothing until the user chooses`() = runTest(testDispatcher) {
        auth.signInResult = AuthOutcome.Success(user)
        coordinator.syncResult = SyncOutcome.NeedsReplaceConfirmation
        val vm = viewModel()
        vm.fill()
        vm.signIn()
        runCurrent()

        assertEquals("owner@example.com", vm.uiState.value.replacePromptEmail)
        assertNull(vm.uiState.value.completion)
        assertFalse(vm.uiState.value.busy)
        assertFalse("confirmReplace" in coordinator.calls)
    }

    @Test
    fun `case C Replace calls confirmReplace and then completes`() = runTest(testDispatcher) {
        auth.signInResult = AuthOutcome.Success(user)
        coordinator.syncResult = SyncOutcome.NeedsReplaceConfirmation
        coordinator.replaceResult = SyncOutcome.Synced(pushed = 0, pulled = 12)
        val vm = viewModel()
        vm.fill()
        vm.signIn()
        runCurrent()

        vm.confirmReplace()
        runCurrent()

        assertEquals(listOf("syncNow", "confirmReplace"), coordinator.calls)
        assertNull(vm.uiState.value.replacePromptEmail)
        assertEquals(AuthCompletion.ONBOARDING, vm.uiState.value.completion)
    }

    @Test
    fun `case C Cancel signs the new account out and never replaces anything`() = runTest(testDispatcher) {
        auth.signInResult = AuthOutcome.Success(user)
        coordinator.syncResult = SyncOutcome.NeedsReplaceConfirmation
        val vm = viewModel()
        vm.fill()
        vm.signIn()
        runCurrent()

        vm.cancelReplace()
        runCurrent()

        assertFalse("confirmReplace" in coordinator.calls)
        assertTrue("signOut" in auth.calls)
        assertNull(auth.currentUserNow())
        assertNull(vm.uiState.value.replacePromptEmail)
        assertNull(vm.uiState.value.completion)
        assertEquals("", vm.uiState.value.password)
    }

    @Test
    fun `case C a failed Replace keeps the question open and never opens Home on the other account's data`() =
        runTest(testDispatcher) {
            profiles = FakeProfileRepository(initial = existingProfile())
            auth.signInResult = AuthOutcome.Success(user)
            coordinator.syncResult = SyncOutcome.NeedsReplaceConfirmation
            coordinator.replaceResult = SyncOutcome.Failed(RuntimeException("offline"))
            val vm = viewModel()
            vm.fill()
            vm.signIn()
            runCurrent()

            vm.confirmReplace()
            runCurrent()

            assertNull(vm.uiState.value.completion)
            assertEquals("owner@example.com", vm.uiState.value.replacePromptEmail)
            assertEquals(AuthUiError.SYNC_FAILED, vm.uiState.value.error)
            assertFalse(vm.uiState.value.busy)
        }

    // ---- first sync fails ----

    @Test
    fun `a failed first sync with a profile on the phone still lets the user in`() = runTest(testDispatcher) {
        profiles = FakeProfileRepository(initial = existingProfile())
        auth.signInResult = AuthOutcome.Success(user)
        coordinator.syncResult = SyncOutcome.Failed(RuntimeException("offline"))
        val vm = viewModel()
        vm.fill()
        vm.signIn()
        runCurrent()

        assertEquals(AuthCompletion.HOME, vm.uiState.value.completion)
    }

    @Test
    fun `a failed first sync with no profile does NOT go to onboarding, which would overwrite the cloud profile`() =
        runTest(testDispatcher) {
            auth.signInResult = AuthOutcome.Success(user)
            coordinator.syncResult = SyncOutcome.Failed(RuntimeException("offline"))
            val vm = viewModel()
            vm.fill()
            vm.signIn()
            runCurrent()

            assertNull(vm.uiState.value.completion)
            assertEquals(AuthUiError.SYNC_FAILED, vm.uiState.value.error)
        }

    @Test
    fun `finishSignIn runs a sync for the already signed-in user`() = runTest(testDispatcher) {
        auth = FakeAuthRepository(initial = user)
        val vm = viewModel()
        vm.finishSignIn()
        runCurrent()

        assertEquals(listOf("syncNow"), coordinator.calls)
        assertEquals(AuthCompletion.ONBOARDING, vm.uiState.value.completion)
    }

    // ---- password reset ----

    @Test
    fun `password reset reports sent`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setEmail("owner@example.com")
        vm.sendPasswordReset()
        runCurrent()

        assertTrue(vm.uiState.value.resetSent)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf("reset:owner@example.com"), auth.calls)
    }

    @Test
    fun `password reset with a bad email is rejected locally`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.setEmail("nope")
        vm.sendPasswordReset()
        runCurrent()

        assertEquals(AuthUiError.INVALID_EMAIL, vm.uiState.value.error)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `password reset surfaces a network failure`() = runTest(testDispatcher) {
        auth.resetResult = AuthError.NETWORK
        val vm = viewModel()
        vm.setEmail("owner@example.com")
        vm.sendPasswordReset()
        runCurrent()

        assertEquals(AuthUiError.NETWORK, vm.uiState.value.error)
        assertFalse(vm.uiState.value.resetSent)
    }

    @Test
    fun `typing again clears the previous error`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.fill(email = "bad")
        vm.signIn()
        runCurrent()
        assertEquals(AuthUiError.INVALID_EMAIL, vm.uiState.value.error)

        vm.setEmail("good@example.com")
        assertNull(vm.uiState.value.error)
    }
}

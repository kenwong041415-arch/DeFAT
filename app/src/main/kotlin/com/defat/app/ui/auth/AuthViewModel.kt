package com.defat.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthOutcome
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.auth.AuthValidation
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.sync.SyncCoordinator
import com.defat.core.domain.sync.SyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the user is told. Never a raw Firebase message (it can name internals or leak whether an email exists). */
enum class AuthUiError {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    EMAIL_IN_USE,
    WRONG_CREDENTIALS,
    TOO_MANY_REQUESTS,
    NETWORK,
    UNKNOWN,

    /** Signed in, but the first download of this account's records failed. */
    SYNC_FAILED,
}

/** Where to go once sign-in has finished. Consumed once by the screen. */
enum class AuthCompletion { HOME, ONBOARDING }

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: AuthUiError? = null,
    val resetSent: Boolean = false,
    /** Non-null while the Case C dialog (phase1.5 plan §6.4) is showing; holds the email being signed in. */
    val replacePromptEmail: String? = null,
    val completion: AuthCompletion? = null,
)

/**
 * Sign-up, sign-in, password reset, and the first-sync step that follows them.
 *
 * The password lives only in this state and is cleared as soon as sign-in
 * completes. It is never logged and never stored.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val coordinator: SyncCoordinator,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setEmail(value: String) = _uiState.update { it.copy(email = value, error = null, resetSent = false) }
    fun setPassword(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun signIn() = submit(validatePasswordLength = false) { email, password -> auth.signIn(email, password) }

    fun signUp() = submit(validatePasswordLength = true) { email, password -> auth.signUp(email, password) }

    fun sendPasswordReset() {
        val state = _uiState.value
        if (state.busy) return
        val email = AuthValidation.normaliseEmail(state.email)
        if (!AuthValidation.isEmailValid(email)) {
            _uiState.update { it.copy(error = AuthUiError.INVALID_EMAIL) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null, resetSent = false) }
            val error = auth.sendPasswordReset(email)
            _uiState.update {
                if (error == null) it.copy(busy = false, resetSent = true) else it.copy(busy = false, error = error.toUi())
            }
        }
    }

    /**
     * Resuming a sign-in that never got through its first sync (the app was
     * closed, or the network dropped): the user is already authenticated.
     */
    fun finishSignIn() {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            handle(coordinator.syncNow(), auth.currentUserNow()?.email)
        }
    }

    /** The user chose Replace in the Case C dialog. */
    fun confirmReplace() {
        val email = _uiState.value.replacePromptEmail ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, replacePromptEmail = null) }
            when (val outcome = coordinator.confirmReplace()) {
                // The download failed, so nothing was replaced. Do NOT fall through
                // to handle(): a local profile exists (the other account's), so it
                // would open Home showing one person's records under another's
                // session. Put the question back instead; Cancel signs out.
                is SyncOutcome.Failed ->
                    _uiState.update { it.copy(busy = false, replacePromptEmail = email, error = AuthUiError.SYNC_FAILED) }
                else -> handle(outcome, email)
            }
        }
    }

    /** Cancel: sign the new account out again and change nothing on the phone. */
    fun cancelReplace() {
        viewModelScope.launch {
            auth.signOut()
            _uiState.update { it.copy(replacePromptEmail = null, busy = false, password = "", error = null) }
        }
    }

    /** Clears the session only; the records stay on the phone (plan D7). */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    fun consumeCompletion() = _uiState.update { it.copy(completion = null) }

    private fun submit(
        validatePasswordLength: Boolean,
        call: suspend (email: String, password: String) -> AuthOutcome,
    ) {
        val state = _uiState.value
        if (state.busy) return
        val email = AuthValidation.normaliseEmail(state.email)
        if (!AuthValidation.isEmailValid(email)) {
            _uiState.update { it.copy(error = AuthUiError.INVALID_EMAIL) }
            return
        }
        if (validatePasswordLength && !AuthValidation.isPasswordValid(state.password)) {
            _uiState.update { it.copy(error = AuthUiError.WEAK_PASSWORD) }
            return
        }
        if (state.password.isEmpty()) {
            _uiState.update { it.copy(error = AuthUiError.WRONG_CREDENTIALS) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            when (val outcome = call(email, state.password)) {
                is AuthOutcome.Failure -> _uiState.update { it.copy(busy = false, error = outcome.error.toUi()) }
                is AuthOutcome.Success -> handle(coordinator.syncNow(), outcome.user.email)
            }
        }
    }

    private suspend fun handle(outcome: SyncOutcome, email: String?) {
        when (outcome) {
            is SyncOutcome.Synced -> complete()
            // Case C. Nothing has been touched; the dialog asks first.
            SyncOutcome.NeedsReplaceConfirmation ->
                _uiState.update { it.copy(busy = false, replacePromptEmail = email.orEmpty()) }
            SyncOutcome.NotSignedIn -> _uiState.update { it.copy(busy = false, error = AuthUiError.UNKNOWN) }
            // The account works but the first sync did not. If a profile is already
            // on this phone the app is usable and the next pass retries; if not,
            // sending the user to onboarding would write a new profile that then
            // wins over the one in the cloud, so stay and let them retry.
            is SyncOutcome.Failed ->
                if (profileRepository.onboardingComplete.first()) {
                    complete()
                } else {
                    _uiState.update { it.copy(busy = false, error = AuthUiError.SYNC_FAILED) }
                }
        }
    }

    private suspend fun complete() {
        val onboarded = profileRepository.onboardingComplete.first()
        _uiState.update {
            it.copy(
                busy = false,
                password = "",
                error = null,
                completion = if (onboarded) AuthCompletion.HOME else AuthCompletion.ONBOARDING,
            )
        }
    }

    private fun AuthError.toUi(): AuthUiError = when (this) {
        AuthError.INVALID_EMAIL -> AuthUiError.INVALID_EMAIL
        AuthError.WEAK_PASSWORD -> AuthUiError.WEAK_PASSWORD
        AuthError.EMAIL_IN_USE -> AuthUiError.EMAIL_IN_USE
        AuthError.WRONG_CREDENTIALS -> AuthUiError.WRONG_CREDENTIALS
        AuthError.TOO_MANY_REQUESTS -> AuthUiError.TOO_MANY_REQUESTS
        AuthError.NETWORK -> AuthUiError.NETWORK
        AuthError.NOT_SIGNED_IN, AuthError.UNKNOWN -> AuthUiError.UNKNOWN
    }
}

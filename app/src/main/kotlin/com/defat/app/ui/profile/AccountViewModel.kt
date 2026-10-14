package com.defat.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.sync.DeleteAccountResult
import com.defat.core.domain.sync.ExportMyDataUseCase
import com.defat.core.domain.sync.SyncCoordinator
import com.defat.core.domain.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DeleteError { WRONG_PASSWORD, TOO_MANY_REQUESTS, NETWORK, CLOUD, LOCAL, UNKNOWN }

sealed interface DeleteUiState {
    data object Idle : DeleteUiState
    data object Busy : DeleteUiState
    data class Failed(val error: DeleteError) : DeleteUiState
}

data class AccountUiState(
    val email: String? = null,
    val sync: SyncStatus = SyncStatus(lastSyncAt = null, running = false, lastFailed = false),
    val delete: DeleteUiState = DeleteUiState.Idle,
)

/**
 * The account block on the 我 tab: who is signed in, the sync status line,
 * sign-out, export, and delete-account (phase1.5 plan §7, §7.1).
 *
 * Signing out or deleting does not navigate from here. The app watches the
 * signed-in state and returns to the sign-in screen whenever it becomes false
 * (DefatApp), so every route out of the signed-in state is handled in one place.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val coordinator: SyncCoordinator,
    private val exportMyData: ExportMyDataUseCase,
) : ViewModel() {

    private val delete = MutableStateFlow<DeleteUiState>(DeleteUiState.Idle)

    val uiState: StateFlow<AccountUiState> =
        combine(auth.currentUser, coordinator.status, delete) { user, status, deleteState ->
            AccountUiState(email = user?.email, sync = status, delete = deleteState)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    /** Clears the session only. The records stay on the phone (plan D7). */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    /** The whole export as JSON text. Local rows only; never touches the network. */
    suspend fun exportJson(appVersion: String): String = exportMyData(appVersion)

    fun deleteAccount(password: String) {
        if (delete.value == DeleteUiState.Busy) return
        viewModelScope.launch {
            delete.value = DeleteUiState.Busy
            delete.value = when (val result = coordinator.deleteAccount(password)) {
                DeleteAccountResult.Deleted, DeleteAccountResult.NotSignedIn -> DeleteUiState.Idle
                is DeleteAccountResult.AuthFailed -> DeleteUiState.Failed(result.error.toDeleteError())
                DeleteAccountResult.CloudDeleteFailed -> DeleteUiState.Failed(DeleteError.CLOUD)
                DeleteAccountResult.LocalWipeFailed -> DeleteUiState.Failed(DeleteError.LOCAL)
            }
        }
    }

    fun dismissDeleteError() = delete.update { if (it is DeleteUiState.Failed) DeleteUiState.Idle else it }

    private fun AuthError.toDeleteError(): DeleteError = when (this) {
        AuthError.WRONG_CREDENTIALS -> DeleteError.WRONG_PASSWORD
        AuthError.TOO_MANY_REQUESTS -> DeleteError.TOO_MANY_REQUESTS
        AuthError.NETWORK -> DeleteError.NETWORK
        else -> DeleteError.UNKNOWN
    }
}

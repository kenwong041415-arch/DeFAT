package com.defat.app.fake

import com.defat.core.domain.auth.SyncAccountState
import com.defat.core.domain.sync.DeleteAccountResult
import com.defat.core.domain.sync.SyncCoordinator
import com.defat.core.domain.sync.SyncOutcome
import com.defat.core.domain.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncCoordinator : SyncCoordinator {

    var syncResult: SyncOutcome = SyncOutcome.Synced(pushed = 0, pulled = 0)
    var replaceResult: SyncOutcome = SyncOutcome.Synced(pushed = 0, pulled = 0)
    var deleteResult: DeleteAccountResult = DeleteAccountResult.Deleted

    val calls = mutableListOf<String>()

    val statusState = MutableStateFlow(SyncStatus(lastSyncAt = null, running = false, lastFailed = false))
    override val status: Flow<SyncStatus> = statusState
    override val accountState: Flow<SyncAccountState> = MutableStateFlow(SyncAccountState.NEVER_SYNCED)

    override suspend fun syncNow(): SyncOutcome {
        calls += "syncNow"
        return syncResult
    }

    override suspend fun confirmReplace(): SyncOutcome {
        calls += "confirmReplace"
        return replaceResult
    }

    override suspend fun deleteAccount(password: String): DeleteAccountResult {
        calls += "deleteAccount"
        return deleteResult
    }
}

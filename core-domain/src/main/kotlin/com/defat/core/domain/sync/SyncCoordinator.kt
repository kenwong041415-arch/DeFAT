package com.defat.core.domain.sync

import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.auth.SyncAccountState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SyncStatus(
    /** Epoch millis of the last pass that completed, or null if none ever has. */
    val lastSyncAt: Long?,
    val running: Boolean,
    /** The most recent pass failed; cleared by the next success. */
    val lastFailed: Boolean,
)

sealed interface SyncOutcome {
    data class Synced(val pushed: Int, val pulled: Int) : SyncOutcome

    /**
     * Case C: this phone holds another account's records. Nothing was changed.
     * Only the sign-in screen may resolve it, with an explicit choice.
     */
    data object NeedsReplaceConfirmation : SyncOutcome

    data object NotSignedIn : SyncOutcome
    data class Failed(val cause: Throwable) : SyncOutcome
}

sealed interface DeleteAccountResult {
    data object Deleted : DeleteAccountResult
    data object NotSignedIn : DeleteAccountResult

    /** The password check or the Auth deletion failed. Nothing was deleted if this came from the password check. */
    data class AuthFailed(val error: AuthError) : DeleteAccountResult

    /** The cloud tree could not be emptied. Nothing else was touched; safe to retry. */
    data object CloudDeleteFailed : DeleteAccountResult

    /** Cloud data and the account are gone, but clearing this phone failed. */
    data object LocalWipeFailed : DeleteAccountResult
}

interface SyncCoordinator {
    val status: Flow<SyncStatus>

    /** How the signed-in uid relates to the account whose first pass last completed here (§6.3). */
    val accountState: Flow<SyncAccountState>

    /** One pass for the signed-in user. Never destroys anything without confirmation. */
    suspend fun syncNow(): SyncOutcome

    /**
     * The user chose Replace in the Case C dialog. Does nothing unless the
     * decision is *still* Case C, so a stale tap can never wipe the phone.
     */
    suspend fun confirmReplace(): SyncOutcome

    suspend fun deleteAccount(password: String): DeleteAccountResult
}

/**
 * Serialises every sync pass and the account deletion behind one lock, so a
 * background upload can never run between "cloud emptied" and "account deleted".
 */
class DefaultSyncCoordinator(
    private val auth: AuthRepository,
    private val sources: List<SyncableCollection>,
    private val remoteFor: (uid: String) -> RemoteSyncStore,
    private val state: SyncStateStore,
    private val nowMillis: () -> Long,
    private val batchSize: Int = PUSH_BATCH_SIZE,
) : SyncCoordinator {

    /**
     * The flow's first value rather than a synchronous read: in a fresh process
     * (a WorkManager job) Firebase may not have restored the saved session at the
     * instant of the call, and the first emission is the moment it has.
     */
    private suspend fun currentUser() = auth.currentUser.first()

    private val gate = Mutex()
    private val running = MutableStateFlow(false)
    private val failed = MutableStateFlow(false)

    override val status: Flow<SyncStatus> =
        combine(state.lastSyncAt, running, failed) { last, isRunning, isFailed ->
            SyncStatus(lastSyncAt = last, running = isRunning, lastFailed = isFailed)
        }

    override val accountState: Flow<SyncAccountState> =
        combine(auth.currentUser, state.lastSignedInUid) { user, last ->
            when {
                user == null || last == null -> SyncAccountState.NEVER_SYNCED
                user.uid == last -> SyncAccountState.SAME_ACCOUNT
                else -> SyncAccountState.OTHER_ACCOUNT
            }
        }

    override suspend fun syncNow(): SyncOutcome = gate.withLock {
        val user = currentUser() ?: return@withLock SyncOutcome.NotSignedIn
        guarded {
            val engine = SyncEngine(sources, remoteFor(user.uid), state, batchSize)
            runPlan(decide(engine, user.uid), engine, user.uid)
        }
    }

    override suspend fun confirmReplace(): SyncOutcome = gate.withLock {
        val user = currentUser() ?: return@withLock SyncOutcome.NotSignedIn
        guarded {
            val engine = SyncEngine(sources, remoteFor(user.uid), state, batchSize)
            val plan = decide(engine, user.uid)
            if (plan == SignInPlan.CONFIRM_REPLACE) {
                val pulled = engine.replaceLocalWithRemote()
                state.setLastSignedInUid(user.uid)
                SyncOutcome.Synced(pushed = 0, pulled = pulled)
            } else {
                // The situation changed since the dialog was shown. Replacing now
                // would be destructive for no reason; do the normal pass instead.
                runPlan(plan, engine, user.uid)
            }
        }
    }

    private suspend fun decide(engine: SyncEngine, uid: String): SignInPlan =
        SyncDecision.decide(engine.hasLocalRows(), state.getLastSignedInUid(), uid)

    private suspend fun runPlan(plan: SignInPlan, engine: SyncEngine, uid: String): SyncOutcome = when (plan) {
        SignInPlan.ADOPT_LOCAL -> {
            // Case A. This phone's rows become the account's data: nothing local
            // is removed or overwritten, so an empty cloud can never be mistaken
            // for the truth. adoptLocal() also holds back any row the cloud
            // already has a newer copy of, which is the opposite direction of
            // data loss and just as real.
            val moved = engine.adoptLocal()
            state.setLastSignedInUid(uid)
            SyncOutcome.Synced(pushed = moved, pulled = 0)
        }
        SignInPlan.SYNC_SAME_ACCOUNT -> {
            val pulled = engine.pullSince()
            SyncOutcome.Synced(pushed = engine.pushPending(), pulled = pulled)
        }
        SignInPlan.DOWNLOAD_ONLY -> {
            val pulled = engine.fullPull()
            state.setLastSignedInUid(uid)
            SyncOutcome.Synced(pushed = engine.pushPending(), pulled = pulled)
        }
        SignInPlan.CONFIRM_REPLACE -> SyncOutcome.NeedsReplaceConfirmation
    }

    override suspend fun deleteAccount(password: String): DeleteAccountResult = gate.withLock {
        val user = currentUser() ?: return@withLock DeleteAccountResult.NotSignedIn

        // 1. Prove it is really the owner, before anything is touched.
        auth.reauthenticate(password)?.let { return@withLock DeleteAccountResult.AuthFailed(it) }

        // 2. Cloud data first: after the Auth user is gone the rules would no
        //    longer let us in. A failure here leaves the account and phone intact.
        try {
            RemoteTreeDeleter.deleteAll(remoteFor(user.uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return@withLock DeleteAccountResult.CloudDeleteFailed
        }

        // 3. The Auth user.
        auth.deleteAccount()?.let { return@withLock DeleteAccountResult.AuthFailed(it) }

        // 4. This phone.
        try {
            sources.forEach { it.deleteAllLocal() }
            state.clearAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return@withLock DeleteAccountResult.LocalWipeFailed
        }
        DeleteAccountResult.Deleted
    }

    private suspend fun guarded(block: suspend () -> SyncOutcome): SyncOutcome {
        running.value = true
        return try {
            val outcome = block()
            if (outcome is SyncOutcome.Synced) {
                state.setLastSyncAt(nowMillis())
                failed.value = false
            }
            outcome
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failed.value = true
            SyncOutcome.Failed(e)
        } finally {
            running.value = false
        }
    }
}

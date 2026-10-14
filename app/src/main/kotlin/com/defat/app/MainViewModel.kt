package com.defat.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.data.sync.SyncScheduler
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.auth.StartDestinationPolicy
import com.defat.core.domain.auth.StartScreen
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.sync.SyncCoordinator
import com.defat.core.domain.sync.SyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** null = still loading; the UI shows a progress indicator until this resolves. */
enum class StartDestination { SIGN_IN, FINISH_SIGN_IN, NEEDS_ONBOARDING, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: AuthRepository,
    profileRepository: ProfileRepository,
    private val coordinator: SyncCoordinator,
    private val clock: Clock,
) : ViewModel() {

    /**
     * The sign-in gate (phase1.5 plan §7): signed-out users see the auth screens
     * first, whether or not records are on the phone. The decision itself is
     * [StartDestinationPolicy], which is unit-tested in :core-domain.
     */
    val startDestination: StateFlow<StartDestination?> = combine(
        auth.currentUser,
        profileRepository.onboardingComplete,
        coordinator.accountState,
    ) { user, onboardingComplete, accountState ->
        when (StartDestinationPolicy.decide(user != null, onboardingComplete, accountState)) {
            StartScreen.SIGN_IN -> StartDestination.SIGN_IN
            StartScreen.FINISH_SIGN_IN -> StartDestination.FINISH_SIGN_IN
            StartScreen.ONBOARDING -> StartDestination.NEEDS_ONBOARDING
            StartScreen.HOME -> StartDestination.HOME
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Whether anyone is signed in; null until Firebase has answered. DefatApp returns to sign-in when this turns false. */
    val signedIn: StateFlow<Boolean?> = auth.currentUser
        .map { it != null }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var lastOpenSyncAt: Long? = null

    /**
     * The on-open sync pass (phase1.5 plan D6), called whenever the app comes to
     * the foreground. Throttled so flicking between apps does not hit the network
     * each time; a pass costs a handful of reads when nothing has changed.
     *
     * Does nothing when signed out. A pass that meets another account's records
     * (Case C) changes nothing; only the sign-in screen may ask about that.
     */
    fun syncOnOpen() {
        val now = clock.millis()
        val last = lastOpenSyncAt
        if (last != null && now - last < OPEN_SYNC_MIN_INTERVAL_MS) return
        lastOpenSyncAt = now
        viewModelScope.launch {
            // The flow, not currentUserNow(): right after process start Firebase
            // may not have restored the saved session yet, and the first emission
            // is the moment it has.
            if (auth.currentUser.first() == null) return@launch
            // A failed pass (usually: no network) queues one more for the moment
            // the phone is connected again. Without this the next attempt is the
            // 6-hourly job, and the phase's exit test promises a meal logged
            // offline reaches Firestore "once the network returns", not within
            // the next six hours.
            if (coordinator.syncNow() is SyncOutcome.Failed) {
                SyncScheduler.scheduleCatchUp(appContext)
            }
        }
    }

    private companion object {
        const val OPEN_SYNC_MIN_INTERVAL_MS = 60_000L
    }
}

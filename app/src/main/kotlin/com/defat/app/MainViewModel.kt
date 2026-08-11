package com.defat.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** null = still loading; the UI shows a progress indicator until this resolves. */
enum class StartDestination { NEEDS_ONBOARDING, HOME }

@HiltViewModel
class MainViewModel @Inject constructor(
    profileRepository: ProfileRepository,
) : ViewModel() {

    val startDestination: StateFlow<StartDestination?> = profileRepository.onboardingComplete
        .map { complete -> if (complete) StartDestination.HOME else StartDestination.NEEDS_ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

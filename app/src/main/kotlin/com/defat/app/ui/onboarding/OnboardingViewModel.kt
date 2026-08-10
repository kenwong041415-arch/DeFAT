package com.defat.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.usecase.ComputeDailyTargetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val eventFlow: Flow<OnboardingEvent> = events.receiveAsFlow()

    fun acceptDisclaimer() {
        _uiState.update { it.copy(disclaimerAccepted = true) }
    }

    fun setSex(sex: Sex) = update { it.copy(sex = sex) }

    fun setBirthDate(date: LocalDate) = update { it.copy(birthDate = date) }

    fun setHeightCmText(text: String) = update { it.copy(heightCmText = text) }

    fun setWeightKgText(text: String) = update { it.copy(weightKgText = text) }

    fun setBodyFatPctText(text: String) = update { it.copy(bodyFatPctText = text) }

    fun setBodyFatUnknown(unknown: Boolean) = update {
        it.copy(bodyFatUnknown = unknown, bodyFatPctText = if (unknown) "" else it.bodyFatPctText)
    }

    fun setActivityLevel(level: ActivityLevel) = update { it.copy(activityLevel = level) }

    fun setGoalTargetWeightText(text: String) = update { it.copy(goalTargetWeightText = text) }

    fun setGoalTargetFatText(text: String) = update { it.copy(goalTargetFatText = text) }

    fun setGoalTargetDate(date: LocalDate?) = update { it.copy(goalTargetDate = date) }

    /** Recomputes [OnboardingUiState.dailyTarget] every time relevant fields change. */
    private fun update(transform: (OnboardingUiState) -> OnboardingUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(dailyTarget = computeTargetOrNull(next))
        }
    }

    private fun computeTargetOrNull(state: OnboardingUiState): com.defat.core.domain.model.DailyTarget? {
        val profile = state.toProvisionalProfileOrNull() ?: return null
        return runCatching { computeDailyTarget(profile, LocalDate.now()) }.getOrNull()
    }

    fun completeOnboarding() {
        val profile = _uiState.value.toProvisionalProfileOrNull(
            disclaimerAcceptedAt = Instant.now(),
        ) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            profileRepository.save(profile)
            _uiState.update { it.copy(isSaving = false) }
            events.send(OnboardingEvent.Completed)
        }
    }

    private fun OnboardingUiState.toProvisionalProfileOrNull(
        disclaimerAcceptedAt: Instant? = null,
    ): UserProfile? {
        val sex = sex ?: return null
        val birthDate = birthDate ?: return null
        val heightCm = heightCm ?: return null
        val weightKg = weightKg ?: return null
        val goalTargetWeightKg = goalTargetWeightKg ?: return null
        return UserProfile(
            sex = sex,
            birthDate = birthDate,
            heightCm = heightCm,
            weightKg = weightKg,
            bodyFatPct = bodyFatPct,
            activityLevel = activityLevel,
            goal = Goal(
                targetWeightKg = goalTargetWeightKg,
                targetBodyFatPct = goalTargetFatPct,
                targetDate = goalTargetDate,
            ),
            disclaimerAcceptedAt = disclaimerAcceptedAt,
        )
    }
}

package com.defat.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.usecase.ComputeDailyTargetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Profile is shown read-only plus an inline editable form (plan §8.2 S10 —
 * simplification for Phase 1, no reuse of the onboarding wizard screens).
 */
data class ProfileFormState(
    val sex: Sex = Sex.MALE,
    val birthDate: LocalDate = LocalDate.now().minusYears(30),
    val heightCmText: String = "",
    val weightKgText: String = "",
    val bodyFatPctText: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalTargetWeightText: String = "",
) {
    /** Blank body fat means "unknown" and is valid; anything else must parse and be in range. */
    val isBodyFatValid: Boolean
        get() = bodyFatPctText.isBlank() ||
            bodyFatPctText.toDoubleOrNull()?.let { it in 3.0..75.0 } == true

    val isValid: Boolean
        get() = heightCmText.toDoubleOrNull()?.let { it in 100.0..250.0 } == true &&
            weightKgText.toDoubleOrNull()?.let { it in 30.0..300.0 } == true &&
            goalTargetWeightText.toDoubleOrNull()?.let { it in 30.0..300.0 } == true &&
            isBodyFatValid
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Content(
        val existingProfile: UserProfile,
        val form: ProfileFormState,
        val dailyTarget: DailyTarget?,
        val appVersion: String,
    ) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
) : ViewModel() {

    private val form = MutableStateFlow<ProfileFormState?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(profileRepository.profile, form) { profile, editedForm ->
        if (profile == null) {
            ProfileUiState.Loading
        } else {
            val current = editedForm ?: profile.toFormState()
            val target = runCatching {
                computeDailyTarget(current.toUserProfileOrNull(profile) ?: profile, LocalDate.now())
            }.getOrNull()
            ProfileUiState.Content(
                existingProfile = profile,
                form = current,
                dailyTarget = target,
                appVersion = "0.1.0-phase1",
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    fun updateForm(transform: (ProfileFormState) -> ProfileFormState) {
        val base = (uiState.value as? ProfileUiState.Content)?.form ?: return
        form.value = transform(base)
    }

    fun save() {
        val state = uiState.value as? ProfileUiState.Content ?: return
        if (!state.form.isValid) return
        val updated = state.form.toUserProfileOrNull(state.existingProfile) ?: return
        viewModelScope.launch {
            profileRepository.save(updated)
        }
    }

    private fun UserProfile.toFormState() = ProfileFormState(
        sex = sex,
        birthDate = birthDate,
        heightCmText = heightCm.toString(),
        weightKgText = weightKg.toString(),
        bodyFatPctText = bodyFatPct?.toString() ?: "",
        activityLevel = activityLevel,
        goalTargetWeightText = goal.targetWeightKg.toString(),
    )

    private fun ProfileFormState.toUserProfileOrNull(existing: UserProfile?): UserProfile? {
        val heightCm = heightCmText.toDoubleOrNull() ?: return null
        val weightKg = weightKgText.toDoubleOrNull() ?: return null
        val goalTargetWeightKg = goalTargetWeightText.toDoubleOrNull() ?: return null
        if (!isBodyFatValid) return null
        // Blank = unknown (Mifflin path). Using the text rather than the
        // stored "unknown" flag lets someone who started without a body-fat
        // reading type one in later.
        val bodyFat = bodyFatPctText.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        return UserProfile(
            sex = sex,
            birthDate = birthDate,
            heightCm = heightCm,
            weightKg = weightKg,
            bodyFatPct = bodyFat,
            activityLevel = activityLevel,
            goal = Goal(
                targetWeightKg = goalTargetWeightKg,
                targetBodyFatPct = existing?.goal?.targetBodyFatPct,
                targetDate = existing?.goal?.targetDate,
            ),
            disclaimerAcceptedAt = existing?.disclaimerAcceptedAt,
        )
    }
}

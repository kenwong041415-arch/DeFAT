package com.defat.app.ui.onboarding

import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.Sex
import java.time.LocalDate
import java.time.Period

/**
 * All wizard state in one place, scoped to the onboarding nav graph (plan
 * §8.1) so it survives step navigation. Fields are raw text; parsing and
 * validation happen in [OnboardingViewModel], never in a composable.
 */
data class OnboardingUiState(
    val disclaimerAccepted: Boolean = false,

    val sex: Sex? = null,
    val birthDate: LocalDate? = null,
    val heightCmText: String = "",

    val weightKgText: String = "",
    val bodyFatPctText: String = "",
    val bodyFatUnknown: Boolean = false,

    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,

    val goalTargetWeightText: String = "",
    val goalTargetFatText: String = "",
    val goalTargetDate: LocalDate? = null,

    val dailyTarget: DailyTarget? = null,
    val isSaving: Boolean = false,
) {
    val ageYears: Int?
        get() = birthDate?.let { Period.between(it, LocalDate.now()).years }

    val isBasicsValid: Boolean
        get() = sex != null &&
            (ageYears?.let { it in 13..100 } == true) &&
            (heightCmText.toDoubleOrNull()?.let { it in 100.0..250.0 } == true)

    val isBodyValid: Boolean
        get() {
            val weightOk = weightKgText.toDoubleOrNull()?.let { it in 30.0..300.0 } == true
            val fatOk = bodyFatUnknown ||
                (bodyFatPctText.toDoubleOrNull()?.let { it in 3.0..75.0 } == true)
            return weightOk && fatOk
        }

    val isGoalValid: Boolean
        get() = goalTargetWeightText.toDoubleOrNull()?.let { it in 30.0..300.0 } == true

    val weightKg: Double? get() = weightKgText.toDoubleOrNull()
    val bodyFatPct: Double? get() = if (bodyFatUnknown) null else bodyFatPctText.toDoubleOrNull()
    val heightCm: Double? get() = heightCmText.toDoubleOrNull()
    val goalTargetWeightKg: Double? get() = goalTargetWeightText.toDoubleOrNull()
    val goalTargetFatPct: Double? get() = goalTargetFatText.toDoubleOrNull()
}

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
}

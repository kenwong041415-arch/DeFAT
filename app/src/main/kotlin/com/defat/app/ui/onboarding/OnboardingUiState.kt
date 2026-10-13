package com.defat.app.ui.onboarding

import com.defat.core.domain.calc.NutritionConstants
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.Sex
import com.defat.core.domain.usecase.GoalRate
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
    val goalRate: GoalRate? = null,
    val isSaving: Boolean = false,
) {
    val ageYears: Int?
        get() = birthDate?.let { Period.between(it, LocalDate.now()).years }

    /**
     * DeFAT is adults-only (plan §5.4): Henry's adult equations do not apply to
     * minors, and the medical-disclaimer posture does not cover coaching a
     * child. A birth date under 18 is a validation error, not a silent clamp.
     */
    val isUnderMinAge: Boolean
        get() = ageYears?.let { it < NutritionConstants.HENRY_MIN_ADULT_AGE } == true

    val isBasicsValid: Boolean
        get() = sex != null &&
            (ageYears?.let { it in NutritionConstants.HENRY_MIN_ADULT_AGE..MAX_AGE_YEARS } == true) &&
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

private const val MAX_AGE_YEARS = 100

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
}

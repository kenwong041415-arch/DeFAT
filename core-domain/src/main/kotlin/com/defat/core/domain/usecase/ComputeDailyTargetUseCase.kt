package com.defat.core.domain.usecase

import com.defat.core.domain.calc.BmrCalculator
import com.defat.core.domain.calc.CalorieTargetCalculator
import com.defat.core.domain.calc.MacroCalculator
import com.defat.core.domain.calc.TdeeCalculator
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.UserProfile
import java.time.LocalDate
import javax.inject.Inject

/**
 * The single entry point the UI uses to go from a profile to a full
 * [DailyTarget]: BMR -> TDEE -> calorie target -> macros. No screen may
 * call the individual calculators directly.
 */
class ComputeDailyTargetUseCase @Inject constructor() {
    operator fun invoke(
        profile: UserProfile,
        on: LocalDate,
        activeKcal: Double? = null,
        /** The day's manually logged exercise (plan §10.1); 0 leaves the target exactly as before. */
        exerciseKcal: Double = 0.0,
    ): DailyTarget {
        val bmr = BmrCalculator.bmr(profile, on)
        val tdee = TdeeCalculator.tdee(bmr.kcal, profile.activityLevel, activeKcal, exerciseKcal)
        val target = CalorieTargetCalculator.target(tdee.kcal, profile.sex)
        val macros = MacroCalculator.macroTargets(target.kcal, profile.weightKg, profile.bodyFatPct)
        return DailyTarget(
            bmrKcal = bmr.kcal,
            bmrMethod = bmr.method,
            tdeeKcal = tdee.kcal,
            tdeeSource = tdee.source,
            targetKcal = target.kcal,
            floorApplied = target.floorApplied,
            macros = macros,
        )
    }
}

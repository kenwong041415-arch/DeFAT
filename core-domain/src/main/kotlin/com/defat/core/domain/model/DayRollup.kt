package com.defat.core.domain.model

import java.time.LocalDate

data class DayRollup(
    val date: LocalDate,
    val target: DailyTarget,
    val intakeKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val mealCount: Int,
    /** The day's manually logged exercise, already counted in [target]'s TDEE (plan §10.2). */
    val exerciseKcal: Double = 0.0,
) {
    /** The TDEE the target was built from — higher than the multiplier baseline only on a big exercise day. */
    val tdeeKcal: Double
        get() = target.tdeeKcal

    val remainingKcal: Double
        get() = target.targetKcal - intakeKcal

    val remainingProteinG: Double
        get() = (target.macros.proteinG - proteinG).coerceAtLeast(0.0)

    val isOverTarget: Boolean
        get() = remainingKcal < 0

    val progressFraction: Float
        get() = if (target.targetKcal <= 0.0) {
            0f
        } else {
            (intakeKcal / target.targetKcal).toFloat().coerceIn(0f, 1f)
        }
}

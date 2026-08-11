package com.defat.core.domain.model

/** Which BMR formula produced the result — see docs/04. */
enum class BmrMethod { KATCH_MCARDLE, MIFFLIN_ST_JEOR }

/** Which TDEE computation mode produced the result — see docs/04. */
enum class TdeeSource { ACTIVITY_MULTIPLIER, WEARABLE }

data class DailyTarget(
    val bmrKcal: Double,
    val bmrMethod: BmrMethod,
    val tdeeKcal: Double,
    val tdeeSource: TdeeSource,
    val targetKcal: Double,
    val floorApplied: Boolean,
    val macros: MacroTargets,
)

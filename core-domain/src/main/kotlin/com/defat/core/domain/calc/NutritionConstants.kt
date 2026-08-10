package com.defat.core.domain.calc

/**
 * Single source of truth for every nutrition/metabolism constant. No magic
 * numbers anywhere else in the codebase — see docs/04-health-metabolism.md.
 */
object NutritionConstants {
    // docs/04 — Calorie target: "4 kcal/g protein & carbs, 9 kcal/g fat"
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARB = 4.0
    const val KCAL_PER_G_FAT = 9.0

    // docs/04 — BMR: Katch-McArdle "BMR = 370 + 21.6 x LBM"
    const val KATCH_BASE = 370.0
    const val KATCH_LBM_COEFFICIENT = 21.6

    // docs/04 — TDEE: wearable mode "TDEE = BMR x 1.1 (NEAT/TEF baseline) + activeEnergy"
    const val NEAT_TEF_BASELINE = 1.1 // docs/04 wearable mode; tune with owner

    // docs/04 — Calorie target: "Default deficit: 20% below TDEE"
    const val DEFAULT_DEFICIT_FRACTION = 0.20

    // docs/04 — Calorie target: floor "never below ~1200 kcal female / ~1500 kcal male"
    const val FLOOR_KCAL_MALE = 1500.0
    const val FLOOR_KCAL_FEMALE = 1200.0

    // docs/04 — Calorie target: macros for a cut
    const val PROTEIN_G_PER_KG_BODYWEIGHT = 1.8
    const val PROTEIN_G_PER_KG_LBM = 2.2
    const val MIN_FAT_G_PER_KG = 0.6

    // docs/04 — Trend engine: "7700 kcal ~= 1 kg fat"
    const val KCAL_PER_KG_FAT = 7700.0
    // docs/04 — Calorie target: "Safe rate: 0.5-1.0% bodyweight per week"
    const val SAFE_WEEKLY_LOSS_PCT_MIN = 0.5
    const val SAFE_WEEKLY_LOSS_PCT_MAX = 1.0
}

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

    // Henry (2005) "Oxford" equations — docs/04. kcal/day, W in kg, H in cm.
    // Secondary-sourced coefficients; see docs/04 note 2 before changing them.
    const val HENRY_M_18_30_W = 14.4
    const val HENRY_M_18_30_H = 3.13
    const val HENRY_M_18_30_C = 113.0
    const val HENRY_M_30_60_W = 11.4
    const val HENRY_M_30_60_H = 5.41
    const val HENRY_M_30_60_C = -137.0
    const val HENRY_M_60_PLUS_W = 11.4
    const val HENRY_M_60_PLUS_H = 5.41
    const val HENRY_M_60_PLUS_C = -256.0
    const val HENRY_F_18_30_W = 10.4
    const val HENRY_F_18_30_H = 6.15
    const val HENRY_F_18_30_C = -282.0
    const val HENRY_F_30_60_W = 8.18
    const val HENRY_F_30_60_H = 5.02
    const val HENRY_F_30_60_C = -11.6
    const val HENRY_F_60_PLUS_W = 8.52
    const val HENRY_F_60_PLUS_H = 4.21
    const val HENRY_F_60_PLUS_C = 10.7

    /** Lower bound of the adult band Henry's adult equations cover. */
    const val HENRY_MIN_ADULT_AGE = 18

    // Age at which Henry's bands step (half-open on the lower bound; docs/04).
    const val HENRY_BAND_2_FROM_AGE = 30
    const val HENRY_BAND_3_FROM_AGE = 60

    // docs/04 — TDEE: wearable mode "TDEE = BMR x 1.1 (NEAT/TEF baseline) + activeEnergy"
    const val NEAT_TEF_BASELINE = 1.1 // docs/04 wearable mode; tune with owner

    // Phase 1.7 plan §11 — water goal: 35 ml per kg, rounded down to 100 ml,
    // never below 1500 ml. A common coaching heuristic, not a clinical target.
    const val WATER_ML_PER_KG = 35.0
    const val WATER_GOAL_STEP_ML = 100
    const val WATER_GOAL_MIN_ML = 1500

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

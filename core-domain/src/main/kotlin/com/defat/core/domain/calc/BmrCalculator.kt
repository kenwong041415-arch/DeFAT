package com.defat.core.domain.calc

import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import java.time.LocalDate

data class BmrResult(val kcal: Double, val method: BmrMethod)

/**
 * Basal metabolic rate — docs/04-health-metabolism.md "BMR" section.
 *
 * All arithmetic is Double; rounding is a presentation concern and never
 * happens here (see [Rounding]).
 */
object BmrCalculator {

    /** LBM = weightKg x (1 - fatPct/100). */
    fun leanBodyMassKg(weightKg: Double, bodyFatPct: Double): Double {
        require(weightKg > 0) { "weightKg must be > 0, was $weightKg" }
        requireValidBodyFatPct(bodyFatPct)
        return weightKg * (1 - bodyFatPct / 100.0)
    }

    /** Katch-McArdle: BMR = 370 + 21.6 x LBM (docs/04). Preferred when body fat % is known. */
    fun katchMcArdle(weightKg: Double, bodyFatPct: Double): Double {
        val lbm = leanBodyMassKg(weightKg, bodyFatPct)
        return NutritionConstants.KATCH_BASE + NutritionConstants.KATCH_LBM_COEFFICIENT * lbm
    }

    /**
     * Mifflin-St Jeor (docs/04):
     * male:   10*weightKg + 6.25*heightCm - 5*age + 5
     * female: 10*weightKg + 6.25*heightCm - 5*age - 161
     *
     * NOTE (D4): docs/04 prints the male reference vector as 1780 kcal, but
     * the formula itself gives 1748.75 for its own example (80kg/175cm/30y).
     * This implementation follows the formula; docs/04 was corrected to match.
     */
    fun mifflinStJeor(weightKg: Double, heightCm: Double, ageYears: Int, sex: Sex): Double {
        require(weightKg > 0) { "weightKg must be > 0, was $weightKg" }
        require(heightCm > 0) { "heightCm must be > 0, was $heightCm" }
        require(ageYears >= 0) { "ageYears must be >= 0, was $ageYears" }
        val base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    /**
     * Henry (2005) "Oxford" equations — docs/04. Banded by sex and age;
     * bands are half-open on the lower bound (age 30 -> the 30..59 band,
     * age 60 -> the 60+ band).
     *
     * The bands are discontinuous by design: BMR steps down at each boundary
     * (see docs/04). We implement the published equation exactly and do not
     * interpolate; smoothing would be our invention, not Henry's.
     *
     * @throws IllegalArgumentException if [ageYears] < HENRY_MIN_ADULT_AGE —
     *   DeFAT is an adults-only app and the wizard rejects minors.
     */
    fun henryOxford(weightKg: Double, heightCm: Double, ageYears: Int, sex: Sex): Double {
        require(weightKg > 0) { "weightKg must be > 0, was $weightKg" }
        require(heightCm > 0) { "heightCm must be > 0, was $heightCm" }
        require(ageYears >= NutritionConstants.HENRY_MIN_ADULT_AGE) {
            "ageYears must be >= 18 (adults only), was $ageYears"
        }
        return when (sex) {
            Sex.MALE -> when {
                ageYears < NutritionConstants.HENRY_BAND_2_FROM_AGE ->
                    NutritionConstants.HENRY_M_18_30_W * weightKg +
                        NutritionConstants.HENRY_M_18_30_H * heightCm +
                        NutritionConstants.HENRY_M_18_30_C
                ageYears < NutritionConstants.HENRY_BAND_3_FROM_AGE ->
                    NutritionConstants.HENRY_M_30_60_W * weightKg +
                        NutritionConstants.HENRY_M_30_60_H * heightCm +
                        NutritionConstants.HENRY_M_30_60_C
                else ->
                    NutritionConstants.HENRY_M_60_PLUS_W * weightKg +
                        NutritionConstants.HENRY_M_60_PLUS_H * heightCm +
                        NutritionConstants.HENRY_M_60_PLUS_C
            }
            Sex.FEMALE -> when {
                ageYears < NutritionConstants.HENRY_BAND_2_FROM_AGE ->
                    NutritionConstants.HENRY_F_18_30_W * weightKg +
                        NutritionConstants.HENRY_F_18_30_H * heightCm +
                        NutritionConstants.HENRY_F_18_30_C
                ageYears < NutritionConstants.HENRY_BAND_3_FROM_AGE ->
                    NutritionConstants.HENRY_F_30_60_W * weightKg +
                        NutritionConstants.HENRY_F_30_60_H * heightCm +
                        NutritionConstants.HENRY_F_30_60_C
                else ->
                    NutritionConstants.HENRY_F_60_PLUS_W * weightKg +
                        NutritionConstants.HENRY_F_60_PLUS_H * heightCm +
                        NutritionConstants.HENRY_F_60_PLUS_C
            }
        }
    }

    /**
     * Selects Katch-McArdle when [UserProfile.bodyFatPct] is known, otherwise
     * Henry (2005) "Oxford" (docs/04). Mifflin-St Jeor stays implemented as the
     * alternative but is not selected here.
     */
    fun bmr(profile: UserProfile, on: LocalDate): BmrResult {
        val fatPct = profile.bodyFatPct
        return if (fatPct != null) {
            BmrResult(katchMcArdle(profile.weightKg, fatPct), BmrMethod.KATCH_MCARDLE)
        } else {
            val age = profile.ageYears(on)
            BmrResult(
                henryOxford(profile.weightKg, profile.heightCm, age, profile.sex),
                BmrMethod.HENRY_OXFORD,
            )
        }
    }

    private fun requireValidBodyFatPct(bodyFatPct: Double) {
        require(bodyFatPct in 0.0..75.0) {
            "bodyFatPct must be in 0.0..75.0, was $bodyFatPct"
        }
    }
}

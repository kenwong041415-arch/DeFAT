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
     * Selects Katch-McArdle when [UserProfile.bodyFatPct] is known, otherwise
     * falls back to Mifflin-St Jeor (docs/04).
     */
    fun bmr(profile: UserProfile, on: LocalDate): BmrResult {
        val fatPct = profile.bodyFatPct
        return if (fatPct != null) {
            BmrResult(katchMcArdle(profile.weightKg, fatPct), BmrMethod.KATCH_MCARDLE)
        } else {
            val age = profile.ageYears(on)
            BmrResult(
                mifflinStJeor(profile.weightKg, profile.heightCm, age, profile.sex),
                BmrMethod.MIFFLIN_ST_JEOR,
            )
        }
    }

    private fun requireValidBodyFatPct(bodyFatPct: Double) {
        require(bodyFatPct in 0.0..75.0) {
            "bodyFatPct must be in 0.0..75.0, was $bodyFatPct"
        }
    }
}

package com.defat.core.domain.calc

import com.defat.core.domain.model.MacroTargets

/** Macro split for a cut — docs/04-health-metabolism.md "Calorie target" section. */
object MacroCalculator {

    fun macroTargets(targetKcal: Double, weightKg: Double, bodyFatPct: Double?): MacroTargets {
        require(targetKcal > 0) { "targetKcal must be > 0, was $targetKcal" }
        require(weightKg > 0) { "weightKg must be > 0, was $weightKg" }

        val proteinG = if (bodyFatPct != null) {
            val lbm = BmrCalculator.leanBodyMassKg(weightKg, bodyFatPct)
            NutritionConstants.PROTEIN_G_PER_KG_LBM * lbm
        } else {
            NutritionConstants.PROTEIN_G_PER_KG_BODYWEIGHT * weightKg
        }
        val fatG = NutritionConstants.MIN_FAT_G_PER_KG * weightKg

        val rawCarbsG = (
            targetKcal -
                NutritionConstants.KCAL_PER_G_PROTEIN * proteinG -
                NutritionConstants.KCAL_PER_G_FAT * fatG
            ) / NutritionConstants.KCAL_PER_G_CARB

        return if (rawCarbsG < 0.0) {
            MacroTargets(proteinG = proteinG, fatG = fatG, carbsG = 0.0, carbsClamped = true)
        } else {
            MacroTargets(proteinG = proteinG, fatG = fatG, carbsG = rawCarbsG, carbsClamped = false)
        }
    }
}

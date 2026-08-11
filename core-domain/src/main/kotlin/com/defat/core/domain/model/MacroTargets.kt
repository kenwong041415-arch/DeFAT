package com.defat.core.domain.model

import com.defat.core.domain.calc.NutritionConstants

data class MacroTargets(
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val carbsClamped: Boolean, // true when carbs would have been < 0
) {
    val energyKcal: Double
        get() = proteinG * NutritionConstants.KCAL_PER_G_PROTEIN +
            carbsG * NutritionConstants.KCAL_PER_G_CARB +
            fatG * NutritionConstants.KCAL_PER_G_FAT
}

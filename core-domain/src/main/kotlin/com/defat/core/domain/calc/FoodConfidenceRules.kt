package com.defat.core.domain.calc

import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodSource

/** docs/07 §3. Confidence is derived from source and spread, then stored. */
object FoodConfidenceRules {

    /** A published spread wider than this share of the point value makes a row LOW. */
    const val WIDE_SPREAD_FRACTION: Double = 0.25

    /**
     * Rules, in order:
     * 1. TRAINER or OFFICIAL -> HIGH.
     * 2. MEDIA -> LOW.
     * 3. A range exists and (high - low) / kcal > 25% -> LOW.
     * 4. COMPOSITION_TABLE or DIETITIAN -> MEDIUM.
     * 5. USER -> MEDIUM.
     *
     * "A range exists" means both bounds are present.
     */
    fun confidenceFor(
        source: FoodSource,
        kcal: Double,
        kcalLow: Double?,
        kcalHigh: Double?,
    ): FoodConfidence {
        if (source == FoodSource.TRAINER || source == FoodSource.OFFICIAL) return FoodConfidence.HIGH
        if (source == FoodSource.MEDIA) return FoodConfidence.LOW
        if (kcalLow != null && kcalHigh != null && kcal > 0.0 &&
            (kcalHigh - kcalLow) / kcal > WIDE_SPREAD_FRACTION
        ) {
            return FoodConfidence.LOW
        }
        return when (source) {
            FoodSource.COMPOSITION_TABLE, FoodSource.DIETITIAN, FoodSource.USER -> FoodConfidence.MEDIUM
            // Unreachable: handled by rules 1 and 2. Listed so a new FoodSource
            // fails to compile here instead of silently becoming MEDIUM.
            FoodSource.TRAINER, FoodSource.OFFICIAL -> FoodConfidence.HIGH
            FoodSource.MEDIA -> FoodConfidence.LOW
        }
    }
}

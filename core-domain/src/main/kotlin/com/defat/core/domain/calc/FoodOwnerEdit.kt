package com.defat.core.domain.calc

import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.FoodSource

/**
 * The owner's edit of a seed row (plan §8.5): the number becomes theirs, so
 * the source is TRAINER, the confidence is derived from that, the row is
 * marked `editedByOwner` (so a later app update never overwrites it — plan
 * §8.3), and any published range is cleared, because it described someone
 * else's estimate.
 */
object FoodOwnerEdit {

    fun editItem(
        item: FoodItem,
        kcal: Double,
        proteinG: Double?,
        carbsG: Double?,
        fatG: Double?,
    ): FoodItem {
        require(kcal.isFinite() && kcal > 0.0) { "kcal must be > 0, was $kcal" }
        return item.copy(
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            kcalLow = null,
            kcalHigh = null,
            source = FoodSource.TRAINER,
            confidence = FoodConfidenceRules.confidenceFor(FoodSource.TRAINER, kcal, null, null),
            editedByOwner = true,
        )
    }

    /** Modifier deltas are owner-editable exactly like food rows (docs/07 §6.3 rule 4). */
    fun editModifier(
        modifier: FoodModifier,
        kcalDelta: Double,
        proteinDelta: Double?,
        carbsDelta: Double?,
        fatDelta: Double?,
    ): FoodModifier {
        require(kcalDelta.isFinite()) { "kcalDelta must be finite, was $kcalDelta" }
        return modifier.copy(
            kcalDelta = kcalDelta,
            proteinDelta = proteinDelta,
            carbsDelta = carbsDelta,
            fatDelta = fatDelta,
            source = FoodSource.TRAINER,
            confidence = FoodConfidenceRules.confidenceFor(FoodSource.TRAINER, kotlin.math.abs(kcalDelta), null, null),
            editedByOwner = true,
        )
    }
}

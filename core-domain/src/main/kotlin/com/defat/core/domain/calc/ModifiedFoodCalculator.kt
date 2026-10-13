package com.defat.core.domain.calc

import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.MacroTotals

/**
 * Everything the 點食法 screen needs to show its own arithmetic (plan §8.6):
 * the base, each modifier's delta, and the result. The screen renders these
 * fields; it does no maths of its own.
 */
data class ModifiedFoodBreakdown(
    val base: FoodItem,
    /** The chosen modifiers, in the order given. */
    val modifiers: List<FoodModifier>,
    val quantity: Int,
    /** `base.kcal + sum(deltas)` before clamping and before [quantity]. */
    val unclampedUnitKcal: Double,
    /** True when [unclampedUnitKcal] was below zero and the result was held at zero. */
    val clampedAtZero: Boolean,
    /** One serving after the modifiers, clamped at zero. */
    val perUnit: MacroTotals,
    /** [perUnit] x [quantity] — the figure that is logged. */
    val totals: MacroTotals,
)

/**
 * docs/07 §6.3. A [FoodItem]'s stored numbers are the standard as-served
 * configuration; deltas are relative to that, so 走汁 is -100 from a row that
 * already includes sauce.
 */
object ModifiedFoodCalculator {

    /**
     * `(base + deltas) x quantity`, per macro, clamped at zero.
     *
     * - At most one modifier per `groupId`: two options from one group is a
     *   programming error, so it throws rather than letting the last one win.
     * - A modifier that does not apply to [base] (e.g. 走糖 on 叉燒飯) throws.
     * - A macro is null when the base or any applied modifier has no number for
     *   it. Reporting protein as "unchanged" when no source published a protein
     *   delta would be false precision (docs/07 §2).
     * - Quantity multiplies after the modifiers.
     */
    fun apply(base: FoodItem, modifiers: List<FoodModifier>, quantity: Int = 1): MacroTotals =
        breakdown(base, modifiers, quantity).totals

    fun breakdown(base: FoodItem, modifiers: List<FoodModifier>, quantity: Int = 1): ModifiedFoodBreakdown {
        require(quantity >= 1) { "quantity must be >= 1, was $quantity" }

        val groups = modifiers.map { it.groupId }
        require(groups.size == groups.toSet().size) {
            "at most one modifier per group, got groups $groups"
        }
        modifiers.forEach { modifier ->
            require(modifier.canApplyTo(base)) {
                "modifier ${modifier.id} does not apply to ${base.id} (${base.category})"
            }
        }

        val unclamped = base.kcal + modifiers.sumOf { it.kcalDelta }
        val perUnit = MacroTotals(
            kcal = unclamped.coerceAtLeast(0.0),
            proteinG = combine(base.proteinG, modifiers.map { it.proteinDelta }),
            carbsG = combine(base.carbsG, modifiers.map { it.carbsDelta }),
            fatG = combine(base.fatG, modifiers.map { it.fatDelta }),
        )
        return ModifiedFoodBreakdown(
            base = base,
            modifiers = modifiers,
            quantity = quantity,
            unclampedUnitKcal = unclamped,
            clampedAtZero = unclamped < 0.0,
            perUnit = perUnit,
            totals = MacroTotals(
                kcal = perUnit.kcal * quantity,
                proteinG = perUnit.proteinG?.times(quantity),
                carbsG = perUnit.carbsG?.times(quantity),
                fatG = perUnit.fatG?.times(quantity),
            ),
        )
    }

    /** null if the base or any delta is unknown; otherwise the clamped sum. */
    private fun combine(base: Double?, deltas: List<Double?>): Double? {
        if (base == null) return null
        var sum = base
        for (delta in deltas) {
            if (delta == null) return null
            sum += delta
        }
        return sum.coerceAtLeast(0.0)
    }
}

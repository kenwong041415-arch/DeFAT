package com.defat.app.ui.food

import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.MacroTotals

/**
 * What the student picked from the 食物庫: a row, the modifiers chosen in
 * 點食法, a quantity, and the resulting numbers. [totals] comes from
 * ModifiedFoodCalculator (or, for a row picked without adjusting, from
 * FoodSearch.prefillKcal) — nothing is computed here.
 */
data class FoodSelection(
    val item: FoodItem,
    val modifiers: List<FoodModifier>,
    val quantity: Int,
    val totals: MacroTotals,
) {
    /** The meal name: 叉燒飯（瘦、少飯、走汁） or 蝦餃 × 3. */
    fun displayName(english: Boolean): String {
        val base = item.displayName(english)
        val separator = if (english) ", " else "、"
        val withModifiers = if (modifiers.isEmpty()) {
            base
        } else {
            val joined = modifiers.joinToString(separator) { it.displayName(english) }
            if (english) "$base ($joined)" else "$base（$joined）"
        }
        return if (quantity > 1) "$withModifiers × $quantity" else withModifiers
    }
}

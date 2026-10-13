package com.defat.core.domain.calc

import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodSource
import kotlin.math.floor

/** The small label next to a food's name (docs/07 §3, plan §8.4). */
enum class FoodBadge {
    /** 約 — standard composition-table values; the portion is still an estimate. */
    APPROX,

    /** 估算 — dietitian or press figures. */
    ESTIMATE,

    /** 教練 — the owner entered or corrected it. */
    TRAINER,

    /** 自訂 — the student's own entry. */
    CUSTOM,
}

object FoodBadges {
    /**
     * By source, per docs/07 §3: none for OFFICIAL (the chain's own data), 約
     * for COMPOSITION_TABLE, 估算 for DIETITIAN and MEDIA, 教練 for TRAINER,
     * 自訂 for USER. The badge is not decoration (plan §17 R4): it is what
     * stops the app asserting that 西多士 is 522 kcal.
     */
    fun forSource(source: FoodSource): FoodBadge? = when (source) {
        FoodSource.OFFICIAL -> null
        FoodSource.COMPOSITION_TABLE -> FoodBadge.APPROX
        FoodSource.DIETITIAN, FoodSource.MEDIA -> FoodBadge.ESTIMATE
        FoodSource.TRAINER -> FoodBadge.TRAINER
        FoodSource.USER -> FoodBadge.CUSTOM
    }
}

/** Search and pre-fill rules for the 搵食物 sheet (plan §8.4). No fuzzy matching, no network. */
object FoodSearch {

    /** Unexplained ranges pre-fill their midpoint rounded to this many kcal. */
    const val PREFILL_ROUND_KCAL: Int = 10

    /**
     * Filters [items] (keeping their order) by [category] and by [query]:
     * `nameZh` by substring, `nameEn` by case-insensitive substring. A blank
     * query matches everything.
     */
    fun filter(items: List<FoodItem>, query: String, category: FoodCategory?): List<FoodItem> {
        val q = query.trim()
        return items.filter { item ->
            (category == null || item.category == category) &&
                (q.isEmpty() || item.nameZh.contains(q) || item.nameEn.contains(q, ignoreCase = true))
        }
    }

    /** True when the row still carries a published range that no modifier explains. */
    fun hasUnexplainedRange(item: FoodItem): Boolean = item.kcalLow != null && item.kcalHigh != null

    /**
     * The calories to pre-fill when a row is picked without adjusting it: the
     * row's own number, or — for a row with an unexplained range — the midpoint
     * of that range rounded to the nearest 10 kcal (docs/07 §6.3 rule 2, plan
     * §8.4 item 6).
     */
    fun prefillKcal(item: FoodItem): Double {
        val low = item.kcalLow
        val high = item.kcalHigh
        if (low == null || high == null) return item.kcal
        val midpoint = (low + high) / 2.0
        val step = PREFILL_ROUND_KCAL
        return floor(midpoint / step + 0.5) * step
    }
}

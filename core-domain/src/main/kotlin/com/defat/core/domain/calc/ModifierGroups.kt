package com.defat.core.domain.calc

import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier

/**
 * The options of one modifier group, ready for a row of chips.
 *
 * [options] are sorted from the most negative delta to the most positive. The
 * default option (正常 / 半肥瘦 / 無) is not a [FoodModifier]; it is "none
 * chosen", and sits at [defaultIndex] so the chips read 少飯 · 正常 · 加飯.
 */
data class ModifierGroup(
    val groupId: String,
    val options: List<FoodModifier>,
) {
    val defaultIndex: Int get() = options.count { it.kcalDelta < 0.0 }
}

object ModifierGroups {

    /** docs/07 §6.3 order: 飯量, 肉款, 皮, 醬汁, 甜度, 奶, 牛油, 花生醬, 加配料. */
    val ORDER: List<String> = listOf(
        "rice_amount",
        "meat_cut",
        "skin",
        "sauce",
        "sweetness",
        "milk",
        "butter",
        "peanut_butter",
        "add_ons",
    )

    /** The modifiers that may be attached to [item], one [ModifierGroup] per group, in §6.3 order. */
    fun forItem(item: FoodItem, modifiers: List<FoodModifier>): List<ModifierGroup> =
        modifiers
            .filter { it.canApplyTo(item) }
            .groupBy { it.groupId }
            .map { (groupId, options) ->
                ModifierGroup(groupId, options.sortedWith(compareBy({ it.kcalDelta }, { it.id })))
            }
            .sortedWith(
                compareBy(
                    { ORDER.indexOf(it.groupId).let { index -> if (index < 0) ORDER.size else index } },
                    { it.groupId },
                ),
            )

    /**
     * Whether tapping [item] opens the 點食法 step rather than filling the
     * editor directly: when it has any applicable modifier group, and always
     * for dim sum, which is counted in 件 with a quantity stepper (docs/07
     * §6.2 rule 3).
     */
    fun needsAdjustStep(item: FoodItem, modifiers: List<FoodModifier>): Boolean =
        item.category == FoodCategory.DIM_SUM || forItem(item, modifiers).isNotEmpty()

    /**
     * Turns saved modifier ids back into the modifiers they name, for
     * re-opening a logged meal. Ids that no longer exist, or no longer apply to
     * [item], are dropped; and only the first id of any group is kept, so a
     * hand-edited or stale list can never trip the one-per-group rule.
     */
    fun resolve(item: FoodItem, modifiers: List<FoodModifier>, ids: List<String>): List<FoodModifier> {
        val byId = modifiers.associateBy { it.id }
        val seenGroups = HashSet<String>()
        return ids.mapNotNull { byId[it] }
            .filter { it.canApplyTo(item) }
            .filter { seenGroups.add(it.groupId) }
    }
}

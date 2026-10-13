package com.defat.core.domain.model

/**
 * A signed adjustment to a [FoodItem] (docs/07 §6.3): 少飯, 瘦, 走汁, 半糖...
 *
 * Options in one [groupId] are mutually exclusive. The 正常 / 半肥瘦 / 無
 * "default" option of a group is not a row: it is simply "no modifier chosen
 * from this group". A zero-delta row with unknown macro deltas would turn the
 * macros of an unmodified dish into nulls.
 *
 * Macro deltas are nullable on purpose. docs/07 §6.3 publishes kcal deltas
 * only; a null macro delta means "unknown", and the calculator then reports
 * that macro as unknown for the whole dish rather than pretending it did not
 * change.
 */
data class FoodModifier(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val groupId: String, // options within a group are exclusive
    /** Every item of these categories can take this modifier. */
    val appliesTo: Set<FoodCategory>,
    /**
     * Individual items that can take this modifier regardless of category.
     * docs/07 §6.3 lists "appliesTo: List<FoodCategory> or an explicit item
     * list", and several of its rows are item-level (走牛油 applies to 西多士
     * and 多士, not to every 茶餐廳 dish). Empty = category-only.
     */
    val appliesToItemIds: Set<String> = emptySet(),
    val kcalDelta: Double, // signed
    val proteinDelta: Double?,
    val carbsDelta: Double?,
    val fatDelta: Double?,
    val source: FoodSource,
    val confidence: FoodConfidence,
    val editedByOwner: Boolean,
) {
    /** True when this modifier may be attached to [item]. */
    fun canApplyTo(item: FoodItem): Boolean =
        item.category in appliesTo || item.id in appliesToItemIds
}

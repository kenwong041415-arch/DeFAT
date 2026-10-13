package com.defat.app.ui.food

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.defat.app.R
import com.defat.core.domain.calc.FoodBadge
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier

/** Exhaustive on purpose: a new category without a label must fail the build. */
@StringRes
fun foodCategoryLabelRes(category: FoodCategory): Int = when (category) {
    FoodCategory.TEA_RESTAURANT -> R.string.food_category_tea_restaurant
    FoodCategory.DIM_SUM -> R.string.food_category_dim_sum
    FoodCategory.ROAST_RICE -> R.string.food_category_roast_rice
    FoodCategory.NOODLE -> R.string.food_category_noodle
    FoodCategory.FAST_FOOD -> R.string.food_category_fast_food
    FoodCategory.DRINK -> R.string.food_category_drink
    FoodCategory.STAPLE -> R.string.food_category_staple
    FoodCategory.SNACK -> R.string.food_category_snack
}

/** 約 / 估算 / 教練 / 自訂 — docs/07 §3. Exhaustive on purpose (plan §17 R4). */
@StringRes
fun foodBadgeLabelRes(badge: FoodBadge): Int = when (badge) {
    FoodBadge.APPROX -> R.string.food_badge_approx
    FoodBadge.ESTIMATE -> R.string.food_badge_estimate
    FoodBadge.TRAINER -> R.string.food_badge_trainer
    FoodBadge.CUSTOM -> R.string.food_badge_custom
}

/** The group names of docs/07 §6.3; null for a group id this build has no label for. */
@StringRes
fun modifierGroupLabelRes(groupId: String): Int? = when (groupId) {
    "rice_amount" -> R.string.food_group_rice_amount
    "meat_cut" -> R.string.food_group_meat_cut
    "skin" -> R.string.food_group_skin
    "sauce" -> R.string.food_group_sauce
    "sweetness" -> R.string.food_group_sweetness
    "milk" -> R.string.food_group_milk
    "butter" -> R.string.food_group_butter
    "peanut_butter" -> R.string.food_group_peanut_butter
    "add_ons" -> R.string.food_group_add_ons
    else -> null
}

/** The label of a group's "nothing chosen" option: 正常 / 半肥瘦 / 無. */
@StringRes
fun modifierDefaultLabelRes(groupId: String): Int = when (groupId) {
    "meat_cut" -> R.string.food_default_standard_cut
    "peanut_butter", "add_ons" -> R.string.food_default_none
    else -> R.string.food_default_normal
}

@Composable
fun modifierGroupLabel(groupId: String): String {
    val res = modifierGroupLabelRes(groupId)
    return if (res != null) stringResource(res) else groupId
}

fun FoodItem.displayName(english: Boolean): String = if (english) nameEn else nameZh

fun FoodModifier.displayName(english: Boolean): String = if (english) nameEn else nameZh

package com.defat.core.domain.fake

import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.FoodSource

/** Base 叉燒飯 from plan §15.4a: 939 kcal, 42 g P, 118 g C, 30 g F, ROAST_RICE. */
fun chaSiuRice(
    kcal: Double = 939.0,
    proteinG: Double? = 42.0,
    carbsG: Double? = 118.0,
    fatG: Double? = 30.0,
): FoodItem = FoodItem(
    id = "cha_siu_rice",
    nameZh = "叉燒飯",
    nameEn = "Barbecued pork rice",
    portionLabel = "1 碟（正常飯、半肥瘦、有汁）",
    portionGrams = null,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    kcalLow = null,
    kcalHigh = null,
    source = FoodSource.COMPOSITION_TABLE,
    confidence = FoodConfidence.MEDIUM,
    category = FoodCategory.ROAST_RICE,
    isSeed = true,
    editedByOwner = false,
)

fun foodModifier(
    id: String,
    groupId: String,
    kcalDelta: Double,
    appliesTo: Set<FoodCategory> = setOf(FoodCategory.ROAST_RICE),
    appliesToItemIds: Set<String> = emptySet(),
    proteinDelta: Double? = null,
    carbsDelta: Double? = null,
    fatDelta: Double? = null,
): FoodModifier = FoodModifier(
    id = id,
    nameZh = id,
    nameEn = id,
    groupId = groupId,
    appliesTo = appliesTo,
    appliesToItemIds = appliesToItemIds,
    kcalDelta = kcalDelta,
    proteinDelta = proteinDelta,
    carbsDelta = carbsDelta,
    fatDelta = fatDelta,
    source = FoodSource.COMPOSITION_TABLE,
    confidence = FoodConfidence.MEDIUM,
    editedByOwner = false,
)

/** Docs/07 §6.3 deltas used in the plan's examples. */
val leanMeat = foodModifier("meat_lean", "meat_cut", -120.0)
val lessRice = foodModifier("rice_less", "rice_amount", -130.0)
val noSauce = foodModifier("sauce_none", "sauce", -100.0)
val lessSauce = foodModifier("sauce_less", "sauce", -50.0)
val moreRice = foodModifier("rice_more", "rice_amount", 130.0)
val noSugar = foodModifier("sugar_none", "sweetness", -48.0, appliesTo = setOf(FoodCategory.DRINK))

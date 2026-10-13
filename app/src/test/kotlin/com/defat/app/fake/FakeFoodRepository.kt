package com.defat.app.fake

import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.FoodSource
import com.defat.core.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFoodRepository(
    items: List<FoodItem> = emptyList(),
    modifiers: List<FoodModifier> = emptyList(),
) : FoodRepository {

    private val itemState = MutableStateFlow(items)
    private val modifierState = MutableStateFlow(modifiers)

    var lastUpsertedItem: FoodItem? = null
        private set
    var lastUpsertedModifier: FoodModifier? = null
        private set

    override val items: Flow<List<FoodItem>> = itemState
    override val modifiers: Flow<List<FoodModifier>> = modifierState

    override suspend fun itemById(id: String): FoodItem? = itemState.value.firstOrNull { it.id == id }

    override suspend fun upsertItem(item: FoodItem) {
        lastUpsertedItem = item
        itemState.value = itemState.value.filterNot { it.id == item.id } + item
    }

    override suspend fun upsertModifier(modifier: FoodModifier) {
        lastUpsertedModifier = modifier
        modifierState.value = modifierState.value.filterNot { it.id == modifier.id } + modifier
    }
}

/** docs/07 §6.1 base: 叉燒飯, 939 kcal, no published macros. */
fun chaSiuRice(): FoodItem = FoodItem(
    id = "cha_siu_rice",
    nameZh = "叉燒飯",
    nameEn = "Barbecued pork rice",
    portionLabel = "1 碟（正常飯、半肥瘦、有汁）",
    portionGrams = null,
    kcal = 939.0,
    proteinG = null,
    carbsG = null,
    fatG = null,
    kcalLow = null,
    kcalHigh = null,
    source = FoodSource.COMPOSITION_TABLE,
    confidence = FoodConfidence.MEDIUM,
    category = FoodCategory.ROAST_RICE,
    isSeed = true,
    editedByOwner = false,
)

fun foodItem(
    id: String,
    nameZh: String,
    nameEn: String,
    kcal: Double,
    category: FoodCategory,
    portionLabel: String = "1 份",
    proteinG: Double? = null,
    kcalLow: Double? = null,
    kcalHigh: Double? = null,
    source: FoodSource = FoodSource.MEDIA,
): FoodItem = FoodItem(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    portionLabel = portionLabel,
    portionGrams = null,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = null,
    fatG = null,
    kcalLow = kcalLow,
    kcalHigh = kcalHigh,
    source = source,
    confidence = FoodConfidence.LOW,
    category = category,
    isSeed = true,
    editedByOwner = false,
)

fun foodModifier(
    id: String,
    nameZh: String,
    nameEn: String,
    groupId: String,
    kcalDelta: Double,
    appliesTo: Set<FoodCategory> = emptySet(),
    appliesToItemIds: Set<String> = emptySet(),
): FoodModifier = FoodModifier(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    groupId = groupId,
    appliesTo = appliesTo,
    appliesToItemIds = appliesToItemIds,
    kcalDelta = kcalDelta,
    proteinDelta = null,
    carbsDelta = null,
    fatDelta = null,
    source = FoodSource.COMPOSITION_TABLE,
    confidence = FoodConfidence.MEDIUM,
    editedByOwner = false,
)

/** The three modifiers of the plan's §15.4a example. */
fun chaSiuModifiers(): List<FoodModifier> = listOf(
    foodModifier("meat_lean", "瘦", "Lean meat", "meat_cut", -120.0, appliesToItemIds = setOf("cha_siu_rice")),
    foodModifier("rice_less", "少飯", "Less rice", "rice_amount", -130.0, appliesTo = setOf(FoodCategory.ROAST_RICE)),
    foodModifier("rice_more", "加飯", "Extra rice", "rice_amount", 130.0, appliesTo = setOf(FoodCategory.ROAST_RICE)),
    foodModifier("sauce_none", "走汁", "No sauce", "sauce", -100.0, appliesTo = setOf(FoodCategory.ROAST_RICE)),
    foodModifier("sugar_none", "走糖", "No sugar", "sweetness", -48.0, appliesTo = setOf(FoodCategory.DRINK)),
)

package com.defat.core.data.mapper

import com.defat.core.data.db.entity.FoodItemEntity
import com.defat.core.data.db.entity.FoodModifierEntity
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.FoodSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class FoodMappersTest {

    private val item = FoodItem(
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

    @Test
    fun `a food item round-trips and keeps unknown macros null`() {
        val back = item.toEntity().toDomain()
        assertEquals(item, back)
        assertNull(back.proteinG)
    }

    @Test
    fun `an unknown enum name falls back rather than throwing`() {
        val entity: FoodItemEntity = item.toEntity().copy(source = "FUTURE", confidence = "FUTURE", category = "FUTURE")
        val back = entity.toDomain()
        assertEquals(FoodSource.MEDIA, back.source)
        assertEquals(FoodConfidence.LOW, back.confidence)
        assertEquals(FoodCategory.SNACK, back.category)
    }

    @Test
    fun `appliesTo splits into categories and item ids`() {
        val entity = FoodModifierEntity(
            id = "sauce_none",
            nameZh = "走汁",
            nameEn = "No sauce",
            groupId = "sauce",
            appliesTo = "ROAST_RICE,NOODLE,cha_siu_rice",
            kcalDelta = -100.0,
            proteinDelta = null,
            carbsDelta = null,
            fatDelta = null,
            source = "COMPOSITION_TABLE",
            confidence = "MEDIUM",
            editedByOwner = false,
        )
        val modifier = entity.toDomain()

        assertEquals(setOf(FoodCategory.ROAST_RICE, FoodCategory.NOODLE), modifier.appliesTo)
        assertEquals(setOf("cha_siu_rice"), modifier.appliesToItemIds)
        assertEquals(-100.0, modifier.kcalDelta)
        assertNull(modifier.proteinDelta)
    }

    @Test
    fun `a modifier round-trips and its stored appliesTo is stable`() {
        val modifier = FoodModifier(
            id = "skin_off",
            nameZh = "去皮",
            nameEn = "No skin",
            groupId = "skin",
            appliesTo = setOf(FoodCategory.NOODLE, FoodCategory.ROAST_RICE),
            appliesToItemIds = setOf("roast_goose_rice", "soy_sauce_chicken_rice"),
            kcalDelta = -90.0,
            proteinDelta = -3.0,
            carbsDelta = null,
            fatDelta = -9.0,
            source = FoodSource.TRAINER,
            confidence = FoodConfidence.HIGH,
            editedByOwner = true,
        )
        val entity = modifier.toEntity()

        assertEquals("ROAST_RICE,NOODLE,roast_goose_rice,soy_sauce_chicken_rice", entity.appliesTo)
        assertEquals(modifier, entity.toDomain())
    }
}

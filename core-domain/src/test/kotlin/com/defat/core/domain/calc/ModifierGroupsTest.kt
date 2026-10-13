package com.defat.core.domain.calc

import com.defat.core.domain.fake.chaSiuRice
import com.defat.core.domain.fake.foodModifier
import com.defat.core.domain.fake.leanMeat
import com.defat.core.domain.fake.lessRice
import com.defat.core.domain.fake.lessSauce
import com.defat.core.domain.fake.moreRice
import com.defat.core.domain.fake.noSauce
import com.defat.core.domain.fake.noSugar
import com.defat.core.domain.model.FoodCategory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ModifierGroupsTest {

    private val item = chaSiuRice()
    private val library = listOf(noSauce, lessSauce, moreRice, lessRice, leanMeat, noSugar)

    @Test
    fun `only applicable modifiers are offered, grouped in docs07 order`() {
        val groups = ModifierGroups.forItem(item, library)
        // 飯量, 肉款, 醬汁 — 走糖 is a DRINK modifier and is not offered.
        assertEquals(listOf("rice_amount", "meat_cut", "sauce"), groups.map { it.groupId })
    }

    @Test
    fun `options run from the most negative delta to the most positive`() {
        val rice = ModifierGroups.forItem(item, library).first { it.groupId == "rice_amount" }
        assertEquals(listOf("rice_less", "rice_more"), rice.options.map { it.id })
        assertEquals(1, rice.defaultIndex) // 少飯 · 正常 · 加飯

        val sauce = ModifierGroups.forItem(item, library).first { it.groupId == "sauce" }
        assertEquals(listOf("sauce_none", "sauce_less"), sauce.options.map { it.id })
        assertEquals(2, sauce.defaultIndex) // 走汁 · 少汁 · 正常
    }

    @Test
    fun `a group of only additions has its default first`() {
        val add = foodModifier("pb", "peanut_butter", 95.0)
        val group = ModifierGroups.forItem(item, listOf(add)).single()
        assertEquals(0, group.defaultIndex) // 無 · 加花生醬
    }

    @Test
    fun `an unknown group sorts after the known ones`() {
        val odd = foodModifier("odd", "zzz_new_group", -10.0)
        val groups = ModifierGroups.forItem(item, library + odd)
        assertEquals("zzz_new_group", groups.last().groupId)
    }

    @Test
    fun `dim sum always needs the adjust step, others only when they have modifiers`() {
        val dimSum = item.copy(id = "har_gow", category = FoodCategory.DIM_SUM)
        assertTrue(ModifierGroups.needsAdjustStep(dimSum, emptyList()))
        assertTrue(ModifierGroups.needsAdjustStep(item, library))
        assertFalse(ModifierGroups.needsAdjustStep(item.copy(category = FoodCategory.FAST_FOOD), library))
    }

    @Test
    fun `resolve drops unknown ids, inapplicable modifiers and a second option of one group`() {
        val resolved = ModifierGroups.resolve(
            item,
            library,
            listOf("meat_lean", "gone", "sugar_none", "sauce_none", "sauce_less"),
        )
        assertEquals(listOf("meat_lean", "sauce_none"), resolved.map { it.id })
    }
}

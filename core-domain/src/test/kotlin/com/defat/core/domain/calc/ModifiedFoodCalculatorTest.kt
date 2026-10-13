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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

/** Plan §15.4a. Base 叉燒飯: 939 kcal, 42 g P, 118 g C, 30 g F, ROAST_RICE. */
class ModifiedFoodCalculatorTest {

    private val base = chaSiuRice()

    @Test
    fun `no modifiers leaves the base exactly as stored`() {
        val totals = ModifiedFoodCalculator.apply(base, emptyList())
        assertEquals(939.0, totals.kcal, 1e-9)
        assertEquals(42.0, totals.proteinG!!, 1e-9)
        assertEquals(118.0, totals.carbsG!!, 1e-9)
        assertEquals(30.0, totals.fatG!!, 1e-9)
    }

    @Test
    fun `lean meat takes 120 off`() {
        assertEquals(819.0, ModifiedFoodCalculator.apply(base, listOf(leanMeat)).kcal, 1e-9)
    }

    @Test
    fun `lean, less rice and no sauce give 589`() {
        val totals = ModifiedFoodCalculator.apply(base, listOf(leanMeat, lessRice, noSauce))
        assertEquals(589.0, totals.kcal, 1e-9)
    }

    @Test
    fun `quantity multiplies after the modifiers`() {
        val totals = ModifiedFoodCalculator.apply(base, listOf(noSauce), quantity = 2)
        assertEquals(1678.0, totals.kcal, 1e-9) // (939 - 100) x 2
    }

    @Test
    fun `two options from the same group throw`() {
        assertFailsWith<IllegalArgumentException> {
            ModifiedFoodCalculator.apply(base, listOf(noSauce, lessSauce))
        }
        assertFailsWith<IllegalArgumentException> {
            ModifiedFoodCalculator.apply(base, listOf(lessRice, moreRice))
        }
    }

    @Test
    fun `a modifier that does not apply to the category throws`() {
        // 走糖 is for DRINK; it cannot be attached to 叉燒飯 (ROAST_RICE).
        assertFailsWith<IllegalArgumentException> {
            ModifiedFoodCalculator.apply(base, listOf(noSugar))
        }
    }

    @Test
    fun `an item-level modifier applies only to the items it names`() {
        val itemOnly = foodModifier(
            id = "skin_off",
            groupId = "skin",
            kcalDelta = -90.0,
            appliesTo = emptySet(),
            appliesToItemIds = setOf("roast_goose_rice"),
        )
        // Same category as the goose rice, but not the named item.
        assertFailsWith<IllegalArgumentException> {
            ModifiedFoodCalculator.apply(base, listOf(itemOnly))
        }
        val goose = base.copy(id = "roast_goose_rice")
        assertEquals(849.0, ModifiedFoodCalculator.apply(goose, listOf(itemOnly)).kcal, 1e-9)
    }

    @Test
    fun `quantity zero throws`() {
        assertFailsWith<IllegalArgumentException> {
            ModifiedFoodCalculator.apply(base, emptyList(), quantity = 0)
        }
    }

    @Test
    fun `a null protein delta makes the protein null, not 42`() {
        val withProtein = foodModifier("sauce_none", "sauce", -100.0, proteinDelta = null, carbsDelta = -5.0, fatDelta = -8.0)
        val totals = ModifiedFoodCalculator.apply(base, listOf(withProtein))

        assertNull(totals.proteinG)
        // The macros that did have a delta are still computed.
        assertEquals(113.0, totals.carbsG!!, 1e-9)
        assertEquals(22.0, totals.fatG!!, 1e-9)
        assertEquals(839.0, totals.kcal, 1e-9)
    }

    @Test
    fun `a base with an unpublished macro keeps it unknown`() {
        val kcalOnly = chaSiuRice(proteinG = null, carbsG = null, fatG = null)
        val totals = ModifiedFoodCalculator.apply(kcalOnly, listOf(leanMeat))
        assertEquals(819.0, totals.kcal, 1e-9)
        assertNull(totals.proteinG)
        assertNull(totals.carbsG)
        assertNull(totals.fatG)
    }

    @Test
    fun `the result is clamped at zero and never negative`() {
        val light = chaSiuRice(kcal = 120.0, proteinG = 5.0, carbsG = 10.0, fatG = 2.0)
        val big = foodModifier("sauce_none", "sauce", -200.0, proteinDelta = -9.0, carbsDelta = -1.0, fatDelta = -3.0)

        val breakdown = ModifiedFoodCalculator.breakdown(light, listOf(big))

        assertEquals(0.0, breakdown.totals.kcal, 0.0)
        assertEquals(0.0, breakdown.totals.proteinG!!, 0.0)
        assertEquals(9.0, breakdown.totals.carbsG!!, 1e-9)
        assertEquals(0.0, breakdown.totals.fatG!!, 0.0)
        assertTrue(breakdown.clampedAtZero)
        assertEquals(-80.0, breakdown.unclampedUnitKcal, 1e-9)
    }

    @Test
    fun `order does not matter`() {
        val modifiers = listOf(leanMeat, lessRice, noSauce)
        val permutations = listOf(
            listOf(leanMeat, lessRice, noSauce),
            listOf(leanMeat, noSauce, lessRice),
            listOf(lessRice, leanMeat, noSauce),
            listOf(lessRice, noSauce, leanMeat),
            listOf(noSauce, leanMeat, lessRice),
            listOf(noSauce, lessRice, leanMeat),
        )
        assertEquals(modifiers.toSet(), permutations.first().toSet())
        permutations.forEach { order ->
            assertEquals(589.0, ModifiedFoodCalculator.apply(base, order).kcal, 1e-9, "order $order")
        }
    }

    @Test
    fun `the breakdown carries the lines the screen shows`() {
        val breakdown = ModifiedFoodCalculator.breakdown(base, listOf(leanMeat, lessRice, noSauce), quantity = 2)

        assertEquals(939.0, breakdown.base.kcal, 1e-9)
        assertEquals(listOf(-120.0, -130.0, -100.0), breakdown.modifiers.map { it.kcalDelta })
        assertEquals(589.0, breakdown.unclampedUnitKcal, 1e-9)
        assertEquals(589.0, breakdown.perUnit.kcal, 1e-9)
        assertEquals(1178.0, breakdown.totals.kcal, 1e-9)
        assertEquals(FoodCategory.ROAST_RICE, breakdown.base.category)
    }
}

package com.defat.core.domain.calc

import com.defat.core.domain.fake.chaSiuRice
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class FoodSearchTest {

    private val chaSiu = chaSiuRice()
    private val milkTea = chaSiuRice().copy(
        id = "milk_tea",
        nameZh = "港式奶茶",
        nameEn = "HK-style milk tea",
        category = FoodCategory.DRINK,
    )
    private val bigMac = chaSiuRice().copy(
        id = "big_mac",
        nameZh = "巨無霸",
        nameEn = "Big Mac",
        category = FoodCategory.FAST_FOOD,
    )
    private val all = listOf(chaSiu, milkTea, bigMac)

    @Test
    fun `a blank query matches everything and keeps the order`() {
        assertEquals(all, FoodSearch.filter(all, "", null))
        assertEquals(all, FoodSearch.filter(all, "   ", null))
    }

    @Test
    fun `Chinese names match by substring`() {
        assertEquals(listOf(milkTea), FoodSearch.filter(all, "奶茶", null))
        assertEquals(listOf(chaSiu), FoodSearch.filter(all, "燒飯", null))
    }

    @Test
    fun `English names match by case-insensitive substring`() {
        assertEquals(listOf(bigMac), FoodSearch.filter(all, "big mac", null))
        assertEquals(listOf(bigMac), FoodSearch.filter(all, "BIG", null))
        assertEquals(listOf(milkTea), FoodSearch.filter(all, "milk", null))
    }

    @Test
    fun `there is no fuzzy matching`() {
        assertEquals(emptyList(), FoodSearch.filter(all, "milkk", null))
        assertEquals(emptyList(), FoodSearch.filter(all, "奶 茶", null))
    }

    @Test
    fun `a category narrows the results and combines with the query`() {
        assertEquals(listOf(milkTea), FoodSearch.filter(all, "", FoodCategory.DRINK))
        assertEquals(emptyList(), FoodSearch.filter(all, "big", FoodCategory.DRINK))
        assertEquals(listOf(bigMac), FoodSearch.filter(all, "big", FoodCategory.FAST_FOOD))
    }

    @Test
    fun `a row without a range pre-fills its own number`() {
        assertEquals(939.0, FoodSearch.prefillKcal(chaSiu), 0.0)
    }

    @Test
    fun `a row with an unexplained range pre-fills the midpoint rounded to 10`() {
        val range = chaSiu.copy(kcal = 910.0, kcalLow = 850.0, kcalHigh = 970.0)
        assertEquals(910.0, FoodSearch.prefillKcal(range), 0.0)

        val odd = chaSiu.copy(kcal = 1259.5, kcalLow = 1235.0, kcalHigh = 1284.0)
        assertEquals(1260.0, FoodSearch.prefillKcal(odd), 0.0) // 1259.5 -> 1260

        val rounding = chaSiu.copy(kcal = 188.5, kcalLow = 137.0, kcalHigh = 240.0)
        assertEquals(190.0, FoodSearch.prefillKcal(rounding), 0.0) // 188.5 -> 190
    }

    @Test
    fun `hasUnexplainedRange needs both bounds`() {
        assertEquals(false, FoodSearch.hasUnexplainedRange(chaSiu))
        assertEquals(true, FoodSearch.hasUnexplainedRange(chaSiu.copy(kcalLow = 1.0, kcalHigh = 2.0)))
        assertEquals(false, FoodSearch.hasUnexplainedRange(chaSiu.copy(kcalLow = 1.0)))
    }

    @Test
    fun `badges follow docs07 section 3`() {
        assertNull(FoodBadges.forSource(FoodSource.OFFICIAL))
        assertEquals(FoodBadge.APPROX, FoodBadges.forSource(FoodSource.COMPOSITION_TABLE))
        assertEquals(FoodBadge.ESTIMATE, FoodBadges.forSource(FoodSource.DIETITIAN))
        assertEquals(FoodBadge.ESTIMATE, FoodBadges.forSource(FoodSource.MEDIA))
        assertEquals(FoodBadge.TRAINER, FoodBadges.forSource(FoodSource.TRAINER))
        assertEquals(FoodBadge.CUSTOM, FoodBadges.forSource(FoodSource.USER))
    }
}

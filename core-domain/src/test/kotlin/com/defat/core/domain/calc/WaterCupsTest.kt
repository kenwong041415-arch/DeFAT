package com.defat.core.domain.calc

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class WaterCupsTest {

    @Test
    fun `tapping an empty cup fills up to it`() {
        assertEquals(750, WaterCups.tap(0, 3))
        assertEquals(1000, WaterCups.tap(500, 4))
    }

    @Test
    fun `tapping the last filled cup removes one`() {
        assertEquals(500, WaterCups.tap(750, 3))
        assertEquals(0, WaterCups.tap(250, 1))
    }

    @Test
    fun `tapping an earlier filled cup fills only up to it`() {
        assertEquals(500, WaterCups.tap(1500, 2))
    }

    @Test
    fun `a custom amount is carried through a cup tap`() {
        // 600 ml = 2 cups + 100 ml extra.
        assertEquals(2, WaterCups.filledCups(600))
        assertEquals(100, WaterCups.extraMl(600))
        assertEquals(850, WaterCups.tap(600, 3))
        assertEquals(350, WaterCups.tap(600, 1))
    }

    @Test
    fun `intake beyond eight cups keeps its extra`() {
        assertEquals(8, WaterCups.filledCups(2400))
        assertEquals(400, WaterCups.extraMl(2400))
        assertEquals(2150, WaterCups.tap(2400, 8))
    }

    @Test
    fun `adding a custom amount`() {
        assertEquals(1750, WaterCups.add(1500, 250))
        assertEquals(WaterCups.MAX_DAILY_ML, WaterCups.add(9900, 500))
        assertFailsWith<IllegalArgumentException> { WaterCups.add(0, 0) }
    }

    @Test
    fun `a cup outside 1 to 8 throws`() {
        assertFailsWith<IllegalArgumentException> { WaterCups.tap(0, 0) }
        assertFailsWith<IllegalArgumentException> { WaterCups.tap(0, 9) }
    }
}

package com.defat.core.domain.calc

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class WaterGoalCalculatorTest {

    @Test
    fun `80 kg gives 2800`() = assertEquals(2800, WaterGoalCalculator.goalMl(80.0))

    @Test
    fun `62 kg gives 2100 - 2170 rounded down to the hundred`() =
        assertEquals(2100, WaterGoalCalculator.goalMl(62.0))

    @Test
    fun `40 kg is floored at 1500`() = assertEquals(1500, WaterGoalCalculator.goalMl(40.0))

    @Test
    fun `0 kg throws`() {
        assertFailsWith<IllegalArgumentException> { WaterGoalCalculator.goalMl(0.0) }
    }

    @Test
    fun `a goal is always a whole hundred and never below the floor`() {
        for (kg in 30..200) {
            val goal = WaterGoalCalculator.goalMl(kg.toDouble())
            assertEquals(0, goal % 100, "$kg kg")
            assertEquals(true, goal >= 1500, "$kg kg")
        }
    }
}

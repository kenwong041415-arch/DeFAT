package com.defat.core.domain.calc

import kotlin.test.assertEquals
import org.junit.Test

class MetCalculatorTest {

    @Test
    fun `walking one hour at 70 kg`() {
        assertEquals(245.0, MetCalculator.kcal(met = 3.5, weightKg = 70.0, hours = 1.0), 1e-6)
    }

    @Test
    fun `jogging half an hour at 70 kg`() {
        assertEquals(245.0, MetCalculator.kcal(met = 7.0, weightKg = 70.0, hours = 0.5), 1e-6)
    }

    @Test
    fun `MET table matches docs04`() {
        val expected = mapOf(
            "walking" to 3.5,
            "jogging" to 7.0,
            "cycling_moderate" to 6.8,
            "swimming" to 6.0,
            "weight_training" to 5.0,
            "hiit" to 8.0,
            "hiking" to 6.0,
            "badminton" to 5.5,
            "basketball" to 6.5,
            "yoga" to 2.5,
        )
        assertEquals(expected.keys.toList(), MetTable.ACTIVITIES.map { it.key })
        expected.forEach { (key, met) ->
            assertEquals(met, MetTable.metFor(key)!!, 1e-9)
        }
    }
}

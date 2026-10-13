package com.defat.core.domain.calc

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test

class ExerciseFormulaTest {

    @Test
    fun `45 minutes of medium weight training at 80 kg`() {
        val preview = ExerciseFormula.preview(met = 5.0, weightKg = 80.0, minutes = 45)
        assertEquals(0.75, preview.hours, 1e-9)
        assertEquals(300.0, preview.kcal, 1e-9) // 5 x 80 x 0.75
    }

    @Test
    fun `the three weight training levels`() {
        assertEquals(
            listOf(3.5, 5.0, 6.0),
            MetTable.WEIGHT_TRAINING_LEVELS.map { it.met },
        )
        // 中 is the existing table row, so MetTable.ACTIVITIES is unchanged.
        assertEquals(5.0, MetTable.metFor(MetTable.WEIGHT_TRAINING_KEY)!!, 1e-9)
    }

    @Test
    fun `minutes outside 1 to 1440 are rejected`() {
        assertFailsWith<IllegalArgumentException> { ExerciseFormula.preview(5.0, 80.0, 0) }
        assertFailsWith<IllegalArgumentException> { ExerciseFormula.preview(5.0, 80.0, 1441) }
    }

    @Test
    fun `newEntry stores the computed kcal and the weight it used`() {
        val entry = ExerciseFormula.newEntry(
            id = "e1",
            loggedAt = Instant.parse("2026-10-13T06:00:00Z"),
            activityKey = "jogging",
            met = 7.0,
            minutes = 30,
            weightKg = 70.0,
        )
        assertEquals(245.0, entry.kcal, 1e-9)
        assertEquals(70.0, entry.weightKgAtTime, 1e-9)
        assertEquals(30, entry.minutes)
        assertNull(entry.note)
    }
}

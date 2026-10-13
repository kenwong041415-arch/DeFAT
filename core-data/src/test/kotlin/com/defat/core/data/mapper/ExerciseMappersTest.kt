package com.defat.core.data.mapper

import com.defat.core.domain.model.ExerciseEntry
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ExerciseMappersTest {

    @Test
    fun `an exercise entry round-trips with its stored kcal and the weight it used`() {
        val entry = ExerciseEntry(
            id = "e1",
            loggedAt = Instant.parse("2026-10-13T06:00:00Z"),
            activityKey = "weight_training_heavy",
            met = 6.0,
            minutes = 45,
            weightKgAtTime = 80.0,
            kcal = 360.0,
            note = null,
        )
        val entity = entry.toEntity()

        assertEquals(1_791_871_200_000L, entity.loggedAtMillis)
        assertEquals(360.0, entity.kcal)
        assertEquals(80.0, entity.weightKgAtTime)
        assertNull(entity.note)
        assertEquals(entry, entity.toDomain())
    }
}

package com.defat.core.domain.calc

import com.defat.core.domain.model.ExerciseEntry
import java.time.Instant

/** The numbers behind `MET x 體重(kg) x 小時 = kcal`, ready to display (plan §10.4). */
data class ExercisePreview(
    val met: Double,
    val weightKg: Double,
    val minutes: Int,
    val hours: Double,
    val kcal: Double,
)

object ExerciseFormula {

    const val MIN_MINUTES: Int = 1
    const val MAX_MINUTES: Int = 24 * 60

    private const val MINUTES_PER_HOUR = 60.0

    fun isValidMinutes(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES

    fun preview(met: Double, weightKg: Double, minutes: Int): ExercisePreview {
        require(isValidMinutes(minutes)) { "minutes must be in $MIN_MINUTES..$MAX_MINUTES, was $minutes" }
        val hours = minutes / MINUTES_PER_HOUR
        return ExercisePreview(
            met = met,
            weightKg = weightKg,
            minutes = minutes,
            hours = hours,
            kcal = MetCalculator.kcal(met, weightKg, hours),
        )
    }

    /** Builds the entry that gets stored, with [ExerciseEntry.kcal] computed once, here. */
    fun newEntry(
        id: String,
        loggedAt: Instant,
        activityKey: String,
        met: Double,
        minutes: Int,
        weightKg: Double,
        note: String? = null,
    ): ExerciseEntry {
        val preview = preview(met, weightKg, minutes)
        return ExerciseEntry(
            id = id,
            loggedAt = loggedAt,
            activityKey = activityKey,
            met = met,
            minutes = minutes,
            weightKgAtTime = weightKg,
            kcal = preview.kcal,
            note = note,
        )
    }
}

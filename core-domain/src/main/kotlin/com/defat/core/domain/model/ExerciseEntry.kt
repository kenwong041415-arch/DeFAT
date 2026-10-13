package com.defat.core.domain.model

import java.time.Instant

/**
 * One manually logged session (Phase 1.7 plan §10.2).
 *
 * [kcal] is computed once from [met] x [weightKgAtTime] x hours and stored, so
 * a later weight change never rewrites an earlier day.
 */
data class ExerciseEntry(
    val id: String,
    val loggedAt: Instant,
    val activityKey: String,
    val met: Double,
    val minutes: Int,
    val weightKgAtTime: Double,
    val kcal: Double,
    val note: String? = null,
)

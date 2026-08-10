package com.defat.core.domain.model

import java.time.Instant
import java.time.LocalDate

data class WeightEntry(
    val date: LocalDate,
    val weightKg: Double,
    val bodyFatPct: Double?,
    val recordedAt: Instant,
    val source: MeasurementSource = MeasurementSource.MANUAL,
)

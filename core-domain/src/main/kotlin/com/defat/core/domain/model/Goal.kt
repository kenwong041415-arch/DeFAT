package com.defat.core.domain.model

import java.time.LocalDate

data class Goal(
    val targetWeightKg: Double,
    val targetBodyFatPct: Double? = null,
    val targetDate: LocalDate? = null,
)

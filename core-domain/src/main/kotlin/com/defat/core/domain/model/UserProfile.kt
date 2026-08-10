package com.defat.core.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.Period

data class UserProfile(
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPct: Double?, // null = unknown -> Mifflin path
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val disclaimerAcceptedAt: Instant?,
) {
    fun ageYears(on: LocalDate): Int = Period.between(birthDate, on).years
}

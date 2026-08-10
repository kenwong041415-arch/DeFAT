package com.defat.core.domain.model

import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

class UserProfileTest {

    private fun profile(birthDate: LocalDate) = UserProfile(
        sex = Sex.MALE,
        birthDate = birthDate,
        heightCm = 175.0,
        weightKg = 80.0,
        bodyFatPct = null,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = null,
    )

    @Test
    fun `age is 29 the day before the birthday`() {
        val p = profile(LocalDate.of(1996, 3, 15))
        assertEquals(29, p.ageYears(LocalDate.of(2026, 3, 14)))
    }

    @Test
    fun `age is 30 on the birthday`() {
        val p = profile(LocalDate.of(1996, 3, 15))
        assertEquals(30, p.ageYears(LocalDate.of(2026, 3, 15)))
    }
}

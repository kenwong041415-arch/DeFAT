package com.defat.core.domain.usecase

import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.TdeeSource
import com.defat.core.domain.model.UserProfile
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class ComputeDailyTargetUseCaseTest {

    private val useCase = ComputeDailyTargetUseCase()
    private val on = LocalDate.of(2026, 6, 15)

    private fun profile(bodyFatPct: Double?) = UserProfile(
        sex = Sex.MALE,
        birthDate = on.minusYears(30),
        heightCm = 175.0,
        weightKg = 80.0,
        bodyFatPct = bodyFatPct,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = null,
    )

    @Test
    fun `composes BMR TDEE target and macros for the golden profile`() {
        val target = useCase(profile(bodyFatPct = 25.0), on)

        assertEquals(1666.0, target.bmrKcal, 1e-6)
        assertEquals(BmrMethod.KATCH_MCARDLE, target.bmrMethod)
        assertEquals(2582.3, target.tdeeKcal, 1e-6)
        assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, target.tdeeSource)
        assertEquals(2065.84, target.targetKcal, 1e-6)
        assertFalse(target.floorApplied)
        assertEquals(132.0, target.macros.proteinG, 1e-6)
        assertEquals(48.0, target.macros.fatG, 1e-6)
        assertEquals(276.46, target.macros.carbsG, 1e-6)

        assertEquals(2066, target.targetKcal.roundKcal())
        assertEquals(276, target.macros.carbsG.roundGrams())
    }

    @Test
    fun `passing activeKcal switches to the wearable TDEE source`() {
        val target = useCase(profile(bodyFatPct = 25.0), on, activeKcal = 500.0)
        assertEquals(TdeeSource.WEARABLE, target.tdeeSource)
        assertEquals(1666.0 * 1.1 + 500.0, target.tdeeKcal, 1e-6)
    }

    @Test
    fun `falls back to Henry when body fat is unknown`() {
        // Age 30 on `on` -> Henry's 30-60 male band: 11.4*80 + 5.41*175 - 137.
        val target = useCase(profile(bodyFatPct = null), on)
        assertEquals(BmrMethod.HENRY_OXFORD, target.bmrMethod)
        assertEquals(1721.75, target.bmrKcal, 1e-6)
    }
}

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

    // ---- Phase 1.7 PR B: plan §10.1 / §15.3 / R3 ----

    @Test
    fun `zero exercise is byte-identical to the target before exercise existed`() {
        for (bodyFat in listOf(25.0, null)) {
            val p = profile(bodyFatPct = bodyFat)
            val before = useCase(p, on)
            val after = useCase(p, on, exerciseKcal = 0.0)

            assertEquals(before, after)
            assertEquals(before.tdeeKcal.toRawBits(), after.tdeeKcal.toRawBits())
            assertEquals(before.targetKcal.toRawBits(), after.targetKcal.toRawBits())
            assertEquals(TdeeSource.ACTIVITY_MULTIPLIER, after.tdeeSource)
        }
        // And the literal numbers the golden profile has always had.
        val golden = useCase(profile(bodyFatPct = 25.0), on, exerciseKcal = 0.0)
        assertEquals(2582.3, golden.tdeeKcal, 1e-9)
        assertEquals(2065.84, golden.targetKcal, 1e-9)
    }

    @Test
    fun `a 45 minute session on a moderate profile does not double count`() {
        // BMR 1666, 45 min of weight training at 5 MET, 80 kg = 300 kcal.
        val target = useCase(profile(bodyFatPct = 25.0), on, exerciseKcal = 300.0)
        assertEquals(2582.3, target.tdeeKcal, 1e-6) // not 2582.3 + 300
        assertEquals(2065.84, target.targetKcal, 1e-6)
    }

    @Test
    fun `a big session raises the target by 80 percent of the part the multiplier did not assume`() {
        val target = useCase(profile(bodyFatPct = 25.0), on, exerciseKcal = 1500.0)
        assertEquals(TdeeSource.MANUAL_EXERCISE, target.tdeeSource)
        assertEquals(1666.0 * 1.1 + 1500.0, target.tdeeKcal, 1e-6)
        assertEquals((1666.0 * 1.1 + 1500.0) * 0.8, target.targetKcal, 1e-6)
    }
}

package com.defat.core.domain.usecase

import com.defat.core.domain.calc.RateAssessment
import com.defat.core.domain.calc.WeightRateCalculator
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

/**
 * The golden profile (§9.9 of the Phase 1 plan) is the number the owner
 * will eyeball in the exit test: male, 80 kg, 175 cm, 30 years old, 25%
 * body fat, MODERATE activity.
 *
 * A regression anywhere in BMR -> TDEE -> target -> macros -> weight-rate
 * fails this one obvious test.
 */
class GoldenProfileEndToEndTest {

    private val computeDailyTarget = ComputeDailyTargetUseCase()
    private val on = LocalDate.of(2026, 6, 15)

    private fun goldenProfile(bodyFatPct: Double?) = UserProfile(
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
    fun `golden profile with known body fat — full Katch-McArdle chain`() {
        val target = computeDailyTarget(goldenProfile(bodyFatPct = 25.0), on)

        // BMR (Katch-McArdle)
        assertEquals(1666.0, target.bmrKcal, 1e-6)
        assertEquals(BmrMethod.KATCH_MCARDLE, target.bmrMethod)

        // TDEE (x1.55)
        assertEquals(2582.3, target.tdeeKcal, 1e-6)

        // Target (-20%) -> displayed 2066 kcal
        assertEquals(2065.84, target.targetKcal, 1e-6)
        assertEquals(2066, target.targetKcal.roundKcal())

        // Protein (2.2 x 60 LBM)
        assertEquals(132.0, target.macros.proteinG, 1e-6)

        // Fat (0.6 x 80)
        assertEquals(48.0, target.macros.fatG, 1e-6)

        // Carbs -> displayed 276 g
        assertEquals(276.46, target.macros.carbsG, 1e-6)
        assertEquals(276, target.macros.carbsG.roundGrams())

        // Implied daily deficit
        val dailyDeficit = target.tdeeKcal - target.targetKcal
        assertEquals(516.46, dailyDeficit, 1e-6)

        // Projected weekly loss ~= 0.47 kg = 0.59% bodyweight -> SAFE
        val weeklyLossKg = WeightRateCalculator.projectedWeeklyLossKg(dailyDeficit)
        assertEquals(0.4695, weeklyLossKg, 1e-3)
        val pct = WeightRateCalculator.weeklyRatePctOfBodyweight(-weeklyLossKg, goldenProfile(25.0).weightKg)
        assertEquals(0.59, pct, 1e-2)
        assertEquals(
            RateAssessment.SAFE,
            WeightRateCalculator.assess(-weeklyLossKg, goldenProfile(25.0).weightKg),
        )
    }

    @Test
    fun `golden profile with unknown body fat — full Mifflin chain`() {
        val target = computeDailyTarget(goldenProfile(bodyFatPct = null), on)

        // BMR (Mifflin-St Jeor) — see D4: docs/04 corrected to 1748.75
        assertEquals(1748.75, target.bmrKcal, 1e-6)
        assertEquals(BmrMethod.MIFFLIN_ST_JEOR, target.bmrMethod)

        // TDEE (x1.55)
        assertEquals(2710.5625, target.tdeeKcal, 1e-6)

        // Target (-20%)
        assertEquals(2168.45, target.targetKcal, 1e-6)

        // Protein (1.8 x 80 kg bodyweight)
        assertEquals(144.0, target.macros.proteinG, 1e-6)

        // Fat (0.6 x 80)
        assertEquals(48.0, target.macros.fatG, 1e-6)

        // Carbs
        assertEquals(290.1125, target.macros.carbsG, 1e-6)
    }
}

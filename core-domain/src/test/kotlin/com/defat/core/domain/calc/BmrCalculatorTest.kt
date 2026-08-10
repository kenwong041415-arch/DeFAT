package com.defat.core.domain.calc

import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.model.ActivityLevel
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class BmrCalculatorTest {

    @Test
    fun `katch-McArdle reference vector`() {
        val lbm = BmrCalculator.leanBodyMassKg(weightKg = 80.0, bodyFatPct = 25.0)
        assertEquals(60.0, lbm, 1e-6)
        val bmr = BmrCalculator.katchMcArdle(weightKg = 80.0, bodyFatPct = 25.0)
        assertEquals(1666.0, bmr, 1e-6)
    }

    @Test
    fun `mifflin male reference vector`() {
        // D4: docs/04 printed 1780 kcal for this example, which is an
        // arithmetic error. The formula itself gives:
        //   10*80 + 6.25*175 - 5*30 + 5 = 800 + 1093.75 - 150 + 5 = 1748.75
        val bmr = BmrCalculator.mifflinStJeor(
            weightKg = 80.0,
            heightCm = 175.0,
            ageYears = 30,
            sex = Sex.MALE,
        )
        assertEquals(1748.75, bmr, 1e-6)
    }

    @Test
    fun `mifflin female reference vector`() {
        val bmr = BmrCalculator.mifflinStJeor(
            weightKg = 60.0,
            heightCm = 162.0,
            ageYears = 28,
            sex = Sex.FEMALE,
        )
        assertEquals(1311.5, bmr, 1e-6)
    }

    @Test
    fun `bmr() prefers Katch-McArdle when body fat is known`() {
        val profile = goldenProfile(bodyFatPct = 25.0)
        val result = BmrCalculator.bmr(profile, LocalDate.of(2026, 1, 1))
        assertEquals(BmrMethod.KATCH_MCARDLE, result.method)
    }

    @Test
    fun `bmr() falls back to Mifflin when body fat is null`() {
        val profile = goldenProfile(bodyFatPct = null)
        val result = BmrCalculator.bmr(profile, LocalDate.of(2026, 1, 1))
        assertEquals(BmrMethod.MIFFLIN_ST_JEOR, result.method)
    }

    @Test
    fun `rejects non-positive weight`() {
        assertFailsWith<IllegalArgumentException> {
            BmrCalculator.mifflinStJeor(weightKg = 0.0, heightCm = 175.0, ageYears = 30, sex = Sex.MALE)
        }
    }

    @Test
    fun `rejects body fat outside 0 to 75`() {
        assertFailsWith<IllegalArgumentException> {
            BmrCalculator.katchMcArdle(weightKg = 80.0, bodyFatPct = 80.0)
        }
        assertFailsWith<IllegalArgumentException> {
            BmrCalculator.katchMcArdle(weightKg = 80.0, bodyFatPct = -1.0)
        }
    }

    private fun goldenProfile(bodyFatPct: Double?): UserProfile = UserProfile(
        sex = Sex.MALE,
        birthDate = LocalDate.of(1996, 1, 1),
        heightCm = 175.0,
        weightKg = 80.0,
        bodyFatPct = bodyFatPct,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal(targetWeightKg = 75.0),
        disclaimerAcceptedAt = null,
    )
}

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
    fun `bmr() falls back to Henry when body fat is null`() {
        val profile = goldenProfile(bodyFatPct = null)
        val result = BmrCalculator.bmr(profile, LocalDate.of(2026, 1, 1))
        assertEquals(BmrMethod.HENRY_OXFORD, result.method)
    }

    // --- Henry (2005) "Oxford" — docs/04 reference vectors -----------------

    @Test
    fun `henry male 18 to 30 reference vector`() {
        // 14.4*80 + 3.13*175 + 113 = 1152 + 547.75 + 113
        val bmr = BmrCalculator.henryOxford(weightKg = 80.0, heightCm = 175.0, ageYears = 25, sex = Sex.MALE)
        assertEquals(1812.75, bmr, 0.01)
    }

    @Test
    fun `henry male 30 to 60 reference vector`() {
        // 11.4*80 + 5.41*175 - 137 = 912 + 946.75 - 137
        val bmr = BmrCalculator.henryOxford(weightKg = 80.0, heightCm = 175.0, ageYears = 35, sex = Sex.MALE)
        assertEquals(1721.75, bmr, 0.01)
    }

    @Test
    fun `henry male 60 plus reference vector`() {
        // 11.4*80 + 5.41*175 - 256 = 912 + 946.75 - 256
        val bmr = BmrCalculator.henryOxford(weightKg = 80.0, heightCm = 175.0, ageYears = 65, sex = Sex.MALE)
        assertEquals(1602.75, bmr, 0.01)
    }

    @Test
    fun `henry female 18 to 30 reference vector`() {
        // 10.4*60 + 6.15*162 - 282 = 624 + 996.3 - 282
        val bmr = BmrCalculator.henryOxford(weightKg = 60.0, heightCm = 162.0, ageYears = 28, sex = Sex.FEMALE)
        assertEquals(1338.3, bmr, 0.01)
    }

    @Test
    fun `henry female 30 to 60 reference vector`() {
        // 8.18*58 + 5.02*160 - 11.6 = 474.44 + 803.2 - 11.6
        val bmr = BmrCalculator.henryOxford(weightKg = 58.0, heightCm = 160.0, ageYears = 38, sex = Sex.FEMALE)
        assertEquals(1266.04, bmr, 0.01)
    }

    @Test
    fun `henry female 60 plus reference vector`() {
        // 8.52*70 + 4.21*158 + 10.7 = 596.4 + 665.18 + 10.7
        val bmr = BmrCalculator.henryOxford(weightKg = 70.0, heightCm = 158.0, ageYears = 65, sex = Sex.FEMALE)
        assertEquals(1272.28, bmr, 0.01)
    }

    @Test
    fun `henry steps down at the 30th birthday - the step is real and intentional`() {
        // docs/04 note 3: bands are discontinuous and we implement the
        // published equation exactly. Age 30 belongs to the 30-60 band.
        val at29 = BmrCalculator.henryOxford(weightKg = 80.0, heightCm = 175.0, ageYears = 29, sex = Sex.MALE)
        val at30 = BmrCalculator.henryOxford(weightKg = 80.0, heightCm = 175.0, ageYears = 30, sex = Sex.MALE)
        assertEquals(1812.75, at29, 0.01)
        assertEquals(1721.75, at30, 0.01)
    }

    @Test
    fun `henry rejects minors`() {
        assertFailsWith<IllegalArgumentException> {
            BmrCalculator.henryOxford(weightKg = 80.0, heightCm = 175.0, ageYears = 17, sex = Sex.MALE)
        }
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

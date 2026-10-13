package com.defat.core.domain.usecase

import com.defat.core.domain.fake.FakeExerciseRepo
import com.defat.core.domain.fake.FakeMealRepo
import com.defat.core.domain.fake.FakeProfileRepo
import com.defat.core.domain.fake.FakeSettingsRepo
import com.defat.core.domain.fake.testExercise
import com.defat.core.domain.fake.testMeal
import com.defat.core.domain.fake.testProfile
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveTodayDashboardUseCaseTest {

    private val zone = ZoneId.of("Asia/Hong_Kong")
    private val clock = Clock.fixed(Instant.parse("2026-10-13T06:00:00Z"), zone)

    private fun at(day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant()

    private fun useCase(
        meals: FakeMealRepo,
        settings: FakeSettingsRepo = FakeSettingsRepo(),
        profile: FakeProfileRepo = FakeProfileRepo(testProfile()),
        exercise: FakeExerciseRepo = FakeExerciseRepo(),
    ) = ObserveTodayDashboardUseCase(profile, meals, exercise, settings, ComputeDailyTargetUseCase(), clock)

    @Test
    fun `a 01-30 snack counts toward the previous day`() = runTest {
        val meals = FakeMealRepo(
            listOf(
                testMeal("late", at(day = 13, hour = 1, minute = 30), kcal = 300.0),
                testMeal("lunch", at(day = 12, hour = 12, minute = 0), kcal = 600.0),
            ),
        )
        val dashboard = useCase(meals)

        val twelfth = dashboard(LocalDate.of(2026, 10, 12)).first()
        assertNotNull(twelfth)
        assertEquals(900.0, twelfth.intakeKcal, 1e-9)
        assertEquals(2, twelfth.mealCount)

        val thirteenth = dashboard(LocalDate.of(2026, 10, 13)).first()
        assertNotNull(thirteenth)
        assertEquals(0.0, thirteenth.intakeKcal, 1e-9)
    }

    @Test
    fun `a meal exactly at the day start belongs to the new day`() = runTest {
        val meals = FakeMealRepo(listOf(testMeal("breakfast", at(day = 13, hour = 4, minute = 0), kcal = 250.0)))
        val dashboard = useCase(meals)

        assertEquals(0.0, dashboard(LocalDate.of(2026, 10, 12)).first()!!.intakeKcal, 1e-9)
        assertEquals(250.0, dashboard(LocalDate.of(2026, 10, 13)).first()!!.intakeKcal, 1e-9)
    }

    @Test
    fun `changing the day start re-buckets without moving the stored meals`() = runTest {
        val late = testMeal("late", at(day = 13, hour = 1, minute = 30), kcal = 300.0)
        val meals = FakeMealRepo(listOf(late))
        val settings = FakeSettingsRepo(LocalTime.MIDNIGHT)
        val dashboard = useCase(meals, settings)

        // With a midnight day start the 01:30 meal is on the 13th.
        assertEquals(300.0, dashboard(LocalDate.of(2026, 10, 13)).first()!!.intakeKcal, 1e-9)

        settings.setDayStart(LocalTime.of(4, 0))

        assertEquals(0.0, dashboard(LocalDate.of(2026, 10, 13)).first()!!.intakeKcal, 1e-9)
        assertEquals(300.0, dashboard(LocalDate.of(2026, 10, 12)).first()!!.intakeKcal, 1e-9)
        assertEquals(late.loggedAt, meals.mealById("late")!!.loggedAt)
    }

    @Test
    fun `no profile means no dashboard`() = runTest {
        val dashboard = useCase(FakeMealRepo(), profile = FakeProfileRepo(null))
        assertNull(dashboard(LocalDate.of(2026, 10, 13)).first())
    }

    @Test
    fun `browsing to a day before the 18th birthday does not crash the Henry path`() = runTest {
        // 19 years old on the clock's date, no body fat -> Henry path.
        val profile = FakeProfileRepo(testProfile(birthDate = LocalDate.of(2007, 6, 1), bodyFatPct = null))
        val dashboard = useCase(FakeMealRepo(), profile = profile)

        val threeYearsAgo = dashboard(LocalDate.of(2023, 10, 13)).first()
        assertNotNull(threeYearsAgo)
    }

    // ---- Phase 1.7 PR B: manually logged exercise (plan §10) ----

    @Test
    fun `a day with no exercise has exactly the multiplier target`() = runTest {
        val dashboard = useCase(FakeMealRepo())
        val rollup = dashboard(LocalDate.of(2026, 10, 12)).first()!!

        assertEquals(0.0, rollup.exerciseKcal, 0.0)
        // testProfile(): 25 % body fat, 80 kg -> Katch-McArdle BMR 1666, MODERATE x 1.55.
        assertEquals(2582.3, rollup.target.tdeeKcal, 1e-6)
        assertEquals(2065.84, rollup.target.targetKcal, 1e-6)
        assertEquals(com.defat.core.domain.model.TdeeSource.ACTIVITY_MULTIPLIER, rollup.target.tdeeSource)
    }

    @Test
    fun `a small session leaves the target alone and a big one raises it`() = runTest {
        val small = FakeExerciseRepo(listOf(testExercise("run", at(12, 18, 0), kcal = 300.0)))
        val big = FakeExerciseRepo(listOf(testExercise("long", at(12, 18, 0), kcal = 1500.0)))
        val day = LocalDate.of(2026, 10, 12)

        val withSmall = useCase(FakeMealRepo(), exercise = small)(day).first()!!
        assertEquals(300.0, withSmall.exerciseKcal, 1e-9)
        // 1666 x 1.1 + 300 = 2132.6 < 2582.3: the multiplier already assumed this.
        assertEquals(2582.3, withSmall.target.tdeeKcal, 1e-6)
        assertEquals(2065.84, withSmall.target.targetKcal, 1e-6)

        val withBig = useCase(FakeMealRepo(), exercise = big)(day).first()!!
        // 1666 x 1.1 + 1500 = 3332.6 beats 2582.3.
        assertEquals(3332.6, withBig.target.tdeeKcal, 1e-6)
        assertEquals(3332.6 * 0.8, withBig.target.targetKcal, 1e-6)
        assertEquals(com.defat.core.domain.model.TdeeSource.MANUAL_EXERCISE, withBig.target.tdeeSource)
    }

    @Test
    fun `exercise logged at 01-30 counts toward the previous day`() = runTest {
        val late = FakeExerciseRepo(listOf(testExercise("late", at(13, 1, 30), kcal = 1500.0)))
        val dashboard = useCase(FakeMealRepo(), exercise = late)

        assertEquals(1500.0, dashboard(LocalDate.of(2026, 10, 12)).first()!!.exerciseKcal, 1e-9)
        assertEquals(0.0, dashboard(LocalDate.of(2026, 10, 13)).first()!!.exerciseKcal, 1e-9)
    }
}

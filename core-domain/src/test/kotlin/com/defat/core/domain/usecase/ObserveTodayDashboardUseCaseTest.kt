package com.defat.core.domain.usecase

import com.defat.core.domain.fake.FakeMealRepo
import com.defat.core.domain.fake.FakeProfileRepo
import com.defat.core.domain.fake.FakeSettingsRepo
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
    ) = ObserveTodayDashboardUseCase(profile, meals, settings, ComputeDailyTargetUseCase(), clock)

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
}

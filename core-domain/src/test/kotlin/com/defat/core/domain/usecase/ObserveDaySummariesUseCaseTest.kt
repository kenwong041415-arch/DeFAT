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
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveDaySummariesUseCaseTest {

    private val zone = ZoneId.of("Asia/Hong_Kong")
    private val clock = Clock.fixed(Instant.parse("2026-10-13T06:00:00Z"), zone)

    private fun at(day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant()

    private fun useCase(
        meals: FakeMealRepo,
        profile: FakeProfileRepo = FakeProfileRepo(testProfile()),
        exercise: FakeExerciseRepo = FakeExerciseRepo(),
    ) = ObserveDaySummariesUseCase(profile, meals, exercise, FakeSettingsRepo(), ComputeDailyTargetUseCase(), clock)

    @Test
    fun `lists only days with meals, newest first, bucketed by logical day`() = runTest {
        val meals = FakeMealRepo(
            listOf(
                testMeal("a", at(10, 12, 0), kcal = 500.0),
                testMeal("b", at(13, 1, 30), kcal = 200.0), // logical day = 12th
                testMeal("c", at(12, 19, 0), kcal = 700.0),
            ),
        )
        val days = useCase(meals)().first()

        assertEquals(listOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 10)), days.map { it.date })
        assertEquals(900.0, days[0].intakeKcal, 1e-9)
        assertEquals(2, days[0].mealCount)
        assertEquals(500.0, days[1].intakeKcal, 1e-9)
    }

    @Test
    fun `over target is strict - exactly on target is not over`() = runTest {
        val under = FakeMealRepo(listOf(testMeal("a", at(12, 12, 0), kcal = 100.0)))
        val over = FakeMealRepo(listOf(testMeal("a", at(12, 12, 0), kcal = 9000.0)))

        assertFalse(useCase(under)().first().single().isOverTarget)
        assertTrue(useCase(over)().first().single().isOverTarget)
    }

    @Test
    fun `no profile gives an empty list`() = runTest {
        val meals = FakeMealRepo(listOf(testMeal("a", at(12, 12, 0))))
        assertTrue(useCase(meals, FakeProfileRepo(null))().first().isEmpty())
    }

    @Test
    fun `a day's target includes that day's exercise, matching Home`() = runTest {
        val meals = FakeMealRepo(
            listOf(
                testMeal("a", at(11, 12, 0), kcal = 500.0),
                testMeal("b", at(12, 12, 0), kcal = 500.0),
            ),
        )
        // 1500 kcal of exercise on the 12th only.
        val exercise = FakeExerciseRepo(listOf(testExercise("long", at(12, 18, 0), kcal = 1500.0)))

        val days = useCase(meals, exercise = exercise)().first()

        assertEquals(LocalDate.of(2026, 10, 12), days[0].date)
        assertEquals(3332.6 * 0.8, days[0].targetKcal, 1e-6)
        assertEquals(2065.84, days[1].targetKcal, 1e-6)
    }
}

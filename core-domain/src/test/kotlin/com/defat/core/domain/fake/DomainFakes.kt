package com.defat.core.domain.fake

import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.ExerciseEntry
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.repository.ExerciseRepository
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProfileRepo(initial: UserProfile?) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<UserProfile?> = state
    override val onboardingComplete: Flow<Boolean> = state.map { it != null }
    override suspend fun save(profile: UserProfile) { state.value = profile }
    override suspend fun updateCurrentBody(weightKg: Double, bodyFatPct: Double?) {
        val current = state.value ?: return
        state.value = current.copy(weightKg = weightKg, bodyFatPct = bodyFatPct)
    }
    override suspend fun clear() { state.value = null }
}

class FakeMealRepo(initial: List<Meal> = emptyList()) : MealRepository {
    private val state = MutableStateFlow(initial)

    override fun mealsBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<Meal>> =
        state.map { list ->
            list.filter { it.loggedAt >= startInclusive && it.loggedAt < endExclusive }
                .sortedBy { it.loggedAt }
        }

    override fun allMeals(): Flow<List<Meal>> = state.map { it.sortedBy { m -> m.loggedAt } }

    override fun recentMeals(limit: Int): Flow<List<Meal>> =
        state.map { it.sortedByDescending { m -> m.loggedAt }.take(limit) }

    override suspend fun mealById(id: String): Meal? = state.value.firstOrNull { it.id == id }
    override suspend fun upsert(meal: Meal) { state.value = state.value.filterNot { it.id == meal.id } + meal }
    override suspend fun delete(id: String) { state.value = state.value.filterNot { it.id == id } }
}

class FakeExerciseRepo(initial: List<ExerciseEntry> = emptyList()) : ExerciseRepository {
    private val state = MutableStateFlow(initial)

    override fun exerciseBetween(startInclusive: Instant, endExclusive: Instant): Flow<List<ExerciseEntry>> =
        state.map { list ->
            list.filter { it.loggedAt >= startInclusive && it.loggedAt < endExclusive }
                .sortedBy { it.loggedAt }
        }

    override fun allExercise(): Flow<List<ExerciseEntry>> = state.map { it.sortedBy { e -> e.loggedAt } }
    override suspend fun upsert(entry: ExerciseEntry) { state.value = state.value.filterNot { it.id == entry.id } + entry }
    override suspend fun delete(id: String) { state.value = state.value.filterNot { it.id == id } }
}

class FakeSettingsRepo(initial: LocalTime = DayBoundary.DEFAULT_DAY_START) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val dayStart: Flow<LocalTime> = state
    override suspend fun setDayStart(time: LocalTime) {
        state.value = DayBoundary.dayStartFromMinutes(DayBoundary.toMinutes(time))
    }
}

fun testProfile(
    birthDate: LocalDate = LocalDate.of(1996, 1, 1),
    bodyFatPct: Double? = 25.0,
): UserProfile = UserProfile(
    sex = Sex.MALE,
    birthDate = birthDate,
    heightCm = 175.0,
    weightKg = 80.0,
    bodyFatPct = bodyFatPct,
    activityLevel = ActivityLevel.MODERATE,
    goal = Goal(targetWeightKg = 75.0),
    disclaimerAcceptedAt = null,
)

fun testMeal(id: String, loggedAt: Instant, kcal: Double = 500.0): Meal = Meal(
    id = id,
    loggedAt = loggedAt,
    date = LocalDate.of(2000, 1, 1), // deliberately wrong: bucketing must ignore the stored date
    name = id,
    kcal = kcal,
    proteinG = 10.0,
    carbsG = 10.0,
    fatG = 10.0,
)

fun testExercise(id: String, loggedAt: Instant, kcal: Double): ExerciseEntry = ExerciseEntry(
    id = id,
    loggedAt = loggedAt,
    activityKey = "weight_training",
    met = 5.0,
    minutes = 60,
    weightKgAtTime = 80.0,
    kcal = kcal,
)

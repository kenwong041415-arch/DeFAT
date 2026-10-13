package com.defat.core.domain.usecase

import com.defat.core.domain.calc.DayBuckets
import com.defat.core.domain.calc.TargetReferenceDate
import com.defat.core.domain.model.DaySummary
import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.repository.ExerciseRepository
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Every logical day that has at least one meal, newest first, with its total
 * against the *current* profile's target (Phase 1.6 plan §2 E18 — no per-day
 * target snapshot yet).
 *
 * A day's target includes that day's logged exercise, exactly as Home's does,
 * so the 達標 / 超標 chip here never disagrees with the ring there.
 */
class ObserveDaySummariesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<DaySummary>> = combine(
        profileRepository.profile,
        mealRepository.allMeals(),
        exerciseRepository.allExercise(),
        settingsRepository.dayStart,
    ) { profile, meals, exercise, dayStart ->
        if (profile == null) {
            emptyList()
        } else {
            val exerciseKcalByDay: Map<LocalDate, Double> = exercise
                .groupBy { DayBoundary.logicalDate(it.loggedAt, clock.zone, dayStart) }
                .mapValues { (_, entries) -> entries.sumOf { it.kcal } }
            DayBuckets.byLogicalDate(meals, clock.zone, dayStart)
                .map { (date, dayMeals) ->
                    val target = computeDailyTarget(
                        profile,
                        TargetReferenceDate.forDay(profile, date),
                        exerciseKcal = exerciseKcalByDay[date] ?: 0.0,
                    )
                    DaySummary(
                        date = date,
                        intakeKcal = dayMeals.sumOf { it.kcal },
                        targetKcal = target.targetKcal,
                        mealCount = dayMeals.size,
                    )
                }
                .sortedByDescending { it.date }
        }
    }
}

package com.defat.core.domain.usecase

import com.defat.core.domain.calc.DayBuckets
import com.defat.core.domain.calc.TargetReferenceDate
import com.defat.core.domain.model.DaySummary
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Every logical day that has at least one meal, newest first, with its total
 * against the *current* profile's target (Phase 1.6 plan §2 E18 — no per-day
 * target snapshot yet).
 */
class ObserveDaySummariesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val settingsRepository: SettingsRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<DaySummary>> = combine(
        profileRepository.profile,
        mealRepository.allMeals(),
        settingsRepository.dayStart,
    ) { profile, meals, dayStart ->
        if (profile == null) {
            emptyList()
        } else {
            DayBuckets.byLogicalDate(meals, clock.zone, dayStart)
                .map { (date, dayMeals) ->
                    val target = computeDailyTarget(profile, TargetReferenceDate.forDay(profile, date))
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

package com.defat.core.domain.usecase

import com.defat.core.domain.calc.DayRollupCalculator
import com.defat.core.domain.calc.TargetReferenceDate
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.repository.ExerciseRepository
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import com.defat.core.domain.repository.exerciseOnLogicalDay
import com.defat.core.domain.repository.mealsOnLogicalDay
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * The day rollup is computed on demand from the profile and that day's
 * meals — no cache (D2 in the Phase 1 plan).
 *
 * Despite the name, [invoke] works for any [LocalDate], not just today —
 * History (Phase 1.6) passes a past date. The target it computes always
 * reflects the *current* profile, not a snapshot from that day (plan
 * §2 E18); a per-day target snapshot is deferred to the Phase 4 trend engine.
 *
 * [date] is a *logical* date (Phase 1.7 plan §7): the day's meals are those
 * whose instant falls in the [com.defat.core.domain.calc.DayBoundary] window
 * for the user's day-start time, so a 01:30 snack counts toward the previous
 * day.
 *
 * The day's logged exercise goes into the target itself (plan §10.1,
 * DayTdeeCalculator), so the target is computed inside the flow, from that
 * day's exercise window, rather than once per profile. With no exercise logged
 * the target is exactly what it was before exercise existed.
 */
class ObserveTodayDashboardUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate): Flow<DayRollup?> =
        combine(profileRepository.profile, settingsRepository.dayStart) { profile, dayStart ->
            profile to dayStart
        }.flatMapLatest { (profile, dayStart) ->
            if (profile == null) {
                flowOf(null)
            } else {
                val referenceDate = TargetReferenceDate.forDay(profile, date)
                combine(
                    mealRepository.mealsOnLogicalDay(date, clock.zone, dayStart),
                    exerciseRepository.exerciseOnLogicalDay(date, clock.zone, dayStart),
                ) { meals, exercise ->
                    val target = computeDailyTarget(
                        profile,
                        referenceDate,
                        exerciseKcal = exercise.sumOf { it.kcal },
                    )
                    DayRollupCalculator.rollup(date, target, meals, exercise)
                }
            }
        }
}

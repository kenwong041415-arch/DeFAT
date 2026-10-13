package com.defat.core.domain.usecase

import com.defat.core.domain.calc.DayRollupCalculator
import com.defat.core.domain.calc.TargetReferenceDate
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.SettingsRepository
import com.defat.core.domain.repository.mealsOnLogicalDay
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
 */
class ObserveTodayDashboardUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
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
                val target = computeDailyTarget(profile, TargetReferenceDate.forDay(profile, date))
                mealRepository.mealsOnLogicalDay(date, clock.zone, dayStart).map { meals ->
                    DayRollupCalculator.rollup(date, target, meals)
                }
            }
        }
}

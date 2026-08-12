package com.defat.core.domain.usecase

import com.defat.core.domain.calc.DayRollupCalculator
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
 */
class ObserveTodayDashboardUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val computeDailyTarget: ComputeDailyTargetUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate): Flow<DayRollup?> =
        profileRepository.profile.flatMapLatest { profile ->
            if (profile == null) {
                flowOf(null)
            } else {
                val target = computeDailyTarget(profile, date)
                mealRepository.mealsOn(date).map { meals ->
                    DayRollupCalculator.rollup(date, target, meals)
                }
            }
        }
}

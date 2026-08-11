package com.defat.core.domain.usecase

import com.defat.core.domain.model.WeightEntry
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.WeightRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Writes the measurement and, when it is the most recent one on record,
 * updates the profile's current weight / body-fat % so the daily target
 * recomputes immediately. This coupling is intentional.
 *
 * Back-dated entries (correcting last Tuesday's weigh-in) are stored but do
 * NOT move the profile: the target must always follow the latest known body
 * state, otherwise the home screen would show a newer weight than the
 * numbers it derived its target from.
 */
class LogWeightUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(entry: WeightEntry) {
        val previousLatest = weightRepository.latest.first()
        weightRepository.upsert(entry)

        val isLatest = previousLatest == null || !entry.date.isBefore(previousLatest.date)
        if (isLatest) {
            profileRepository.updateCurrentBody(entry.weightKg, entry.bodyFatPct)
        }
    }
}

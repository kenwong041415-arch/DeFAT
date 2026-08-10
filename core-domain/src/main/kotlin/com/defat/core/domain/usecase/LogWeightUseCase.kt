package com.defat.core.domain.usecase

import com.defat.core.domain.model.WeightEntry
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.WeightRepository
import javax.inject.Inject

/**
 * Writes the measurement AND updates the profile's current weight/fat %
 * so targets recompute immediately. This coupling is intentional.
 */
class LogWeightUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(entry: WeightEntry) {
        weightRepository.upsert(entry)
        profileRepository.updateCurrentBody(entry.weightKg, entry.bodyFatPct)
    }
}

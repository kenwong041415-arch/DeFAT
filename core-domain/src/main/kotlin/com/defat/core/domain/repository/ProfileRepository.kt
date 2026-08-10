package com.defat.core.domain.repository

import com.defat.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profile: Flow<UserProfile?>
    val onboardingComplete: Flow<Boolean>
    suspend fun save(profile: UserProfile)
    suspend fun updateCurrentBody(weightKg: Double, bodyFatPct: Double?)
    suspend fun clear()
}

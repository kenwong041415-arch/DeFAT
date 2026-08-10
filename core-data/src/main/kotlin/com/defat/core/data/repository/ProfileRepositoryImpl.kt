package com.defat.core.data.repository

import com.defat.core.data.datastore.ProfileLocalDataSource
import com.defat.core.data.di.IoDispatcher
import com.defat.core.domain.model.UserProfile
import com.defat.core.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val localDataSource: ProfileLocalDataSource,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProfileRepository {

    override val profile: Flow<UserProfile?> = localDataSource.profile.flowOn(io)

    override val onboardingComplete: Flow<Boolean> = localDataSource.onboardingComplete.flowOn(io)

    override suspend fun save(profile: UserProfile) = withContext(io) {
        localDataSource.save(profile)
    }

    override suspend fun updateCurrentBody(weightKg: Double, bodyFatPct: Double?) = withContext(io) {
        localDataSource.updateBody(weightKg, bodyFatPct)
    }

    override suspend fun clear() = withContext(io) {
        localDataSource.clear()
    }
}

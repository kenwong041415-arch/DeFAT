package com.defat.core.data.di

import com.defat.core.data.repository.MealRepositoryImpl
import com.defat.core.data.repository.ProfileRepositoryImpl
import com.defat.core.data.repository.WeightRepositoryImpl
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.repository.ProfileRepository
import com.defat.core.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces (:core-domain) to their :core-data
 * implementations. The four use cases in :core-domain carry @Inject
 * constructors (plan §7.3) so Hilt provides them without an explicit
 * @Provides here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository
}

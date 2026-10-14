package com.defat.core.data.di

import com.defat.core.data.auth.FirebaseAuthRepository
import com.defat.core.data.sync.DataStoreSyncStateStore
import com.defat.core.data.sync.ExerciseSyncSource
import com.defat.core.data.sync.FirestoreSyncStore
import com.defat.core.data.sync.FoodOverridesSyncSource
import com.defat.core.data.sync.MealsSyncSource
import com.defat.core.data.sync.ProfileSyncSource
import com.defat.core.data.sync.WaterSyncSource
import com.defat.core.data.sync.WeightsSyncSource
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.sync.DefaultSyncCoordinator
import com.defat.core.domain.sync.ExportMyDataUseCase
import com.defat.core.domain.sync.SyncCoordinator
import com.defat.core.domain.sync.SyncStateStore
import com.defat.core.domain.sync.SyncableCollection
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncStateStore(impl: DataStoreSyncStateStore): SyncStateStore
}

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    /**
     * Every local source. SyncEngine checks this list against SYNCED_SUBCOLLECTIONS
     * on construction, so forgetting one here fails the first sync loudly in the
     * debug build instead of silently skipping a collection.
     */
    @Provides
    @Singleton
    fun provideSyncSources(
        profile: ProfileSyncSource,
        meals: MealsSyncSource,
        weights: WeightsSyncSource,
        exercise: ExerciseSyncSource,
        water: WaterSyncSource,
        foodOverrides: FoodOverridesSyncSource,
    ): List<SyncableCollection> = listOf(profile, meals, weights, exercise, water, foodOverrides)

    @Provides
    @Singleton
    fun provideSyncCoordinator(
        auth: AuthRepository,
        sources: List<@JvmSuppressWildcards SyncableCollection>,
        firestore: FirebaseFirestore,
        state: SyncStateStore,
        clock: Clock,
    ): SyncCoordinator = DefaultSyncCoordinator(
        auth = auth,
        sources = sources,
        remoteFor = { uid -> FirestoreSyncStore(firestore, uid) },
        state = state,
        nowMillis = { clock.millis() },
    )

    @Provides
    @Singleton
    fun provideExportMyData(
        auth: AuthRepository,
        sources: List<@JvmSuppressWildcards SyncableCollection>,
        clock: Clock,
    ): ExportMyDataUseCase = ExportMyDataUseCase(sources = sources, auth = auth, now = { clock.instant() })
}

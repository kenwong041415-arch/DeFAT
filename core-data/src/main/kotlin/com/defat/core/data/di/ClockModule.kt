package com.defat.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * The one place the wall clock enters the app. Use cases and view models take
 * a [Clock] so tests can fix "now" — with a 04:00 day start, any test that
 * reads the real clock between 00:00 and 04:00 would flip days.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

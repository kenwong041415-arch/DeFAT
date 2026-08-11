package com.defat.core.data.di

import android.content.Context
import androidx.room.Room
import com.defat.core.data.db.DefatDatabase
import com.defat.core.data.db.MealDao
import com.defat.core.data.db.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DefatDatabase =
        Room.databaseBuilder(context, DefatDatabase::class.java, DefatDatabase.DATABASE_NAME)
            // fallbackToDestructiveMigration is forbidden — the owner's real
            // data lives here (plan §7.3). Future schema changes ship real
            // Room migrations.
            .build()

    @Provides
    fun provideMealDao(database: DefatDatabase): MealDao = database.mealDao()

    @Provides
    fun provideWeightDao(database: DefatDatabase): WeightDao = database.weightDao()
}

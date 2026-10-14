package com.defat.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.defat.core.data.db.DefatDatabase
import com.defat.core.data.db.ExerciseDao
import com.defat.core.data.db.FoodDao
import com.defat.core.data.db.FoodMigrations
import com.defat.core.data.db.MealDao
import com.defat.core.data.db.MealMigrations
import com.defat.core.data.db.SyncMigrations
import com.defat.core.data.db.WaterDao
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
            .addMigrations(
                MealMigrations.MIGRATION_1_2,
                FoodMigrations.MIGRATION_2_3,
                SyncMigrations.MIGRATION_3_4,
            )
            // A fresh install is created at the current version and never runs a
            // migration, so it needs the food library seeded here. Not called
            // for an upgraded database, whose seed comes from MIGRATION_2_3.
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    FoodMigrations.SEED_STATEMENTS.forEach(db::execSQL)
                }
            })
            // fallbackToDestructiveMigration is forbidden — the owner's real
            // data lives here (plan §7.3 / phase1.6 plan §3 R3). A wrong
            // migration must crash and be fixed, never silently wipe.
            .build()

    @Provides
    fun provideMealDao(database: DefatDatabase): MealDao = database.mealDao()

    @Provides
    fun provideWeightDao(database: DefatDatabase): WeightDao = database.weightDao()

    @Provides
    fun provideFoodDao(database: DefatDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideExerciseDao(database: DefatDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWaterDao(database: DefatDatabase): WaterDao = database.waterDao()
}

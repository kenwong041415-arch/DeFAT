package com.defat.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.defat.core.data.db.entity.ExerciseEntryEntity
import com.defat.core.data.db.entity.FoodItemEntity
import com.defat.core.data.db.entity.FoodModifierEntity
import com.defat.core.data.db.entity.MealEntity
import com.defat.core.data.db.entity.WaterLogEntity
import com.defat.core.data.db.entity.WeightEntryEntity

@Database(
    entities = [
        MealEntity::class,
        WeightEntryEntity::class,
        FoodItemEntity::class,
        FoodModifierEntity::class,
        ExerciseEntryEntity::class,
        WaterLogEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class DefatDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao
    abstract fun foodDao(): FoodDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun waterDao(): WaterDao

    companion object {
        const val DATABASE_NAME = "defat.db"
    }
}

package com.defat.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.defat.core.data.db.entity.MealEntity
import com.defat.core.data.db.entity.WeightEntryEntity

@Database(
    entities = [MealEntity::class, WeightEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DefatDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao

    companion object {
        const val DATABASE_NAME = "defat.db"
    }
}

package com.defat.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adds `meals.mealType` and back-fills existing rows from the time
 * of day they were logged, using the same boundaries as
 * com.defat.core.domain.calc.MealTypeInference (plan §2 E6).
 *
 * fallbackToDestructiveMigration is forbidden here and everywhere — the
 * owner's real logged meals live in this database (plan §3 R3).
 */
object MealMigrations {

    /**
     * Single source of truth. The Migration below runs these, and
     * MealTypeMigrationSqlTest runs these same strings against real SQLite via
     * the JDBC driver, so the test cannot drift from the shipped SQL.
     */
    val MIGRATION_1_2_STATEMENTS: List<String> = listOf(
        "ALTER TABLE `meals` ADD COLUMN `mealType` TEXT NOT NULL DEFAULT 'SNACK'",
        """
        UPDATE `meals` SET `mealType` = CASE
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 1030 THEN 'BREAKFAST'
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 1430 THEN 'LUNCH'
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 1730 THEN 'AFTERNOON_TEA'
            WHEN CAST(strftime('%H%M', `loggedAtMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER) < 2100 THEN 'DINNER'
            ELSE 'SNACK'
        END
        """.trimIndent(),
    )

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_1_2_STATEMENTS.forEach(db::execSQL)
        }
    }
}

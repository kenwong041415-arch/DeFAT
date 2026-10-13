package com.defat.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 -> v3 (Phase 1.7 PR B): four new tables — `food_items`, `food_modifiers`,
 * `exercise_entries`, `water_log` — plus two nullable columns on `meals`
 * (`baseFoodId`, `modifierIds`), and the seed rows. One migration, one version
 * bump.
 *
 * fallbackToDestructiveMigration is forbidden here and everywhere — the
 * owner's real logged meals live in this database.
 *
 * Nothing here has a SQL `DEFAULT`, and no new entity column declares a
 * `@ColumnInfo(defaultValue)`, so Room's launch-time schema check has no
 * default to disagree with (the trap that bit `meals.mealType` in Phase 1.6).
 * If a default is ever added, the entity and the SQL below must carry the
 * identical text.
 */
object FoodMigrations {

    /** DDL only. Room 2.7 emits this shape for the matching entities. */
    val SCHEMA_STATEMENTS: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS `food_items` (
            `id` TEXT NOT NULL,
            `nameZh` TEXT NOT NULL,
            `nameEn` TEXT NOT NULL,
            `portionLabel` TEXT NOT NULL,
            `portionGrams` REAL,
            `kcal` REAL NOT NULL,
            `proteinG` REAL,
            `carbsG` REAL,
            `fatG` REAL,
            `kcalLow` REAL,
            `kcalHigh` REAL,
            `source` TEXT NOT NULL,
            `confidence` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `isSeed` INTEGER NOT NULL,
            `editedByOwner` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `food_items_category_idx` ON `food_items` (`category`)",
        """
        CREATE TABLE IF NOT EXISTS `food_modifiers` (
            `id` TEXT NOT NULL,
            `nameZh` TEXT NOT NULL,
            `nameEn` TEXT NOT NULL,
            `groupId` TEXT NOT NULL,
            `appliesTo` TEXT NOT NULL,
            `kcalDelta` REAL NOT NULL,
            `proteinDelta` REAL,
            `carbsDelta` REAL,
            `fatDelta` REAL,
            `source` TEXT NOT NULL,
            `confidence` TEXT NOT NULL,
            `editedByOwner` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `exercise_entries` (
            `id` TEXT NOT NULL,
            `loggedAtMillis` INTEGER NOT NULL,
            `activityKey` TEXT NOT NULL,
            `met` REAL NOT NULL,
            `minutes` INTEGER NOT NULL,
            `weightKgAtTime` REAL NOT NULL,
            `kcal` REAL NOT NULL,
            `note` TEXT,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_exercise_entries_loggedAtMillis` ON `exercise_entries` (`loggedAtMillis`)",
        """
        CREATE TABLE IF NOT EXISTS `water_log` (
            `dateEpochDay` INTEGER NOT NULL,
            `ml` INTEGER NOT NULL,
            PRIMARY KEY(`dateEpochDay`)
        )
        """.trimIndent(),
        "ALTER TABLE `meals` ADD COLUMN `baseFoodId` TEXT",
        "ALTER TABLE `meals` ADD COLUMN `modifierIds` TEXT",
    )

    /** The seed `INSERT`s, generated from the two CSVs (see [FoodSeed]). */
    fun seedStatements(foodCsv: String, modifierCsv: String): List<String> =
        FoodSeed.foodInsertStatements(foodCsv) +
            FoodSeed.modifierInsertStatements(modifierCsv, FoodSeed.foodIds(foodCsv))

    /**
     * The seed rows alone, read from the packaged CSVs. A migration only runs
     * for a database that already exists (the owner's v2 install); a fresh
     * install is created at v3 directly and never runs MIGRATION_2_3, so
     * DatabaseModule also runs these in a `RoomDatabase.Callback.onCreate`.
     * `onCreate` is not called for an upgraded database, so no row is seeded twice.
     */
    val SEED_STATEMENTS: List<String> by lazy {
        seedStatements(
            foodCsv = FoodSeed.readResource(FoodSeed.FOOD_CSV),
            modifierCsv = FoodSeed.readResource(FoodSeed.MODIFIER_CSV),
        )
    }

    /**
     * Single source of truth. The Migration below runs these, and
     * FoodItemMigrationSqlTest runs these same strings against real SQLite via
     * the JDBC driver, so the test cannot drift from the shipped migration.
     */
    val MIGRATION_2_3_STATEMENTS: List<String> by lazy { SCHEMA_STATEMENTS + SEED_STATEMENTS }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_2_3_STATEMENTS.forEach(db::execSQL)
        }
    }
}

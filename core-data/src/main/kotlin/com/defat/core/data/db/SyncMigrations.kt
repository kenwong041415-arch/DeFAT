package com.defat.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 -> v4 (Phase 1.5): the bookkeeping columns cloud sync needs. Additive only.
 *
 * What each table gains, and why it is not the same everywhere:
 *
 *  - `meals`, `weight_entries` already carry `updatedAtMillis` (since v1), which
 *    is the sync `updatedAt`. They gain only `syncedAtMillis`; `meals` also gains
 *    the `deletedAtMillis` tombstone (meals are deletable).
 *  - `exercise_entries` (deletable) and `water_log` have no update time yet, so
 *    they gain `updatedAtMillis`. SQLite cannot `ADD COLUMN ... NOT NULL`
 *    without a DEFAULT, so these two carry `DEFAULT 0`, declared identically on
 *    the entities with `@ColumnInfo(defaultValue = "0")` (Phase 1.6 trap), and
 *    are then back-filled from the row's own data.
 *  - `food_items` gains nullable `updatedAtMillis` / `syncedAtMillis`. Nullable
 *    because the seed INSERTs do not name them (see FoodItemEntity).
 *
 * `syncedAtMillis` is NULL for every pre-existing row, so **every row that
 * existed before this migration is pending**. That is exactly what Case A of
 * the first sign-in (plan §6.3) needs: the owner's real logged meals are
 * uploaded, never overwritten.
 *
 * fallbackToDestructiveMigration is forbidden here and everywhere — the owner's
 * real logged meals live in this database. This migration has no DROP, DELETE
 * or INSERT OR REPLACE; SyncMigrationSqlTest asserts it.
 */
object SyncMigrations {

    /**
     * Single source of truth. The Migration below runs these, and
     * SyncMigrationSqlTest runs these same strings against real SQLite via the
     * JDBC driver, so the test cannot drift from the shipped SQL.
     */
    val MIGRATION_3_4_STATEMENTS: List<String> = listOf(
        // meals
        "ALTER TABLE `meals` ADD COLUMN `syncedAtMillis` INTEGER",
        "ALTER TABLE `meals` ADD COLUMN `deletedAtMillis` INTEGER",
        // weight_entries
        "ALTER TABLE `weight_entries` ADD COLUMN `syncedAtMillis` INTEGER",
        // exercise_entries: back-fill updatedAt from the moment the session was logged
        "ALTER TABLE `exercise_entries` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `exercise_entries` ADD COLUMN `syncedAtMillis` INTEGER",
        "ALTER TABLE `exercise_entries` ADD COLUMN `deletedAtMillis` INTEGER",
        "UPDATE `exercise_entries` SET `updatedAtMillis` = `loggedAtMillis`",
        // water_log: the row's own timestamp is its day, so use the start of that
        // day (UTC midnight of the epoch day) — any later real edit is newer.
        "ALTER TABLE `water_log` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `water_log` ADD COLUMN `syncedAtMillis` INTEGER",
        "UPDATE `water_log` SET `updatedAtMillis` = `dateEpochDay` * 86400000",
        // food_items: only owner-edited rows ever sync. They have no timestamp of
        // their own, so they are stamped "as of this migration".
        "ALTER TABLE `food_items` ADD COLUMN `updatedAtMillis` INTEGER",
        "ALTER TABLE `food_items` ADD COLUMN `syncedAtMillis` INTEGER",
        "UPDATE `food_items` SET `updatedAtMillis` = CAST(strftime('%s', 'now') AS INTEGER) * 1000 WHERE `editedByOwner` = 1",
    )

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_3_4_STATEMENTS.forEach(db::execSQL)
        }
    }
}

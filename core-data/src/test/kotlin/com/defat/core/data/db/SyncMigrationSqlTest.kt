package com.defat.core.data.db

import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Drives real SQLite (org.xerial:sqlite-jdbc) through the exact strings in
 * [SyncMigrations.MIGRATION_3_4_STATEMENTS] — the list the app's Migration runs
 * — so the test cannot drift from the shipped SQL.
 *
 * **The whole chain, not just the last step** (phase1.5 plan risk R4): the owner
 * may install v4 without ever having run v3, so the main fixture starts from a
 * real v2 database full of rows and runs 2 -> 3 -> 4 back to back. Another
 * starts from a populated v3.
 *
 * Like the other migration tests this does not exercise Room's runtime schema
 * validation; the CI-exported schema JSON is the authentic check for that, and
 * committing it is a separate task. The JVM is pinned to Asia/Hong_Kong by
 * core-data/build.gradle.kts for every test (the v1 -> v2 back-fill reads the OS
 * timezone), so the v2 fixture built here is deterministic.
 *
 * Lambda parameters are named on purpose: a broken `.use { }` block is what
 * failed CI during Phase 1.6.
 */
class SyncMigrationSqlTest {

    // v1 `meals` exactly as the other migration tests build it.
    private val v1CreateMealsSql = """
        CREATE TABLE IF NOT EXISTS `meals` (
            `id` TEXT NOT NULL,
            `loggedAtMillis` INTEGER NOT NULL,
            `date` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `kcal` REAL NOT NULL,
            `proteinG` REAL NOT NULL,
            `carbsG` REAL NOT NULL,
            `fatG` REAL NOT NULL,
            `source` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            `remoteId` TEXT,
            `pendingSync` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
    """.trimIndent()

    private val v1CreateMealsIndexSql =
        "CREATE INDEX IF NOT EXISTS `index_meals_date` ON `meals` (`date`)"

    private val createWeightSql = """
        CREATE TABLE IF NOT EXISTS `weight_entries` (
            `date` TEXT NOT NULL,
            `weightKg` REAL NOT NULL,
            `bodyFatPct` REAL,
            `recordedAtMillis` INTEGER NOT NULL,
            `source` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            `remoteId` TEXT,
            `pendingSync` INTEGER NOT NULL,
            PRIMARY KEY(`date`)
        )
    """.trimIndent()

    private lateinit var connection: Connection

    @Before
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        // A real v2 database: v1 tables, then the shipped v1 -> v2 statements.
        exec(v1CreateMealsSql, v1CreateMealsIndexSql, createWeightSql)
        exec(*MealMigrations.MIGRATION_1_2_STATEMENTS.toTypedArray())
    }

    @After
    fun tearDown() {
        connection.close()
    }

    private fun exec(vararg statements: String) {
        connection.createStatement().use { statement ->
            statements.forEach { sql -> statement.execute(sql) }
        }
    }

    private fun migrate2To3() = exec(*FoodMigrations.MIGRATION_2_3_STATEMENTS.toTypedArray())
    private fun migrate3To4() = exec(*SyncMigrations.MIGRATION_3_4_STATEMENTS.toTypedArray())

    private data class Column(val type: String, val notNull: Boolean, val primaryKey: Boolean, val default: String?)

    private fun columns(table: String): Map<String, Column> {
        val result = linkedMapOf<String, Column>()
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                while (rs.next()) {
                    result[rs.getString("name")] = Column(
                        type = rs.getString("type"),
                        notNull = rs.getInt("notnull") == 1,
                        primaryKey = rs.getInt("pk") > 0,
                        default = rs.getString("dflt_value"),
                    )
                }
            }
        }
        return result
    }

    private fun count(sql: String): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                rs.next()
                return rs.getInt(1)
            }
        }
    }

    private fun long(sql: String): Long? {
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                assertTrue(rs.next(), "no row for: $sql")
                val value = rs.getLong(1)
                return if (rs.wasNull()) null else value
            }
        }
    }

    // ---- fixtures: rows exactly as the previous builds wrote them ----

    /** Two meals and two weigh-ins as a v2 install holds them (before the food library existed). */
    private fun insertV2Rows() {
        exec(
            "INSERT INTO meals (id, loggedAtMillis, date, name, kcal, proteinG, carbsG, fatG, source, updatedAtMillis, remoteId, pendingSync, mealType) " +
                "VALUES ('m1', 1700000000000, '2026-03-05', '雞胸飯', 520.5, 45.0, 50.0, 8.0, 'MANUAL', 1700000000111, NULL, 1, 'LUNCH')",
            "INSERT INTO meals (id, loggedAtMillis, date, name, kcal, proteinG, carbsG, fatG, source, updatedAtMillis, remoteId, pendingSync, mealType) " +
                "VALUES ('m2', 1700003600000, '2026-03-05', '燕麥', 300.0, 10.0, 50.0, 6.0, 'MANUAL', 1700003600222, NULL, 1, 'BREAKFAST')",
            "INSERT INTO weight_entries (date, weightKg, bodyFatPct, recordedAtMillis, source, updatedAtMillis, remoteId, pendingSync) " +
                "VALUES ('2026-03-05', 79.4, NULL, 1700000000000, 'MANUAL', 1700000000333, NULL, 1)",
            "INSERT INTO weight_entries (date, weightKg, bodyFatPct, recordedAtMillis, source, updatedAtMillis, remoteId, pendingSync) " +
                "VALUES ('2026-03-06', 79.0, 24.5, 1700086400000, 'MANUAL', 1700086400444, NULL, 1)",
        )
    }

    /** What a v3 install adds on top: a food-library meal, exercise, water, and an owner-edited food row. */
    private fun insertV3OnlyRows() {
        exec(
            "INSERT INTO meals (id, loggedAtMillis, date, name, kcal, proteinG, carbsG, fatG, source, updatedAtMillis, remoteId, pendingSync, mealType, baseFoodId, modifierIds) " +
                "VALUES ('m3', 1700007200000, '2026-03-05', '叉燒飯', 880.0, 0, 0, 0, 'MANUAL', 1700007200555, NULL, 1, 'DINNER', 'cha_siu_rice', 'rice_less,sauce_less')",
            "INSERT INTO exercise_entries (id, loggedAtMillis, activityKey, met, minutes, weightKgAtTime, kcal, note) " +
                "VALUES ('e1', 1700010000000, 'running', 9.8, 30, 79.4, 389.0, '晨跑')",
            "INSERT INTO water_log (dateEpochDay, ml) VALUES (19666, 1800)",
            "UPDATE food_items SET kcal = 880.0, source = 'TRAINER', confidence = 'HIGH', editedByOwner = 1 WHERE id = 'cha_siu_rice'",
        )
    }

    // ---- 1 ----

    @Test
    fun `chain from a v2 database with data - 2 to 3 to 4 applies with no error and loses no row`() {
        insertV2Rows()

        migrate2To3()
        migrate3To4()

        assertEquals(2, count("SELECT COUNT(*) FROM meals"))
        assertEquals(2, count("SELECT COUNT(*) FROM weight_entries"))
    }

    @Test
    fun `chain from a populated v3 database - 3 to 4 applies and loses no row`() {
        insertV2Rows()
        migrate2To3()
        insertV3OnlyRows()

        migrate3To4()

        assertEquals(3, count("SELECT COUNT(*) FROM meals"))
        assertEquals(2, count("SELECT COUNT(*) FROM weight_entries"))
        assertEquals(1, count("SELECT COUNT(*) FROM exercise_entries"))
        assertEquals(1, count("SELECT COUNT(*) FROM water_log"))
    }

    // ---- 2 ----

    @Test
    fun `the new columns have the expected types nullability and defaults`() {
        migrate2To3()
        migrate3To4()

        val nullableInt = Column("INTEGER", false, false, null)
        assertEquals(nullableInt, columns("meals")["syncedAtMillis"])
        assertEquals(nullableInt, columns("meals")["deletedAtMillis"])
        assertEquals(nullableInt, columns("weight_entries")["syncedAtMillis"])
        assertEquals(nullableInt, columns("exercise_entries")["syncedAtMillis"])
        assertEquals(nullableInt, columns("exercise_entries")["deletedAtMillis"])
        assertEquals(nullableInt, columns("water_log")["syncedAtMillis"])
        assertEquals(nullableInt, columns("food_items")["updatedAtMillis"])
        assertEquals(nullableInt, columns("food_items")["syncedAtMillis"])

        // SQLite refuses ADD COLUMN ... NOT NULL without a DEFAULT, so these two
        // are the only defaults in the migration. The entities declare
        // @ColumnInfo(defaultValue = "0") to match (ExerciseEntryEntity, WaterLogEntity).
        assertEquals(Column("INTEGER", true, false, "0"), columns("exercise_entries")["updatedAtMillis"])
        assertEquals(Column("INTEGER", true, false, "0"), columns("water_log")["updatedAtMillis"])
    }

    @Test
    fun `meals and weights keep their existing updatedAtMillis column instead of gaining a second one`() {
        migrate2To3()
        migrate3To4()

        assertEquals(Column("INTEGER", true, false, null), columns("meals")["updatedAtMillis"])
        assertEquals(Column("INTEGER", true, false, null), columns("weight_entries")["updatedAtMillis"])
        assertFalse("updatedAt" in columns("meals"))
    }

    @Test
    fun `the only DEFAULT clauses are the two the entities declare identically`() {
        val withDefault = SyncMigrations.MIGRATION_3_4_STATEMENTS.filter { "DEFAULT" in it.uppercase() }
        assertEquals(2, withDefault.size, withDefault.toString())
        assertTrue(withDefault.all { it.contains("`updatedAtMillis` INTEGER NOT NULL DEFAULT 0") })
    }

    @Test
    fun `the Phase 1_6 mealType default is untouched`() {
        migrate2To3()
        migrate3To4()

        assertEquals(Column("TEXT", true, false, "'SNACK'"), columns("meals")["mealType"])
    }

    // ---- 3: every pre-existing row is pending (Case A depends on this) ----

    @Test
    fun `after the chain from v2 every pre-existing row is pending`() {
        insertV2Rows()
        migrate2To3()
        migrate3To4()

        assertEquals(2, count("SELECT COUNT(*) FROM meals WHERE ${SyncSql.PENDING}"))
        assertEquals(2, count("SELECT COUNT(*) FROM weight_entries WHERE ${SyncSql.PENDING}"))
        assertEquals(2, count("SELECT COUNT(*) FROM meals WHERE syncedAtMillis IS NULL"))
        assertEquals(2, count("SELECT COUNT(*) FROM weight_entries WHERE syncedAtMillis IS NULL"))
    }

    @Test
    fun `after the chain from a populated v3 every row in every table is pending`() {
        insertV2Rows()
        migrate2To3()
        insertV3OnlyRows()
        migrate3To4()

        assertEquals(3, count("SELECT COUNT(*) FROM meals WHERE ${SyncSql.PENDING}"))
        assertEquals(2, count("SELECT COUNT(*) FROM weight_entries WHERE ${SyncSql.PENDING}"))
        assertEquals(1, count("SELECT COUNT(*) FROM exercise_entries WHERE ${SyncSql.PENDING}"))
        assertEquals(1, count("SELECT COUNT(*) FROM water_log WHERE ${SyncSql.PENDING}"))
        // The owner's one edited food row is pending; no seed row is.
        assertEquals(1, count("SELECT COUNT(*) FROM food_items WHERE ${SyncSql.FOOD_PENDING}"))
        assertEquals(0, count("SELECT COUNT(*) FROM food_items WHERE editedByOwner = 0 AND syncedAtMillis IS NOT NULL"))
    }

    @Test
    fun `no row is born synced and no row is born deleted`() {
        insertV2Rows()
        migrate2To3()
        insertV3OnlyRows()
        migrate3To4()

        for (table in listOf("meals", "weight_entries", "exercise_entries", "water_log", "food_items")) {
            assertEquals(0, count("SELECT COUNT(*) FROM `$table` WHERE syncedAtMillis IS NOT NULL"), table)
        }
        assertEquals(0, count("SELECT COUNT(*) FROM meals WHERE deletedAtMillis IS NOT NULL"))
        assertEquals(0, count("SELECT COUNT(*) FROM exercise_entries WHERE deletedAtMillis IS NOT NULL"))
    }

    // ---- 4: updatedAt back-fill ----

    @Test
    fun `meals and weights keep the updatedAtMillis they already had`() {
        insertV2Rows()
        migrate2To3()
        migrate3To4()

        assertEquals(1_700_000_000_111L, long("SELECT updatedAtMillis FROM meals WHERE id = 'm1'"))
        assertEquals(1_700_003_600_222L, long("SELECT updatedAtMillis FROM meals WHERE id = 'm2'"))
        assertEquals(1_700_000_000_333L, long("SELECT updatedAtMillis FROM weight_entries WHERE date = '2026-03-05'"))
        assertEquals(1_700_086_400_444L, long("SELECT updatedAtMillis FROM weight_entries WHERE date = '2026-03-06'"))
    }

    @Test
    fun `exercise updatedAt is back-filled from the session's own loggedAt`() {
        migrate2To3()
        insertV3OnlyRows()
        migrate3To4()

        assertEquals(1_700_010_000_000L, long("SELECT updatedAtMillis FROM exercise_entries WHERE id = 'e1'"))
    }

    @Test
    fun `water updatedAt is back-filled from the start of the row's own day`() {
        migrate2To3()
        insertV3OnlyRows()
        migrate3To4()

        assertEquals(19_666L * 86_400_000L, long("SELECT updatedAtMillis FROM water_log WHERE dateEpochDay = 19666"))
    }

    @Test
    fun `an owner-edited food row is stamped and seed rows are not`() {
        migrate2To3()
        insertV3OnlyRows()
        val before = System.currentTimeMillis() - 2_000
        migrate3To4()

        val stamped = long("SELECT updatedAtMillis FROM food_items WHERE id = 'cha_siu_rice'")
        assertNotNull(stamped)
        assertTrue(stamped >= before, "stamped=$stamped before=$before")
        assertNull(long("SELECT updatedAtMillis FROM food_items WHERE id = 'roast_goose_rice'"))
        assertEquals(0, count("SELECT COUNT(*) FROM food_items WHERE editedByOwner = 0 AND updatedAtMillis IS NOT NULL"))
    }

    // ---- 5: nothing else changes ----

    @Test
    fun `every pre-existing column value survives the whole chain`() {
        insertV2Rows()
        migrate2To3()
        insertV3OnlyRows()
        migrate3To4()

        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM meals WHERE id = 'm3'").use { rs ->
                assertTrue(rs.next())
                assertEquals("叉燒飯", rs.getString("name"))
                assertEquals(880.0, rs.getDouble("kcal"))
                assertEquals("DINNER", rs.getString("mealType"))
                assertEquals("cha_siu_rice", rs.getString("baseFoodId"))
                assertEquals("rice_less,sauce_less", rs.getString("modifierIds"))
            }
            statement.executeQuery("SELECT * FROM meals WHERE id = 'm1'").use { rs ->
                assertTrue(rs.next())
                assertEquals("雞胸飯", rs.getString("name"))
                assertEquals(520.5, rs.getDouble("kcal"))
                assertEquals(45.0, rs.getDouble("proteinG"))
                assertEquals("LUNCH", rs.getString("mealType"))
                assertEquals(1, rs.getInt("pendingSync"))
                assertNull(rs.getString("baseFoodId"))
            }
            statement.executeQuery("SELECT * FROM exercise_entries WHERE id = 'e1'").use { rs ->
                assertTrue(rs.next())
                assertEquals("running", rs.getString("activityKey"))
                assertEquals(9.8, rs.getDouble("met"))
                assertEquals(30, rs.getInt("minutes"))
                assertEquals(389.0, rs.getDouble("kcal"))
                assertEquals("晨跑", rs.getString("note"))
            }
            statement.executeQuery("SELECT * FROM water_log WHERE dateEpochDay = 19666").use { rs ->
                assertTrue(rs.next())
                assertEquals(1800, rs.getInt("ml"))
            }
            statement.executeQuery("SELECT * FROM food_items WHERE id = 'cha_siu_rice'").use { rs ->
                assertTrue(rs.next())
                assertEquals(880.0, rs.getDouble("kcal"))
                assertEquals("TRAINER", rs.getString("source"))
                assertEquals(1, rs.getInt("editedByOwner"))
            }
        }
    }

    @Test
    fun `the migration never drops or deletes anything and never replaces a row`() {
        SyncMigrations.MIGRATION_3_4_STATEMENTS.forEach { sql ->
            val upper = sql.uppercase()
            assertFalse("DROP " in upper, sql)
            assertFalse("DELETE " in upper, sql)
            assertFalse("INSERT " in upper, sql)
        }
        // The only UPDATEs are the three back-fills.
        assertEquals(3, SyncMigrations.MIGRATION_3_4_STATEMENTS.count { it.trimStart().uppercase().startsWith("UPDATE") })
    }

    // ---- 6: a fresh install ----

    @Test
    fun `a fresh install at v4 can still be seeded because the new food_items columns are nullable`() {
        // Room creates the v4 tables itself on a fresh install and then onCreate
        // runs SEED_STATEMENTS, which do not name the new columns. The schema
        // statements plus the migration stand in for Room's CREATE.
        connection.close()
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        exec(v1CreateMealsSql, createWeightSql)
        exec(*MealMigrations.MIGRATION_1_2_STATEMENTS.toTypedArray())
        exec(*FoodMigrations.SCHEMA_STATEMENTS.toTypedArray())
        migrate3To4()

        exec(*FoodMigrations.SEED_STATEMENTS.toTypedArray())

        assertTrue(count("SELECT COUNT(*) FROM food_items") > 0)
        assertEquals(0, count("SELECT COUNT(*) FROM food_items WHERE ${SyncSql.FOOD_PENDING}"))
    }

    @Test
    fun `an unedited library is never pending, so a new install uploads no seed rows`() {
        migrate2To3()
        migrate3To4()

        assertEquals(0, count("SELECT COUNT(*) FROM food_items WHERE ${SyncSql.FOOD_PENDING}"))
    }
}

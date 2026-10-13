package com.defat.core.data.db

import com.defat.core.domain.calc.ModifierGroups
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
 * [FoodMigrations.MIGRATION_2_3_STATEMENTS] — the same list the app's
 * Migration runs — so this test cannot drift from the shipped SQL.
 *
 * Like MealTypeMigrationSqlTest this does not exercise Room's runtime schema
 * validation. The CI-exported schema JSON is the authentic check for that, and
 * committing it is a separate task (plan §17 R5).
 *
 * The lambda parameters below are named on purpose: a broken `.use { }` block
 * is what failed CI during Phase 1.6.
 */
class FoodItemMigrationSqlTest {

    // v1 `meals`, exactly as MealTypeMigrationSqlTest builds it; the shipped
    // v1 -> v2 statements are then applied to reach the real v2 shape.
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

    // `weight_entries` as Room generates it from WeightEntryEntity (unchanged since v1).
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
        connection.createStatement().use { statement ->
            statement.execute(v1CreateMealsSql)
            statement.execute(v1CreateMealsIndexSql)
            statement.execute(createWeightSql)
            MealMigrations.MIGRATION_1_2_STATEMENTS.forEach { sql -> statement.execute(sql) }
        }
    }

    @After
    fun tearDown() {
        connection.close()
    }

    private fun runMigration() {
        connection.createStatement().use { statement ->
            FoodMigrations.MIGRATION_2_3_STATEMENTS.forEach { sql -> statement.execute(sql) }
        }
    }

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

    private fun indexNames(table: String): Set<String> {
        val names = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA index_list(`$table`)").use { rs ->
                while (rs.next()) names.add(rs.getString("name"))
            }
        }
        return names
    }

    private fun count(sql: String): Int {
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                rs.next()
                return rs.getInt(1)
            }
        }
    }

    private fun csvDataRows(resource: String): Int =
        FoodSeed.readResource(resource).lines().count { line -> line.isNotBlank() } - 1

    // ---- 1 ----

    @Test
    fun `v2 schema plus MIGRATION_2_3_STATEMENTS applies with no error`() {
        runMigration()

        val tables = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { rs ->
                while (rs.next()) tables.add(rs.getString("name"))
            }
        }
        assertTrue(
            tables.containsAll(setOf("food_items", "food_modifiers", "exercise_entries", "water_log", "meals", "weight_entries")),
            "tables after migration: $tables",
        )
    }

    // ---- 2 ----

    @Test
    fun `new tables have every expected column with the expected nullability`() {
        runMigration()

        assertEquals(
            mapOf(
                "id" to Column("TEXT", true, true, null),
                "nameZh" to Column("TEXT", true, false, null),
                "nameEn" to Column("TEXT", true, false, null),
                "portionLabel" to Column("TEXT", true, false, null),
                "portionGrams" to Column("REAL", false, false, null),
                "kcal" to Column("REAL", true, false, null),
                "proteinG" to Column("REAL", false, false, null),
                "carbsG" to Column("REAL", false, false, null),
                "fatG" to Column("REAL", false, false, null),
                "kcalLow" to Column("REAL", false, false, null),
                "kcalHigh" to Column("REAL", false, false, null),
                "source" to Column("TEXT", true, false, null),
                "confidence" to Column("TEXT", true, false, null),
                "category" to Column("TEXT", true, false, null),
                "isSeed" to Column("INTEGER", true, false, null),
                "editedByOwner" to Column("INTEGER", true, false, null),
            ),
            columns("food_items"),
        )
        assertEquals(
            mapOf(
                "id" to Column("TEXT", true, true, null),
                "nameZh" to Column("TEXT", true, false, null),
                "nameEn" to Column("TEXT", true, false, null),
                "groupId" to Column("TEXT", true, false, null),
                "appliesTo" to Column("TEXT", true, false, null),
                "kcalDelta" to Column("REAL", true, false, null),
                "proteinDelta" to Column("REAL", false, false, null),
                "carbsDelta" to Column("REAL", false, false, null),
                "fatDelta" to Column("REAL", false, false, null),
                "source" to Column("TEXT", true, false, null),
                "confidence" to Column("TEXT", true, false, null),
                "editedByOwner" to Column("INTEGER", true, false, null),
            ),
            columns("food_modifiers"),
        )
        assertEquals(
            mapOf(
                "id" to Column("TEXT", true, true, null),
                "loggedAtMillis" to Column("INTEGER", true, false, null),
                "activityKey" to Column("TEXT", true, false, null),
                "met" to Column("REAL", true, false, null),
                "minutes" to Column("INTEGER", true, false, null),
                "weightKgAtTime" to Column("REAL", true, false, null),
                "kcal" to Column("REAL", true, false, null),
                "note" to Column("TEXT", false, false, null),
            ),
            columns("exercise_entries"),
        )
        assertEquals(
            mapOf(
                "dateEpochDay" to Column("INTEGER", true, true, null),
                "ml" to Column("INTEGER", true, false, null),
            ),
            columns("water_log"),
        )
    }

    @Test
    fun `meals gains baseFoodId and modifierIds as nullable columns and keeps mealType`() {
        runMigration()

        val meals = columns("meals")
        assertEquals(Column("TEXT", false, false, null), meals["baseFoodId"])
        assertEquals(Column("TEXT", false, false, null), meals["modifierIds"])
        // The v2 column and its DEFAULT are untouched (plan §3 R4 from Phase 1.6).
        assertEquals(Column("TEXT", true, false, "'SNACK'"), meals["mealType"])
    }

    @Test
    fun `the indexes exist under the names the entities declare`() {
        runMigration()

        assertTrue("food_items_category_idx" in indexNames("food_items"))
        assertTrue("index_exercise_entries_loggedAtMillis" in indexNames("exercise_entries"))
        assertTrue("index_meals_date" in indexNames("meals"))
    }

    // ---- 3 ----

    @Test
    fun `the seed row counts match each CSV`() {
        runMigration()

        assertEquals(csvDataRows(FoodSeed.FOOD_CSV), count("SELECT COUNT(*) FROM food_items"))
        assertEquals(csvDataRows(FoodSeed.MODIFIER_CSV), count("SELECT COUNT(*) FROM food_modifiers"))
        assertTrue(count("SELECT COUNT(*) FROM food_items") > 0)
        assertTrue(count("SELECT COUNT(*) FROM food_modifiers") > 0)
        assertEquals(count("SELECT COUNT(*) FROM food_items"), count("SELECT COUNT(*) FROM food_items WHERE isSeed = 1 AND editedByOwner = 0"))
        assertEquals(0, count("SELECT COUNT(*) FROM exercise_entries"))
        assertEquals(0, count("SELECT COUNT(*) FROM water_log"))
    }

    // ---- 4 ----

    @Test
    fun `existing meals and weight rows survive untouched`() {
        connection.prepareStatement(
            "INSERT INTO meals (id, loggedAtMillis, date, name, kcal, proteinG, carbsG, fatG, source, updatedAtMillis, remoteId, pendingSync, mealType) " +
                "VALUES ('m1', 1700000000000, '2026-03-05', '雞胸飯', 520.5, 45.0, 50.0, 8.0, 'MANUAL', 1700000000001, NULL, 1, 'LUNCH')",
        ).use { statement -> statement.executeUpdate() }
        connection.prepareStatement(
            "INSERT INTO weight_entries (date, weightKg, bodyFatPct, recordedAtMillis, source, updatedAtMillis, remoteId, pendingSync) " +
                "VALUES ('2026-03-05', 79.4, NULL, 1700000000000, 'MANUAL', 1700000000001, NULL, 1)",
        ).use { statement -> statement.executeUpdate() }

        runMigration()

        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM meals WHERE id = 'm1'").use { rs ->
                assertTrue(rs.next())
                assertEquals(1_700_000_000_000L, rs.getLong("loggedAtMillis"))
                assertEquals("2026-03-05", rs.getString("date"))
                assertEquals("雞胸飯", rs.getString("name"))
                assertEquals(520.5, rs.getDouble("kcal"))
                assertEquals(45.0, rs.getDouble("proteinG"))
                assertEquals(50.0, rs.getDouble("carbsG"))
                assertEquals(8.0, rs.getDouble("fatG"))
                assertEquals("MANUAL", rs.getString("source"))
                assertEquals("LUNCH", rs.getString("mealType"))
                assertEquals(1, rs.getInt("pendingSync"))
                // The new columns read NULL for a meal logged before PR B.
                assertNull(rs.getString("baseFoodId"))
                assertNull(rs.getString("modifierIds"))
            }
            statement.executeQuery("SELECT * FROM weight_entries WHERE date = '2026-03-05'").use { rs ->
                assertTrue(rs.next())
                assertEquals(79.4, rs.getDouble("weightKg"))
                rs.getDouble("bodyFatPct")
                assertTrue(rs.wasNull())
                assertEquals("MANUAL", rs.getString("source"))
            }
        }
        assertEquals(1, count("SELECT COUNT(*) FROM meals"))
        assertEquals(1, count("SELECT COUNT(*) FROM weight_entries"))
    }

    // ---- 5 ----

    @Test
    fun `an UPDATE guarded by editedByOwner = 0 leaves an owner-edited row alone`() {
        runMigration()

        // The owner corrects 叉燒飯: their edit is marked.
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                "UPDATE food_items SET kcal = 880.0, source = 'TRAINER', confidence = 'HIGH', editedByOwner = 1 WHERE id = 'cha_siu_rice'",
            )
        }

        // A later migration's correction, written the way FoodDao's KDoc says to.
        connection.createStatement().use { statement ->
            val touchedOwnerRow = statement.executeUpdate(
                "UPDATE food_items SET kcal = 900.0 WHERE id = 'cha_siu_rice' AND editedByOwner = 0",
            )
            assertEquals(0, touchedOwnerRow)
            val touchedSeedRow = statement.executeUpdate(
                "UPDATE food_items SET kcal = 925.0 WHERE id = 'roast_goose_rice' AND editedByOwner = 0",
            )
            assertEquals(1, touchedSeedRow)
        }

        assertEquals(880, count("SELECT CAST(kcal AS INTEGER) FROM food_items WHERE id = 'cha_siu_rice'"))
        assertEquals(925, count("SELECT CAST(kcal AS INTEGER) FROM food_items WHERE id = 'roast_goose_rice'"))
    }

    // ---- seed content: docs/07 §2, §3, §6 ----

    private fun foodRow(id: String): Map<String, Any?> {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM food_items WHERE id = '$id'").use { rs ->
                assertTrue(rs.next(), "no seed row $id")
                val meta = rs.metaData
                return (1..meta.columnCount).associate { index -> meta.getColumnName(index) to rs.getObject(index) }
            }
        }
    }

    @Test
    fun `the reconstructed char siu rice row states its baseline and carries no range`() {
        runMigration()

        val row = foodRow("cha_siu_rice")
        assertEquals("1 碟（正常飯、半肥瘦、有汁）", row["portionLabel"])
        assertEquals(939.0, row["kcal"])
        assertNull(row["kcalLow"])
        assertNull(row["kcalHigh"])
        // Reconstructed from composition values (docs/07 §6.1), so MEDIUM; no protein was published.
        assertEquals("COMPOSITION_TABLE", row["source"])
        assertEquals("MEDIUM", row["confidence"])
        assertNull(row["proteinG"])
    }

    @Test
    fun `every reconstructed row states its baseline and has no range`() {
        runMigration()

        for (id in listOf("cha_siu_rice", "set_egg_noodles", "french_toast", "milk_tea")) {
            val row = foodRow(id)
            val label = row["portionLabel"] as String
            assertTrue("（" in label && "）" in label, "$id label '$label' does not state a baseline")
            assertNull(row["kcalLow"], id)
            assertNull(row["kcalHigh"], id)
        }
    }

    @Test
    fun `every DIM_SUM row is restated per piece`() {
        runMigration()

        val labels = mutableListOf<Pair<String, String>>()
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT id, portionLabel FROM food_items WHERE category = 'DIM_SUM'").use { rs ->
                while (rs.next()) labels.add(rs.getString("id") to rs.getString("portionLabel"))
            }
        }
        assertTrue(labels.isNotEmpty())
        labels.forEach { (id, label) -> assertEquals("1 件", label, id) }
        assertEquals(40.0, foodRow("steamed_chicken_feet")["kcal"]) // not 63, not 240
    }

    @Test
    fun `confidence is derived from the source and the spread`() {
        runMigration()

        assertEquals("HIGH", foodRow("big_mac")["confidence"]) // OFFICIAL
        assertEquals("MEDIUM", foodRow("white_rice")["confidence"]) // COMPOSITION_TABLE
        assertEquals("MEDIUM", foodRow("beef_brisket_hor_fun")["confidence"]) // DIETITIAN
        assertEquals("LOW", foodRow("luncheon_meat")["confidence"]) // MEDIA
        assertEquals("LOW", foodRow("dry_fried_beef_hor_fun")["confidence"]) // MEDIA with a range
    }

    @Test
    fun `no seed row carries a range that does not bracket its own number`() {
        runMigration()

        assertEquals(
            0,
            count(
                "SELECT COUNT(*) FROM food_items WHERE (kcalLow IS NULL) <> (kcalHigh IS NULL) " +
                    "OR (kcalLow IS NOT NULL AND (kcal < kcalLow OR kcal > kcalHigh))",
            ),
        )
    }

    @Test
    fun `protein is never invented - only the numbers docs07 publishes are present`() {
        runMigration()

        val published = setOf(25.6, 18.3, 12.2, 5.0, 6.3, 31.0, 40.0, 36.0, 27.0)
        val proteins = mutableSetOf<Double>()
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT proteinG FROM food_items WHERE proteinG IS NOT NULL").use { rs ->
                while (rs.next()) proteins.add(rs.getDouble(1))
            }
        }
        assertEquals(published, proteins)
        // And no modifier carries a macro delta: docs/07 §6.3 publishes kcal only.
        assertEquals(
            0,
            count("SELECT COUNT(*) FROM food_modifiers WHERE proteinDelta IS NOT NULL OR carbsDelta IS NOT NULL OR fatDelta IS NOT NULL"),
        )
    }

    @Test
    fun `modifier rows follow docs07 section 6_3`() {
        runMigration()

        val deltas = mutableMapOf<String, Double>()
        val groups = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT id, groupId, kcalDelta FROM food_modifiers").use { rs ->
                while (rs.next()) {
                    deltas[rs.getString("id")] = rs.getDouble("kcalDelta")
                    groups.add(rs.getString("groupId"))
                }
            }
        }
        // The five §6.3 corrections made in the PR B review are part of this
        // expectation, not exceptions to it:
        //   - peanut_butter_none -95 exists because 西多士's 495 base already
        //     includes peanut butter (correction 1),
        //   - syrup_add +120 is the §6.1 line item, not 215 (correction 2),
        //   - each add-on is its own group so 加蛋 and 加午餐肉 combine
        //     (correction 3).
        assertEquals(
            mapOf(
                "rice_less" to -130.0, "rice_more" to 130.0, "meat_lean" to -120.0, "skin_off" to -90.0,
                "sauce_none" to -100.0, "sauce_less" to -50.0, "sugar_none" to -48.0, "sugar_half" to -24.0,
                "milk_none" to -80.0, "milk_less" to -40.0, "butter_none" to -100.0,
                "peanut_butter_add" to 95.0, "peanut_butter_none" to -95.0, "syrup_add" to 120.0,
                "add_egg" to 72.0, "add_luncheon_meat" to 144.0, "add_veg" to 15.0,
            ),
            deltas,
        )
        assertEquals(ModifierGroups.ORDER.toSet(), groups)
    }

    @Test
    fun `the migration never drops or deletes anything`() {
        FoodMigrations.MIGRATION_2_3_STATEMENTS.forEach { sql ->
            val upper = sql.uppercase()
            assertFalse("DROP " in upper, sql)
            assertFalse("DELETE " in upper, sql)
            assertFalse("INSERT OR REPLACE" in upper, sql)
        }
        assertNotNull(FoodMigrations.MIGRATION_2_3_STATEMENTS.firstOrNull())
    }

    @Test
    fun `a fresh install gets the same seed as an upgrade, once`() {
        // Upgrade path: the whole migration list.
        runMigration()
        val upgradedFood = count("SELECT COUNT(*) FROM food_items")
        val upgradedModifiers = count("SELECT COUNT(*) FROM food_modifiers")

        // Fresh path: Room creates the v3 tables itself, then onCreate runs
        // SEED_STATEMENTS. Here the schema statements stand in for Room's CREATE.
        connection.close()
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { statement ->
            statement.execute(v1CreateMealsSql)
            statement.execute(createWeightSql)
            MealMigrations.MIGRATION_1_2_STATEMENTS.forEach { sql -> statement.execute(sql) }
            FoodMigrations.SCHEMA_STATEMENTS.forEach { sql -> statement.execute(sql) }
            FoodMigrations.SEED_STATEMENTS.forEach { sql -> statement.execute(sql) }
        }

        assertEquals(upgradedFood, count("SELECT COUNT(*) FROM food_items"))
        assertEquals(upgradedModifiers, count("SELECT COUNT(*) FROM food_modifiers"))
        // The migration is exactly the schema followed by the seed, nothing else.
        assertEquals(
            FoodMigrations.SCHEMA_STATEMENTS + FoodMigrations.SEED_STATEMENTS,
            FoodMigrations.MIGRATION_2_3_STATEMENTS,
        )
    }
}

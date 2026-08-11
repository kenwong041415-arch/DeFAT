package com.defat.core.data.db

import com.defat.core.domain.calc.MealTypeInference
import java.sql.Connection
import java.sql.DriverManager
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Drives real SQLite (org.xerial:sqlite-jdbc) through the exact same SQL
 * strings [MealMigrations.MIGRATION_1_2_STATEMENTS] runs in the app, so this
 * test cannot drift from the shipped migration.
 *
 * This runs the same SQL the app runs, but it does not exercise Room's
 * runtime schema validation (the @ColumnInfo(defaultValue = …) question in
 * plan §3 R4) — see plan §6.5.
 */
class MealTypeMigrationSqlTest {

    /**
     * The v1 `meals` table, exactly as Room 2.7 generates it from the v1
     * MealEntity — derived by reading that entity's field declarations in
     * git history (`git show 913e8c1:core-data/.../entity/MealEntity.kt`,
     * the last commit before the mealType column was added), not from the
     * fabricated/deleted schemas/.../1.json (plan §3 R2, phase1.6 override).
     * Column order and NOT NULL / nullability follow the entity's declared
     * properties one for one:
     *   id (PK, TEXT), loggedAtMillis (INTEGER), date (TEXT), name (TEXT),
     *   kcal/proteinG/carbsG/fatG (REAL), source (TEXT),
     *   updatedAtMillis (INTEGER), remoteId (TEXT, nullable),
     *   pendingSync (INTEGER, Boolean stored as 0/1).
     */
    private val v1CreateTableSql = """
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

    private lateinit var connection: Connection

    @Before
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { it.execute(v1CreateTableSql) }
    }

    @After
    fun tearDown() {
        connection.close()
    }

    private fun insertV1Row(
        id: String,
        loggedAtMillis: Long,
        date: String = "2026-03-05",
        name: String = "meal",
        kcal: Double = 500.0,
        proteinG: Double = 40.0,
        carbsG: Double = 50.0,
        fatG: Double = 10.0,
        source: String = "MANUAL",
        updatedAtMillis: Long = loggedAtMillis,
        remoteId: String? = null,
        pendingSync: Boolean = true,
    ) {
        connection.prepareStatement(
            "INSERT INTO meals (id, loggedAtMillis, date, name, kcal, proteinG, carbsG, fatG, source, updatedAtMillis, remoteId, pendingSync) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ).use { stmt ->
            stmt.setString(1, id)
            stmt.setLong(2, loggedAtMillis)
            stmt.setString(3, date)
            stmt.setString(4, name)
            stmt.setDouble(5, kcal)
            stmt.setDouble(6, proteinG)
            stmt.setDouble(7, carbsG)
            stmt.setDouble(8, fatG)
            stmt.setString(9, source)
            stmt.setLong(10, updatedAtMillis)
            if (remoteId == null) stmt.setNull(11, java.sql.Types.VARCHAR) else stmt.setString(11, remoteId)
            stmt.setInt(12, if (pendingSync) 1 else 0)
            stmt.executeUpdate()
        }
    }

    private fun runMigration() {
        connection.createStatement().use { stmt ->
            MealMigrations.MIGRATION_1_2_STATEMENTS.forEach { stmt.execute(it) }
        }
    }

    @Test
    fun `adds a non-null mealType column defaulting to SNACK`() {
        insertV1Row(id = "m1", loggedAtMillis = 0L)
        runMigration()

        var foundColumn = false
        connection.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA table_info(meals)").use { rs ->
                while (rs.next()) {
                    if (rs.getString("name") == "mealType") {
                        foundColumn = true
                        assertEquals("TEXT", rs.getString("type"))
                        assertEquals(1, rs.getInt("notnull"))
                        assertEquals("'SNACK'", rs.getString("dflt_value"))
                    }
                }
            }
        }
        assertTrue(foundColumn, "expected a mealType column after migration")

        // A row inserted after the migration without specifying mealType
        // reads back the SQL DEFAULT.
        connection.prepareStatement(
            "INSERT INTO meals (id, loggedAtMillis, date, name, kcal, proteinG, carbsG, fatG, source, updatedAtMillis, pendingSync) " +
                "VALUES ('m2', 0, '2026-03-05', 'x', 0, 0, 0, 0, 'MANUAL', 0, 1)",
        ).use { it.executeUpdate() }
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT mealType FROM meals WHERE id = 'm2'").use { rs ->
                rs.next()
                assertEquals("SNACK", rs.getString("mealType"))
            }
        }
    }

    @Test
    fun `back-fills every minute of the day to match MealTypeInference`() {
        val zone = ZoneId.of("Asia/Hong_Kong")
        for (m in 0..1439) {
            val hour = m / 60
            val minute = m % 60
            val millis = ZonedDateTime.of(2026, 3, 5, hour, minute, 0, 0, zone)
                .toInstant()
                .toEpochMilli()
            insertV1Row(id = "row-$m", loggedAtMillis = millis)
        }

        runMigration()

        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, mealType FROM meals").use { rs ->
                val results = mutableMapOf<String, String>()
                while (rs.next()) {
                    results[rs.getString("id")] = rs.getString("mealType")
                }
                for (m in 0..1439) {
                    val expected = MealTypeInference.forMinuteOfDay(m).name
                    assertEquals(expected, results.getValue("row-$m"), "minute $m")
                }
            }
        }
    }

    @Test
    fun `preserves every other column`() {
        insertV1Row(
            id = "row-preserve",
            loggedAtMillis = 1_700_000_000_000L,
            name = "雞胸飯",
            kcal = 520.0,
            proteinG = 45.0,
            carbsG = 50.0,
            fatG = 8.0,
            source = "MANUAL",
            remoteId = null,
            pendingSync = true,
        )

        runMigration()

        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT * FROM meals WHERE id = 'row-preserve'").use { rs ->
                rs.next()
                assertEquals(1_700_000_000_000L, rs.getLong("loggedAtMillis"))
                assertEquals("雞胸飯", rs.getString("name"))
                assertEquals(520.0, rs.getDouble("kcal"))
                assertEquals(45.0, rs.getDouble("proteinG"))
                assertEquals(50.0, rs.getDouble("carbsG"))
                assertEquals(8.0, rs.getDouble("fatG"))
                assertEquals("MANUAL", rs.getString("source"))
                rs.getString("remoteId")
                assertTrue(rs.wasNull())
                assertEquals(1, rs.getInt("pendingSync"))
            }
        }
    }

    @Test
    fun `a row logged at exactly 10-30 becomes LUNCH`() {
        val millis = ZonedDateTime.of(2026, 3, 5, 10, 30, 0, 0, ZoneId.of("Asia/Hong_Kong"))
            .toInstant()
            .toEpochMilli()
        insertV1Row(id = "boundary", loggedAtMillis = millis)

        runMigration()

        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT mealType FROM meals WHERE id = 'boundary'").use { rs ->
                rs.next()
                assertEquals("LUNCH", rs.getString("mealType"))
            }
        }
    }
}

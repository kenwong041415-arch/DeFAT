package com.defat.core.data.db

import com.defat.core.domain.calc.FoodConfidenceRules
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodSource
import kotlin.math.abs

/**
 * Turns the two checked-in spreadsheets under `src/main/resources/` into the
 * `INSERT` statements that seed `food_items` and `food_modifiers` (plan §8.2).
 * The owner edits a CSV in a spreadsheet, not Kotlin.
 *
 * Plain Kotlin on purpose (no Android types) so MIGRATION_2_3's SQL test can
 * run the exact statements the app runs.
 *
 * - A blank cell is SQL `NULL`.
 * - `confidence` is **not** a CSV column: it is computed here, by
 *   [FoodConfidenceRules], so the rule has one home.
 * - Every problem in a CSV throws with the row's id, because a seed that
 *   silently drops or mangles a row would ship a wrong number as fact.
 */
object FoodSeed {

    const val FOOD_CSV: String = "food_seed.csv"
    const val MODIFIER_CSV: String = "food_modifier_seed.csv"

    val FOOD_COLUMNS: List<String> = listOf(
        "id", "nameZh", "nameEn", "portionLabel", "portionGrams", "kcal",
        "proteinG", "carbsG", "fatG", "kcalLow", "kcalHigh", "source", "category",
    )

    val MODIFIER_COLUMNS: List<String> = listOf(
        "id", "nameZh", "nameEn", "groupId", "appliesTo", "kcalDelta",
        "proteinDelta", "carbsDelta", "fatDelta", "source",
    )

    /** Reads a seed file from the module's resources; fails loudly if it was not packaged. */
    fun readResource(name: String): String {
        val stream = FoodSeed::class.java.classLoader?.getResourceAsStream(name)
            ?: error("Seed file $name is missing from the build (core-data/src/main/resources)")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /**
     * RFC 4180 CSV: comma separated, fields may be wrapped in double quotes
     * (needed when a cell contains a comma, as `appliesTo` does), `""` inside a
     * quoted field is one quote, LF or CRLF line ends. A leading byte-order
     * mark (spreadsheets add one) is ignored and blank lines are skipped.
     */
    fun parseCsv(text: String): List<List<String>> {
        val source = text.removePrefix("﻿")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            row.add(field.toString())
            field.setLength(0)
        }

        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row)
            row = mutableListOf()
        }

        while (i < source.length) {
            val c = source[i]
            if (inQuotes) {
                when {
                    c == '"' && i + 1 < source.length && source[i + 1] == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> endField()
                    '\r' -> {
                        if (i + 1 < source.length && source[i + 1] == '\n') i++
                        endRow()
                    }
                    '\n' -> endRow()
                    else -> field.append(c)
                }
            }
            i++
        }
        require(!inQuotes) { "unterminated quoted field in CSV" }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    /** The data rows of a CSV, each as a column-name -> cell map, after checking the header is exactly [columns]. */
    internal fun records(csv: String, columns: List<String>, fileName: String): List<Map<String, String>> {
        val rows = parseCsv(csv)
        require(rows.isNotEmpty()) { "$fileName is empty" }
        val header = rows.first().map { it.trim() }
        require(header == columns) {
            "$fileName header must be exactly ${columns.joinToString(",")} but was ${header.joinToString(",")}"
        }
        return rows.drop(1).mapIndexed { index, cells ->
            require(cells.size == columns.size) {
                "$fileName row ${index + 2} has ${cells.size} cells, expected ${columns.size}"
            }
            columns.zip(cells.map { it.trim() }).toMap()
        }
    }

    /** `INSERT` statements for every row of `food_seed.csv`. */
    fun foodInsertStatements(csv: String): List<String> {
        val seenIds = HashSet<String>()
        return records(csv, FOOD_COLUMNS, FOOD_CSV).map { row ->
            val id = row.getValue("id")
            require(id.isNotEmpty()) { "$FOOD_CSV has a row with no id" }
            require(seenIds.add(id)) { "$FOOD_CSV repeats the id $id" }
            requireText(row, "nameZh", id)
            requireText(row, "nameEn", id)
            requireText(row, "portionLabel", id)

            val kcal = requireNumber(row, "kcal", id)
            require(kcal > 0.0) { "$id: kcal must be > 0" }
            val grams = optionalNumber(row, "portionGrams", id)
            require(grams == null || grams > 0.0) { "$id: portionGrams must be > 0" }
            val low = optionalNumber(row, "kcalLow", id)
            val high = optionalNumber(row, "kcalHigh", id)
            require((low == null) == (high == null)) { "$id: kcalLow and kcalHigh come as a pair" }
            if (low != null && high != null) {
                require(low <= kcal && kcal <= high) { "$id: kcal $kcal is outside the range $low..$high" }
            }
            listOf("proteinG", "carbsG", "fatG").forEach { column ->
                val value = optionalNumber(row, column, id)
                require(value == null || value >= 0.0) { "$id: $column must not be negative" }
            }

            val source = enumValue<FoodSource>(row.getValue("source"), "source", id)
            val category = enumValue<FoodCategory>(row.getValue("category"), "category", id)
            val confidence = FoodConfidenceRules.confidenceFor(source, kcal, low, high)

            insertInto(
                table = "food_items",
                columns = FOOD_COLUMNS + listOf("confidence", "isSeed", "editedByOwner"),
                values = listOf(
                    sqlText(id),
                    sqlText(row.getValue("nameZh")),
                    sqlText(row.getValue("nameEn")),
                    sqlText(row.getValue("portionLabel")),
                    sqlNumber(row, "portionGrams"),
                    sqlNumber(row, "kcal"),
                    sqlNumber(row, "proteinG"),
                    sqlNumber(row, "carbsG"),
                    sqlNumber(row, "fatG"),
                    sqlNumber(row, "kcalLow"),
                    sqlNumber(row, "kcalHigh"),
                    sqlText(source.name),
                    sqlText(category.name),
                    sqlText(confidence.name),
                    "1", // isSeed
                    "0", // editedByOwner
                ),
            )
        }
    }

    /**
     * `INSERT` statements for every row of `food_modifier_seed.csv`. An
     * `appliesTo` token is either a FoodCategory name or the id of a food row;
     * [knownFoodIds] is checked so a typo cannot silently leave a modifier that
     * applies to nothing.
     */
    fun modifierInsertStatements(csv: String, knownFoodIds: Set<String>): List<String> {
        val seenIds = HashSet<String>()
        return records(csv, MODIFIER_COLUMNS, MODIFIER_CSV).map { row ->
            val id = row.getValue("id")
            require(id.isNotEmpty()) { "$MODIFIER_CSV has a row with no id" }
            require(seenIds.add(id)) { "$MODIFIER_CSV repeats the id $id" }
            requireText(row, "nameZh", id)
            requireText(row, "nameEn", id)
            requireText(row, "groupId", id)

            val tokens = row.getValue("appliesTo").split(",").map { it.trim() }.filter { it.isNotEmpty() }
            require(tokens.isNotEmpty()) { "$id: appliesTo is empty" }
            val categoryNames = FoodCategory.entries.map { it.name }.toSet()
            tokens.forEach { token ->
                require(token in categoryNames || token in knownFoodIds) {
                    "$id: appliesTo names '$token', which is neither a FoodCategory nor a food id"
                }
            }

            val kcalDelta = requireNumber(row, "kcalDelta", id)
            listOf("proteinDelta", "carbsDelta", "fatDelta").forEach { column ->
                optionalNumber(row, column, id)
            }
            val source = enumValue<FoodSource>(row.getValue("source"), "source", id)
            val confidence = FoodConfidenceRules.confidenceFor(source, abs(kcalDelta), null, null)

            insertInto(
                table = "food_modifiers",
                columns = MODIFIER_COLUMNS + listOf("confidence", "editedByOwner"),
                values = listOf(
                    sqlText(id),
                    sqlText(row.getValue("nameZh")),
                    sqlText(row.getValue("nameEn")),
                    sqlText(row.getValue("groupId")),
                    sqlText(tokens.joinToString(",")),
                    sqlNumber(row, "kcalDelta"),
                    sqlNumber(row, "proteinDelta"),
                    sqlNumber(row, "carbsDelta"),
                    sqlNumber(row, "fatDelta"),
                    sqlText(source.name),
                    sqlText(confidence.name),
                    "0", // editedByOwner
                ),
            )
        }
    }

    /** The ids in a `food_seed.csv`, for validating the modifier file against it. */
    fun foodIds(csv: String): Set<String> =
        records(csv, FOOD_COLUMNS, FOOD_CSV).map { it.getValue("id") }.toSet()

    private fun insertInto(table: String, columns: List<String>, values: List<String>): String {
        require(columns.size == values.size) { "internal: $table has ${columns.size} columns but ${values.size} values" }
        return "INSERT INTO `$table` (${columns.joinToString(", ") { "`$it`" }}) VALUES (${values.joinToString(", ")})"
    }

    private fun requireText(row: Map<String, String>, column: String, id: String) {
        require(row.getValue(column).isNotEmpty()) { "$id: $column is blank" }
    }

    private fun requireNumber(row: Map<String, String>, column: String, id: String): Double =
        optionalNumber(row, column, id) ?: throw IllegalArgumentException("$id: $column is blank")

    private fun optionalNumber(row: Map<String, String>, column: String, id: String): Double? {
        val cell = row.getValue(column)
        if (cell.isEmpty()) return null
        val value = cell.toDoubleOrNull()
        require(value != null && value.isFinite()) { "$id: $column '$cell' is not a number" }
        return value
    }

    /** The cell as written (already validated as a number), or `NULL` when blank. */
    private fun sqlNumber(row: Map<String, String>, column: String): String =
        row.getValue(column).ifEmpty { "NULL" }

    private fun sqlText(value: String): String = "'" + value.replace("'", "''") + "'"

    private inline fun <reified E : Enum<E>> enumValue(cell: String, column: String, id: String): E =
        enumValues<E>().firstOrNull { it.name == cell }
            ?: throw IllegalArgumentException(
                "$id: $column '$cell' is not one of ${enumValues<E>().joinToString { it.name }}",
            )
}

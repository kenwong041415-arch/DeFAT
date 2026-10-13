package com.defat.core.data.db

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class FoodSeedTest {

    private val header = FoodSeed.FOOD_COLUMNS.joinToString(",")

    @Test
    fun `parseCsv handles quotes, doubled quotes, CRLF, a BOM and blank lines`() {
        val rows = FoodSeed.parseCsv("﻿a,b,c\r\n\r\n1,\"x,y\",\"say \"\"hi\"\"\"\r\n2,,\n")
        assertEquals(
            listOf(listOf("a", "b", "c"), listOf("1", "x,y", "say \"hi\""), listOf("2", "", "")),
            rows,
        )
    }

    @Test
    fun `a row with no trailing newline is still read`() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), FoodSeed.parseCsv("a,b\n1,2"))
    }

    @Test
    fun `an unterminated quote is an error`() {
        assertFailsWith<IllegalArgumentException> { FoodSeed.parseCsv("a,b\n1,\"oops") }
    }

    @Test
    fun `a blank cell becomes NULL and confidence is computed, not read`() {
        val csv = "$header\ntest_food,測試,Test,1 份,,100,,,,,,MEDIA,SNACK\n"
        val sql = FoodSeed.foodInsertStatements(csv).single()

        assertTrue("NULL" in sql)
        assertTrue("'LOW'" in sql, sql) // MEDIA -> LOW, from FoodConfidenceRules
        assertTrue(sql.startsWith("INSERT INTO `food_items`"))
        assertTrue(sql.endsWith(", 1, 0)"), sql) // isSeed = 1, editedByOwner = 0
    }

    @Test
    fun `an apostrophe in a name is escaped`() {
        val csv = "$header\nx,蛋,Chef's egg,1 隻,,72,,,,,,COMPOSITION_TABLE,STAPLE\n"
        assertTrue("'Chef''s egg'" in FoodSeed.foodInsertStatements(csv).single())
    }

    @Test
    fun `a wrong header is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            FoodSeed.foodInsertStatements("id,name\nx,y\n")
        }
    }

    @Test
    fun `bad rows are rejected with their id`() {
        val unknownSource = "$header\nx,蛋,Egg,1 隻,,72,,,,,,WIKIPEDIA,STAPLE\n"
        val message = assertFailsWith<IllegalArgumentException> { FoodSeed.foodInsertStatements(unknownSource) }.message
        assertTrue(message!!.startsWith("x:"), message)

        assertFailsWith<IllegalArgumentException> {
            FoodSeed.foodInsertStatements("$header\nx,蛋,Egg,1 隻,,abc,,,,,,MEDIA,STAPLE\n")
        }
        assertFailsWith<IllegalArgumentException> {
            FoodSeed.foodInsertStatements("$header\nx,蛋,Egg,1 隻,,0,,,,,,MEDIA,STAPLE\n")
        }
        // A range must bracket the number and come as a pair.
        assertFailsWith<IllegalArgumentException> {
            FoodSeed.foodInsertStatements("$header\nx,蛋,Egg,1 隻,,500,,,,100,200,MEDIA,STAPLE\n")
        }
        assertFailsWith<IllegalArgumentException> {
            FoodSeed.foodInsertStatements("$header\nx,蛋,Egg,1 隻,,500,,,,100,,MEDIA,STAPLE\n")
        }
        // A cell short of the header width.
        assertFailsWith<IllegalArgumentException> {
            FoodSeed.foodInsertStatements("$header\nx,蛋,Egg\n")
        }
    }

    @Test
    fun `a repeated id is rejected`() {
        val row = "x,蛋,Egg,1 隻,,72,,,,,,COMPOSITION_TABLE,STAPLE"
        assertFailsWith<IllegalArgumentException> { FoodSeed.foodInsertStatements("$header\n$row\n$row\n") }
    }

    @Test
    fun `a modifier may name a category or a known food id, and nothing else`() {
        val modHeader = FoodSeed.MODIFIER_COLUMNS.joinToString(",")
        val ok = "$modHeader\nm1,走汁,No sauce,sauce,\"ROAST_RICE,cha_siu_rice\",-100,,,,COMPOSITION_TABLE\n"
        val sql = FoodSeed.modifierInsertStatements(ok, setOf("cha_siu_rice")).single()
        assertTrue("'ROAST_RICE,cha_siu_rice'" in sql, sql)
        assertTrue("'MEDIUM'" in sql, sql)

        val typo = "$modHeader\nm1,走汁,No sauce,sauce,\"ROAST_RICE,cha_siu_rise\",-100,,,,COMPOSITION_TABLE\n"
        assertFailsWith<IllegalArgumentException> { FoodSeed.modifierInsertStatements(typo, setOf("cha_siu_rice")) }
    }

    @Test
    fun `the shipped CSVs produce one statement per data row`() {
        val food = FoodSeed.readResource(FoodSeed.FOOD_CSV)
        val modifiers = FoodSeed.readResource(FoodSeed.MODIFIER_CSV)
        val statements = FoodMigrations.seedStatements(food, modifiers)

        assertEquals(
            FoodSeed.parseCsv(food).size - 1 + FoodSeed.parseCsv(modifiers).size - 1,
            statements.size,
        )
    }
}

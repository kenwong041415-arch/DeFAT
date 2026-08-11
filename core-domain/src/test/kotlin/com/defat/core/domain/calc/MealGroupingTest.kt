package com.defat.core.domain.calc

import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import com.defat.core.domain.model.TdeeSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class MealGroupingTest {

    private val date = LocalDate.of(2026, 8, 11)

    private fun meal(
        id: String,
        time: LocalTime,
        type: MealType,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
    ) = Meal(
        id = id,
        loggedAt = date.atTime(time).atZone(ZoneOffset.UTC).toInstant(),
        date = date,
        name = id,
        kcal = kcal,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        source = MealSource.MANUAL,
        mealType = type,
    )

    private val b1 = meal("b1", LocalTime.of(8, 0), MealType.BREAKFAST, 300.0, 20.0, 40.0, 8.0)
    private val l1 = meal("l1", LocalTime.of(13, 0), MealType.LUNCH, 600.0, 30.0, 80.0, 15.0)
    private val l2 = meal("l2", LocalTime.of(12, 30), MealType.LUNCH, 450.0, 40.0, 30.0, 12.0)

    private val target = DailyTarget(
        bmrKcal = 1666.0,
        bmrMethod = BmrMethod.KATCH_MCARDLE,
        tdeeKcal = 2582.3,
        tdeeSource = TdeeSource.ACTIVITY_MULTIPLIER,
        targetKcal = 2065.84,
        floorApplied = false,
        macros = MacroTargets(proteinG = 132.0, fatG = 48.0, carbsG = 276.46, carbsClamped = false),
    )

    @Test
    fun `an empty day still returns all five groups in order`() {
        val groups = MealGrouping.groupByType(emptyList())
        assertEquals(5, groups.size)
        assertEquals(
            listOf(
                MealType.BREAKFAST,
                MealType.LUNCH,
                MealType.AFTERNOON_TEA,
                MealType.DINNER,
                MealType.SNACK,
            ),
            groups.map { it.type },
        )
        groups.forEach {
            assertTrue(it.isEmpty)
            assertEquals(0.0, it.kcal)
        }
    }

    @Test
    fun `sums each type`() {
        val groups = MealGrouping.groupByType(listOf(b1, l1, l2))
        val lunch = groups.single { it.type == MealType.LUNCH }
        assertEquals(1050.0, lunch.kcal)
        assertEquals(70.0, lunch.proteinG)
        assertEquals(110.0, lunch.carbsG)
        assertEquals(27.0, lunch.fatG)
        assertEquals(2, lunch.mealCount)

        val breakfast = groups.single { it.type == MealType.BREAKFAST }
        assertEquals(300.0, breakfast.kcal)
        assertEquals(1, breakfast.mealCount)

        listOf(MealType.AFTERNOON_TEA, MealType.DINNER, MealType.SNACK).forEach { type ->
            val group = groups.single { it.type == type }
            assertEquals(0.0, group.kcal)
            assertTrue(group.isEmpty)
        }
    }

    @Test
    fun `orders meals inside a group oldest first`() {
        val groups = MealGrouping.groupByType(listOf(l1, l2))
        val lunch = groups.single { it.type == MealType.LUNCH }
        assertEquals(listOf("l2", "l1"), lunch.meals.map { it.id })
    }

    @Test
    fun `group totals reconcile with the day rollup`() {
        val meals = listOf(b1, l1, l2)
        val groups = MealGrouping.groupByType(meals)
        val rollup = DayRollupCalculator.rollup(date, target, meals)
        assertEquals(rollup.intakeKcal, groups.sumOf { it.kcal })
        assertEquals(1350.0, rollup.intakeKcal)
    }

    @Test
    fun `types with no meals still appear`() {
        val groups = MealGrouping.groupByType(listOf(b1, l1, l2))
        assertTrue(groups.single { it.type == MealType.DINNER }.isEmpty)
    }
}

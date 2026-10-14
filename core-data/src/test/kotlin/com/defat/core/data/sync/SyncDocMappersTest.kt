package com.defat.core.data.sync

import com.defat.core.data.db.entity.ExerciseEntryEntity
import com.defat.core.data.db.entity.FoodItemEntity
import com.defat.core.data.db.entity.MealEntity
import com.defat.core.data.db.entity.WaterLogEntity
import com.defat.core.data.db.entity.WeightEntryEntity
import com.defat.core.domain.sync.SyncDoc
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

/**
 * Room entity -> Firestore document -> Room entity. A pulled row that does not
 * round-trip would silently corrupt a second device, so every collection is
 * checked, including the nullable fields and the values Firestore hands back as
 * a different numeric type than was written.
 */
class SyncDocMappersTest {

    private val meal = MealEntity(
        id = "m1",
        loggedAtMillis = 1_700_000_000_000L,
        date = "2026-03-05",
        name = "雞胸飯",
        kcal = 520.5,
        proteinG = 45.0,
        carbsG = 50.0,
        fatG = 8.0,
        source = "MANUAL",
        updatedAtMillis = 1_700_000_000_111L,
        mealType = "LUNCH",
        baseFoodId = "cha_siu_rice",
        modifierIds = "rice_less,sauce_less",
    )

    @Test
    fun `a meal round-trips and comes back already synced`() {
        val doc = meal.toSyncDoc()
        assertEquals("m1", doc.id)
        assertEquals(1_700_000_000_111L, doc.updatedAt)
        assertNull(doc.deletedAt)

        val back = doc.toMealEntityOrNull()!!
        assertEquals(meal.copy(syncedAtMillis = meal.updatedAtMillis, pendingSync = false), back)
    }

    @Test
    fun `a typed meal keeps its null food link and modifiers`() {
        val typed = meal.copy(baseFoodId = null, modifierIds = null)
        val back = typed.toSyncDoc().toMealEntityOrNull()!!
        assertNull(back.baseFoodId)
        assertNull(back.modifierIds)
    }

    @Test
    fun `a tombstoned meal carries its deletion across`() {
        val deleted = meal.copy(deletedAtMillis = 1_700_000_999_000L, updatedAtMillis = 1_700_000_999_000L)
        val doc = deleted.toSyncDoc()
        assertEquals(1_700_000_999_000L, doc.deletedAt)
        assertEquals(1_700_000_999_000L, doc.toMealEntityOrNull()!!.deletedAtMillis)
    }

    @Test
    fun `numbers survive Firestore returning a Long for a whole Double`() {
        val doc = meal.toSyncDoc()
        val widened = doc.copy(fields = doc.fields + mapOf("kcal" to 520L, "proteinG" to 45L, "loggedAt" to 1_700_000_000_000.0))
        val back = widened.toMealEntityOrNull()!!
        assertEquals(520.0, back.kcal)
        assertEquals(45.0, back.proteinG)
        assertEquals(1_700_000_000_000L, back.loggedAtMillis)
    }

    @Test
    fun `a meal document missing a required field is skipped, not half-stored`() {
        val doc = meal.toSyncDoc()
        assertNull(doc.copy(fields = doc.fields - "kcal").toMealEntityOrNull())
        assertNull(doc.copy(fields = doc.fields + ("kcal" to "lots")).toMealEntityOrNull())
        assertNull(doc.copy(fields = emptyMap()).toMealEntityOrNull())
    }

    @Test
    fun `a weight entry round-trips with its date as the id`() {
        val weight = WeightEntryEntity(
            date = "2026-03-05",
            weightKg = 79.4,
            bodyFatPct = null,
            recordedAtMillis = 1_700_000_000_000L,
            source = "MANUAL",
            updatedAtMillis = 1_700_000_000_333L,
        )
        val doc = weight.toSyncDoc()
        assertEquals("2026-03-05", doc.id)
        assertEquals(weight.copy(syncedAtMillis = 1_700_000_000_333L, pendingSync = false), doc.toWeightEntityOrNull())

        val withFat = weight.copy(bodyFatPct = 24.1)
        assertEquals(24.1, withFat.toSyncDoc().toWeightEntityOrNull()!!.bodyFatPct)
    }

    @Test
    fun `an exercise entry round-trips including a null note`() {
        val exercise = ExerciseEntryEntity(
            id = "e1",
            loggedAtMillis = 1_700_010_000_000L,
            activityKey = "running",
            met = 9.8,
            minutes = 30,
            weightKgAtTime = 79.4,
            kcal = 389.0,
            note = null,
            updatedAtMillis = 1_700_010_000_500L,
        )
        assertEquals(
            exercise.copy(syncedAtMillis = 1_700_010_000_500L),
            exercise.toSyncDoc().toExerciseEntityOrNull(),
        )
        assertEquals(30, exercise.toSyncDoc().copy(fields = exercise.toSyncDoc().fields + ("minutes" to 30L)).toExerciseEntityOrNull()!!.minutes)
    }

    @Test
    fun `a water day uses the epoch day as its id`() {
        val water = WaterLogEntity(dateEpochDay = 19_666L, ml = 1800, updatedAtMillis = 1_699_222_400_000L)
        val doc = water.toSyncDoc()
        assertEquals("19666", doc.id)
        assertEquals(water.copy(syncedAtMillis = water.updatedAtMillis), doc.toWaterEntityOrNull())
        assertNull(doc.copy(id = "not-a-day").toWaterEntityOrNull())
    }

    @Test
    fun `an owner-edited food row round-trips as an override and stays flagged as edited`() {
        val food = FoodItemEntity(
            id = "cha_siu_rice",
            nameZh = "叉燒飯",
            nameEn = "BBQ pork rice",
            portionLabel = "1 碟",
            portionGrams = null,
            kcal = 880.0,
            proteinG = null,
            carbsG = null,
            fatG = null,
            kcalLow = null,
            kcalHigh = null,
            source = "TRAINER",
            confidence = "HIGH",
            category = "ROAST_RICE",
            isSeed = true,
            editedByOwner = true,
            updatedAtMillis = 1_700_020_000_000L,
        )
        val back = food.toSyncDoc().toFoodOverrideEntityOrNull()!!
        assertEquals(food.copy(syncedAtMillis = 1_700_020_000_000L), back)
        assertEquals(true, back.editedByOwner)
    }

    @Test
    fun `no document carries a field that would let the payload name its owner`() {
        // Ownership is the path (phase1.5 plan §2); a userId/uid/owner field in
        // a payload would invite a rule that trusts it.
        val docs: List<SyncDoc> = listOf(
            meal.toSyncDoc(),
            WaterLogEntity(1, 1, 1).toSyncDoc(),
        )
        docs.forEach { doc ->
            assertEquals(emptySet(), doc.fields.keys.filter { it.lowercase() in setOf("uid", "userid", "owner", "ownerid") }.toSet())
        }
    }
}

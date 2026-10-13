package com.defat.core.domain.calc

import com.defat.core.domain.fake.chaSiuRice
import com.defat.core.domain.fake.noSauce
import com.defat.core.domain.model.FoodConfidence
import com.defat.core.domain.model.FoodSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class FoodOwnerEditTest {

    @Test
    fun `an owner edit makes the row theirs and clears the range`() {
        val seed = chaSiuRice().copy(
            source = FoodSource.MEDIA,
            confidence = FoodConfidence.LOW,
            kcalLow = 850.0,
            kcalHigh = 970.0,
            isSeed = true,
        )

        val edited = FoodOwnerEdit.editItem(seed, kcal = 880.0, proteinG = 45.0, carbsG = null, fatG = 28.0)

        assertEquals(880.0, edited.kcal, 0.0)
        assertEquals(45.0, edited.proteinG!!, 0.0)
        assertNull(edited.carbsG)
        assertEquals(FoodSource.TRAINER, edited.source)
        assertEquals(FoodConfidence.HIGH, edited.confidence)
        assertTrue(edited.editedByOwner)
        assertNull(edited.kcalLow)
        assertNull(edited.kcalHigh)
        // Still a seed row by origin, so a later update knows to leave it alone.
        assertTrue(edited.isSeed)
        assertEquals(seed.id, edited.id)
    }

    @Test
    fun `a non-positive kcal is rejected`() {
        assertFailsWith<IllegalArgumentException> { FoodOwnerEdit.editItem(chaSiuRice(), 0.0, null, null, null) }
        assertFailsWith<IllegalArgumentException> { FoodOwnerEdit.editItem(chaSiuRice(), Double.NaN, null, null, null) }
    }

    @Test
    fun `an owner edit of a modifier delta is permanent like a food row`() {
        val edited = FoodOwnerEdit.editModifier(noSauce, kcalDelta = -85.0, proteinDelta = null, carbsDelta = -4.0, fatDelta = -9.0)

        assertEquals(-85.0, edited.kcalDelta, 0.0)
        assertEquals(-4.0, edited.carbsDelta!!, 0.0)
        assertEquals(FoodSource.TRAINER, edited.source)
        assertEquals(FoodConfidence.HIGH, edited.confidence)
        assertTrue(edited.editedByOwner)
        assertEquals(noSauce.groupId, edited.groupId)
    }
}

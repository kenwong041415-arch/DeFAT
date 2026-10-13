package com.defat.core.domain.repository

import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import kotlinx.coroutines.flow.Flow

/** The 食物庫: seeded, owner-editable food rows and their modifiers (docs/07). */
interface FoodRepository {
    /** Every food row, grouped by category then name. */
    val items: Flow<List<FoodItem>>

    /** Every modifier row. */
    val modifiers: Flow<List<FoodModifier>>

    suspend fun itemById(id: String): FoodItem?

    /**
     * Replaces the row with the same id. This is the owner-edit path — it must
     * only be handed rows built by FoodOwnerEdit. Re-seeding after an app
     * update must never go through here (see FoodDao).
     */
    suspend fun upsertItem(item: FoodItem)

    suspend fun upsertModifier(modifier: FoodModifier)
}

package com.defat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.defat.core.data.db.entity.FoodItemEntity
import com.defat.core.data.db.entity.FoodModifierEntity
import kotlinx.coroutines.flow.Flow

/**
 * The 食物庫 tables.
 *
 * **Re-seeding rule — read before writing MIGRATION_3_4 or later.** A later
 * migration that corrects a seed row must only touch rows the owner has not
 * edited:
 *
 * ```
 * UPDATE food_items SET ... WHERE id = :id AND editedByOwner = 0
 * ```
 *
 * An owner edit is permanent (docs/07 §2 rule 2). Never `INSERT OR REPLACE`
 * over these tables, and never `DELETE` and re-insert: either one silently
 * destroys the owner's corrections. The same applies to `food_modifiers`
 * (docs/07 §6.3 rule 4).
 */
@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items")
    fun observeItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_modifiers")
    fun observeModifiers(): Flow<List<FoodModifierEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun itemById(id: String): FoodItemEntity?

    /** The owner-edit path: replaces the row with this id. Not for re-seeding (see above). */
    @Upsert
    suspend fun upsertItem(item: FoodItemEntity)

    @Upsert
    suspend fun upsertModifier(modifier: FoodModifierEntity)
}

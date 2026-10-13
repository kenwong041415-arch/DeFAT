package com.defat.core.data.repository

import com.defat.core.data.db.FoodDao
import com.defat.core.data.di.IoDispatcher
import com.defat.core.data.mapper.toDomain
import com.defat.core.data.mapper.toEntity
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.repository.FoodRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val foodDao: FoodDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : FoodRepository {

    // Grouped by category (enum order), then by name, in Kotlin rather than SQL:
    // the order is a product decision and a collation-dependent ORDER BY would
    // put it at the mercy of the device locale.
    override val items: Flow<List<FoodItem>> =
        foodDao.observeItems()
            .map { entities ->
                entities.map { it.toDomain() }
                    .sortedWith(compareBy<FoodItem>({ it.category.ordinal }, { it.nameZh }, { it.id }))
            }
            .flowOn(io)

    override val modifiers: Flow<List<FoodModifier>> =
        foodDao.observeModifiers()
            .map { entities -> entities.map { it.toDomain() }.sortedBy { it.id } }
            .flowOn(io)

    override suspend fun itemById(id: String): FoodItem? = withContext(io) {
        foodDao.itemById(id)?.toDomain()
    }

    override suspend fun upsertItem(item: FoodItem) = withContext(io) {
        foodDao.upsertItem(item.toEntity())
    }

    override suspend fun upsertModifier(modifier: FoodModifier) = withContext(io) {
        foodDao.upsertModifier(modifier.toEntity())
    }
}

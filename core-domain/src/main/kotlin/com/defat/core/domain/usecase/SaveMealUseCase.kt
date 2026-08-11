package com.defat.core.domain.usecase

import com.defat.core.domain.model.Meal
import com.defat.core.domain.repository.MealRepository
import javax.inject.Inject

class SaveMealUseCase @Inject constructor(
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(meal: Meal) {
        mealRepository.upsert(meal)
    }
}

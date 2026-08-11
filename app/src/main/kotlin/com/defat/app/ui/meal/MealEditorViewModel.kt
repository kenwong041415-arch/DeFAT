package com.defat.app.ui.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.calc.MealTypeInference
import com.defat.core.domain.calc.FrequentFoodRanker
import com.defat.core.domain.calc.NutritionConstants
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.FrequentFood
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealType
import com.defat.core.domain.repository.MealRepository
import com.defat.core.domain.usecase.SaveMealUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mismatch warning fires when entered kcal differs from the macro sum by more than this (plan §8.2 S8). */
private const val MACRO_MISMATCH_THRESHOLD = 0.15

/** How many of the most recent meals to scan when ranking frequent foods (plan §8.3). */
private const val FREQUENT_FOOD_SCAN_LIMIT = 200

@HiltViewModel
class MealEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository,
    private val saveMealUseCase: SaveMealUseCase,
) : ViewModel() {

    private val mealId: String? = savedStateHandle.get<String>("mealId")

    private val now: LocalTime = LocalTime.now()

    private val _uiState = MutableStateFlow(
        MealEditorUiState(
            mealId = mealId,
            isEditMode = mealId != null,
            time = now,
            date = LocalDate.now(),
            mealType = MealTypeInference.forTime(now),
        ),
    )
    val uiState: StateFlow<MealEditorUiState> = _uiState.asStateFlow()

    private val events = Channel<MealEditorEvent>(Channel.BUFFERED)
    val eventFlow: Flow<MealEditorEvent> = events.receiveAsFlow()

    init {
        val id = mealId
        if (id != null) {
            viewModelScope.launch {
                val meal = mealRepository.mealById(id)
                _uiState.update { state ->
                    if (meal == null) {
                        state.copy(isLoading = false)
                    } else {
                        state.copy(
                            nameText = meal.name,
                            kcalText = meal.kcal.toString(),
                            proteinText = meal.proteinG.toString(),
                            carbsText = meal.carbsG.toString(),
                            fatText = meal.fatG.toString(),
                            time = meal.loggedAt.atZone(ZoneId.systemDefault()).toLocalTime(),
                            date = meal.date,
                            mealType = meal.mealType,
                            mealTypeManuallySet = true,
                            isLoading = false,
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }

        viewModelScope.launch {
            mealRepository.recentMeals(FREQUENT_FOOD_SCAN_LIMIT).collect { recent ->
                _uiState.update { it.copy(frequentFoods = FrequentFoodRanker.rank(recent)) }
            }
        }
    }

    fun setName(text: String) = update { it.copy(nameText = text) }
    fun setKcal(text: String) = update { it.copy(kcalText = text, kcalTouched = true) }
    fun setProtein(text: String) = update { it.copy(proteinText = text) }
    fun setCarbs(text: String) = update { it.copy(carbsText = text) }
    fun setFat(text: String) = update { it.copy(fatText = text) }
    fun setDate(date: LocalDate) = update { it.copy(date = date) }

    fun setTime(time: LocalTime) = update { state ->
        state.copy(
            time = time,
            mealType = if (state.mealTypeManuallySet) state.mealType else MealTypeInference.forTime(time),
        )
    }

    fun setMealType(type: MealType) = update { it.copy(mealType = type, mealTypeManuallySet = true) }

    /** Fills the name and numbers only — the user's current meal-type choice is left alone (plan §5.5/E9). */
    fun applyFrequentFood(food: FrequentFood) = update {
        it.copy(
            nameText = food.name,
            kcalText = food.kcal.roundKcal().toString(),
            proteinText = food.proteinG.roundGrams().toString(),
            carbsText = food.carbsG.roundGrams().toString(),
            fatText = food.fatG.roundGrams().toString(),
            kcalTouched = true,
        )
    }

    fun calculateKcalFromMacros() = update { state ->
        val kcal = NutritionConstants.KCAL_PER_G_PROTEIN * state.protein +
            NutritionConstants.KCAL_PER_G_CARB * state.carbs +
            NutritionConstants.KCAL_PER_G_FAT * state.fat
        state.copy(kcalText = kcal.toString())
    }

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirm = true) }
    fun cancelDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun save() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return
        val meal = Meal(
            id = state.mealId ?: UUID.randomUUID().toString(),
            loggedAt = state.date.atTime(state.time).atZone(ZoneId.systemDefault()).toInstant(),
            date = state.date,
            name = state.nameText.trim(),
            kcal = state.kcal ?: 0.0,
            proteinG = state.protein,
            carbsG = state.carbs,
            fatG = state.fat,
            mealType = state.mealType,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            saveMealUseCase(meal)
            _uiState.update { it.copy(isSaving = false) }
            events.send(MealEditorEvent.Done)
        }
    }

    fun confirmDelete() {
        val id = _uiState.value.mealId ?: return
        viewModelScope.launch {
            mealRepository.delete(id)
            events.send(MealEditorEvent.Done)
        }
    }

    private fun update(transform: (MealEditorUiState) -> MealEditorUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(showMacroMismatchWarning = hasMacroMismatch(next))
        }
    }

    private fun hasMacroMismatch(state: MealEditorUiState): Boolean {
        val kcal = state.kcal ?: return false
        val macroSum = NutritionConstants.KCAL_PER_G_PROTEIN * state.protein +
            NutritionConstants.KCAL_PER_G_CARB * state.carbs +
            NutritionConstants.KCAL_PER_G_FAT * state.fat
        if (macroSum <= 0.0) return false
        return abs(kcal - macroSum) / macroSum > MACRO_MISMATCH_THRESHOLD
    }
}

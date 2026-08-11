package com.defat.app.ui.meal

import java.time.LocalDate
import java.time.LocalTime

data class MealEditorUiState(
    val mealId: String? = null,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,

    val nameText: String = "",
    val kcalText: String = "",
    val proteinText: String = "",
    val carbsText: String = "",
    val fatText: String = "",
    val time: LocalTime = LocalTime.now(),
    val date: LocalDate = LocalDate.now(),

    val showMacroMismatchWarning: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
    val kcal: Double? get() = kcalText.toDoubleOrNull()
    val protein: Double get() = proteinText.toDoubleOrNull() ?: 0.0
    val carbs: Double get() = carbsText.toDoubleOrNull() ?: 0.0
    val fat: Double get() = fatText.toDoubleOrNull() ?: 0.0

    val isNameValid: Boolean get() = nameText.isNotBlank()

    val isSaveEnabled: Boolean
        get() {
            val kcalValue = kcal
            return isNameValid && kcalValue != null && kcalValue >= 0.0 &&
                protein >= 0.0 && carbs >= 0.0 && fat >= 0.0
        }
}

sealed interface MealEditorEvent {
    data object Done : MealEditorEvent
}

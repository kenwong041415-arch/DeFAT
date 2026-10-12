package com.defat.app.ui.meal

import com.defat.core.domain.model.FrequentFood
import com.defat.core.domain.model.MealType
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
    val mealType: MealType = MealType.SNACK,
    /** Once the user picks a chip, setTime() stops re-inferring (plan §2 E12). */
    val mealTypeManuallySet: Boolean = false,
    val kcalTouched: Boolean = false,
    val frequentFoods: List<FrequentFood> = emptyList(),

    val showMacroMismatchWarning: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
    val kcal: Double? get() = kcalText.toDoubleOrNull()
    val protein: Double get() = proteinText.toDoubleOrNull() ?: 0.0
    val carbs: Double get() = carbsText.toDoubleOrNull() ?: 0.0
    val fat: Double get() = fatText.toDoubleOrNull() ?: 0.0

    val isNameValid: Boolean get() = nameText.isNotBlank()

    /**
     * Macros stay optional; calories do not. A 0-kcal meal made Home read
     * "已食 0 / 2267" and look broken, so it is not saveable.
     */
    val isKcalValid: Boolean get() = (kcal ?: 0.0) > 0.0

    /** Gated on [kcalTouched] so a freshly opened "add meal" screen is not
     * red before the owner has typed anything. */
    val showKcalError: Boolean get() = kcalTouched && !isKcalValid

    val isSaveEnabled: Boolean
        get() = isNameValid && isKcalValid && protein >= 0.0 && carbs >= 0.0 && fat >= 0.0
}

sealed interface MealEditorEvent {
    data object Done : MealEditorEvent
}

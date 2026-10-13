package com.defat.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.calc.FoodOwnerEdit
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The open edit dialog: one food row or one modifier row, with its numbers as typed text. */
data class FoodEditForm(
    val isModifier: Boolean,
    val id: String,
    val kcalText: String,
    val proteinText: String,
    val carbsText: String,
    val fatText: String,
) {
    /** A blank macro is "no number" (null); anything else must be a finite number. */
    private fun macroOrNull(text: String): Result<Double?> =
        if (text.isBlank()) {
            Result.success(null)
        } else {
            val value = text.trim().toDoubleOrNull()
            if (value != null && value.isFinite() && (isModifier || value >= 0.0)) {
                Result.success(value)
            } else {
                Result.failure(IllegalArgumentException("not a number"))
            }
        }

    val kcal: Double?
        get() = kcalText.trim().toDoubleOrNull()?.takeIf { it.isFinite() && (isModifier || it > 0.0) }

    val isValid: Boolean
        get() = kcal != null &&
            macroOrNull(proteinText).isSuccess &&
            macroOrNull(carbsText).isSuccess &&
            macroOrNull(fatText).isSuccess

    val protein: Double? get() = macroOrNull(proteinText).getOrNull()
    val carbs: Double? get() = macroOrNull(carbsText).getOrNull()
    val fat: Double? get() = macroOrNull(fatText).getOrNull()
}

data class FoodEditorUiState(
    val items: List<FoodItem> = emptyList(),
    val modifiers: List<FoodModifier> = emptyList(),
    val editing: FoodEditForm? = null,
)

/**
 * The owner's 食物庫 editor (plan §8.5, §16 step 16). An edit writes
 * `source = TRAINER`, `confidence = HIGH`, `editedByOwner = true` and clears the
 * published range — all of it in :core-domain's [FoodOwnerEdit], so the rule has
 * one home. Modifier deltas are edited the same way (docs/07 §6.3 rule 4).
 *
 * Visible to every user in Phase 1.7: there is no trainer role yet (Phase 5).
 * When there is one, it is a single guard on the navigation entry that opens
 * this screen.
 */
@HiltViewModel
class FoodLibraryEditorViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val editing = MutableStateFlow<FoodEditForm?>(null)

    val uiState: StateFlow<FoodEditorUiState> = combine(
        foodRepository.items,
        foodRepository.modifiers,
        editing,
    ) { items, modifiers, form -> FoodEditorUiState(items, modifiers, form) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodEditorUiState())

    fun startEditItem(item: FoodItem) {
        editing.value = FoodEditForm(
            isModifier = false,
            id = item.id,
            kcalText = formatNumber(item.kcal),
            proteinText = item.proteinG?.let(::formatNumber).orEmpty(),
            carbsText = item.carbsG?.let(::formatNumber).orEmpty(),
            fatText = item.fatG?.let(::formatNumber).orEmpty(),
        )
    }

    fun startEditModifier(modifier: FoodModifier) {
        editing.value = FoodEditForm(
            isModifier = true,
            id = modifier.id,
            kcalText = formatNumber(modifier.kcalDelta),
            proteinText = modifier.proteinDelta?.let(::formatNumber).orEmpty(),
            carbsText = modifier.carbsDelta?.let(::formatNumber).orEmpty(),
            fatText = modifier.fatDelta?.let(::formatNumber).orEmpty(),
        )
    }

    fun setKcal(text: String) = editing.update { it?.copy(kcalText = text) }
    fun setProtein(text: String) = editing.update { it?.copy(proteinText = text) }
    fun setCarbs(text: String) = editing.update { it?.copy(carbsText = text) }
    fun setFat(text: String) = editing.update { it?.copy(fatText = text) }

    fun cancel() {
        editing.value = null
    }

    fun save() {
        val form = editing.value ?: return
        val kcal = form.kcal
        if (!form.isValid || kcal == null) return
        viewModelScope.launch {
            if (form.isModifier) {
                val original = foodRepository.modifiers.first().firstOrNull { it.id == form.id } ?: return@launch
                foodRepository.upsertModifier(
                    FoodOwnerEdit.editModifier(original, kcal, form.protein, form.carbs, form.fat),
                )
            } else {
                val original = foodRepository.itemById(form.id) ?: return@launch
                foodRepository.upsertItem(
                    FoodOwnerEdit.editItem(original, kcal, form.protein, form.carbs, form.fat),
                )
            }
            editing.value = null
        }
    }

    /** 880.0 -> "880", 12.5 -> "12.5": what a person would type into the field. */
    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

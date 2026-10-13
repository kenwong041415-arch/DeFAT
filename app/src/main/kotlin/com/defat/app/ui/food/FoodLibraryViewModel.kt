package com.defat.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.calc.FoodSearch
import com.defat.core.domain.calc.ModifiedFoodBreakdown
import com.defat.core.domain.calc.ModifiedFoodCalculator
import com.defat.core.domain.calc.ModifierGroup
import com.defat.core.domain.calc.ModifierGroups
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier
import com.defat.core.domain.model.MacroTotals
import com.defat.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The 點食法 step for one dish (plan §8.6). Every figure on screen is in [breakdown]. */
data class AdjustState(
    val item: FoodItem,
    val groups: List<ModifierGroup>,
    /** groupId -> chosen modifier id; a group that is absent is on its default option. */
    val selected: Map<String, String>,
    val quantity: Int,
    /** Dim sum is counted in 件 with a stepper (docs/07 §6.2 rule 3). */
    val showQuantity: Boolean,
    val breakdown: ModifiedFoodBreakdown,
)

data class FoodLibraryUiState(
    val query: String = "",
    val category: FoodCategory? = null,
    val results: List<FoodItem> = emptyList(),
    /** Non-null while the 點食法 step is showing instead of the search list. */
    val adjust: AdjustState? = null,
)

/**
 * The 搵食物 sheet (plan §8.4) and its 點食法 step (§8.6). Search and
 * "do I need the adjust step" are pure domain functions; this class only holds
 * what the student has typed and tapped.
 */
@HiltViewModel
class FoodLibraryViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private data class Draft(val itemId: String, val selected: Map<String, String>, val quantity: Int)

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<FoodCategory?>(null)
    private val draft = MutableStateFlow<Draft?>(null)

    private val events = Channel<FoodSelection>(Channel.BUFFERED)

    /** Emits once when the student confirms a choice; the editor fills itself from it. */
    val selections: Flow<FoodSelection> = events.receiveAsFlow()

    val uiState: StateFlow<FoodLibraryUiState> = combine(
        foodRepository.items,
        foodRepository.modifiers,
        query,
        category,
        draft,
    ) { items, modifiers, queryText, selectedCategory, currentDraft ->
        FoodLibraryUiState(
            query = queryText,
            category = selectedCategory,
            results = FoodSearch.filter(items, queryText, selectedCategory),
            adjust = currentDraft?.let { buildAdjust(it, items, modifiers) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodLibraryUiState())

    fun setQuery(text: String) {
        query.value = text
    }

    /** Tapping the chip that is already selected clears the filter. */
    fun setCategory(selected: FoodCategory?) {
        category.value = if (selected == category.value) null else selected
    }

    /** Clears the search and any open adjust step; called when the sheet is opened. */
    fun reset() {
        query.value = ""
        category.value = null
        draft.value = null
    }

    /**
     * A row was tapped. Opens 點食法 when the row has modifier groups (or is
     * dim sum); otherwise it is chosen directly, with an unexplained range
     * pre-filled as its midpoint (plan §8.4 items 5 and 6).
     */
    fun onItemTapped(item: FoodItem) {
        viewModelScope.launch {
            val modifiers = foodRepository.modifiers.first()
            if (ModifierGroups.needsAdjustStep(item, modifiers)) {
                draft.value = Draft(item.id, emptyMap(), 1)
            } else {
                events.send(
                    FoodSelection(
                        item = item,
                        modifiers = emptyList(),
                        quantity = 1,
                        totals = MacroTotals(
                            kcal = FoodSearch.prefillKcal(item),
                            proteinG = item.proteinG,
                            carbsG = item.carbsG,
                            fatG = item.fatG,
                        ),
                    ),
                )
            }
        }
    }

    /**
     * Re-opens 點食法 for a meal that was logged from the library, with its
     * modifiers pre-selected, so it can be re-adjusted rather than retyped.
     * Does nothing if the row no longer exists.
     */
    fun startAdjust(baseFoodId: String, modifierIds: List<String>) {
        viewModelScope.launch {
            val item = foodRepository.itemById(baseFoodId) ?: return@launch
            val modifiers = foodRepository.modifiers.first()
            val chosen = ModifierGroups.resolve(item, modifiers, modifierIds)
            draft.value = Draft(item.id, chosen.associate { it.groupId to it.id }, 1)
        }
    }

    /** [modifierId] = null puts the group back on its default option (正常 / 半肥瘦 / 無). */
    fun selectModifier(groupId: String, modifierId: String?) {
        draft.update { current ->
            current?.copy(
                selected = if (modifierId == null) current.selected - groupId else current.selected + (groupId to modifierId),
            )
        }
    }

    fun setQuantity(quantity: Int) {
        draft.update { current -> current?.copy(quantity = quantity.coerceIn(MIN_QUANTITY, MAX_QUANTITY)) }
    }

    fun closeAdjust() {
        draft.value = null
    }

    /** 記錄: hands the adjusted dish to the meal editor. */
    fun confirmAdjust() {
        val adjust = uiState.value.adjust ?: return
        draft.value = null
        viewModelScope.launch {
            events.send(
                FoodSelection(
                    item = adjust.item,
                    modifiers = adjust.breakdown.modifiers,
                    quantity = adjust.quantity,
                    totals = adjust.breakdown.totals,
                ),
            )
        }
    }

    private fun buildAdjust(draft: Draft, items: List<FoodItem>, modifiers: List<FoodModifier>): AdjustState? {
        val item = items.firstOrNull { it.id == draft.itemId } ?: return null
        val groups = ModifierGroups.forItem(item, modifiers)
        // Group order, so the arithmetic reads in the same order as the chips.
        val chosen = groups.mapNotNull { group ->
            draft.selected[group.groupId]?.let { id -> group.options.firstOrNull { it.id == id } }
        }
        return AdjustState(
            item = item,
            groups = groups,
            selected = chosen.associate { it.groupId to it.id },
            quantity = draft.quantity,
            showQuantity = item.category == FoodCategory.DIM_SUM,
            breakdown = ModifiedFoodCalculator.breakdown(item, chosen, draft.quantity),
        )
    }

    companion object {
        const val MIN_QUANTITY: Int = 1
        const val MAX_QUANTITY: Int = 30
    }
}

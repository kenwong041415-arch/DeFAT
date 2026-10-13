package com.defat.app.ui.food

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.component.plainNumber
import com.defat.app.ui.component.signedNumber
import com.defat.core.domain.calc.FoodBadge
import com.defat.core.domain.calc.FoodBadges
import com.defat.core.domain.calc.FoodSearch
import com.defat.core.domain.calc.ModifiedFoodBreakdown
import com.defat.core.domain.calc.ModifierGroup
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.FoodCategory
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FrequentFood

/**
 * The 搵食物 sheet (plan §8.4), which turns into the 點食法 step (§8.6) when a
 * row with modifiers (or a dim sum row) is tapped.
 *
 * Stateless: [FoodLibraryViewModel] owns the state and the meal editor's route
 * owns the sheet's visibility.
 */
@Composable
fun FoodLibrarySheet(
    uiState: FoodLibraryUiState,
    frequentFoods: List<FrequentFood>,
    english: Boolean,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (FoodCategory?) -> Unit,
    onItemTapped: (FoodItem) -> Unit,
    onApplyFrequentFood: (FrequentFood) -> Unit,
    onEnterManually: () -> Unit,
    onSelectModifier: (groupId: String, modifierId: String?) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBackToSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val adjust = uiState.adjust
        if (adjust == null) {
            SearchContent(
                uiState = uiState,
                frequentFoods = frequentFoods,
                english = english,
                onQueryChange = onQueryChange,
                onCategoryChange = onCategoryChange,
                onItemTapped = onItemTapped,
                onApplyFrequentFood = onApplyFrequentFood,
                onEnterManually = onEnterManually,
            )
        } else {
            AdjustContent(
                adjust = adjust,
                english = english,
                onSelectModifier = onSelectModifier,
                onQuantityChange = onQuantityChange,
                onConfirm = onConfirm,
                onBack = onBackToSearch,
            )
        }
    }
}

@Composable
private fun SearchContent(
    uiState: FoodLibraryUiState,
    frequentFoods: List<FrequentFood>,
    english: Boolean,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (FoodCategory?) -> Unit,
    onItemTapped: (FoodItem) -> Unit,
    onApplyFrequentFood: (FrequentFood) -> Unit,
    onEnterManually: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(horizontal = 16.dp),
    ) {
        // 1. Search field, labelled 搵食物 (plan §4).
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.food_find_label)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. 常用 — FrequentFoodRanker's results, unchanged.
        if (frequentFoods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.food_frequent_title), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                frequentFoods.forEach { food ->
                    val addContentDescription = stringResource(R.string.meal_frequent_add, food.name)
                    AssistChip(
                        onClick = { onApplyFrequentFood(food) },
                        label = { Text(stringResource(R.string.meal_frequent_chip, food.name, food.kcal.roundKcal())) },
                        // Plain semantics, never clearAndSetSemantics: that would
                        // clear the chip's click action (Phase 1.6 regression).
                        modifier = Modifier.semantics { contentDescription = addContentDescription },
                    )
                }
            }
        }

        // 3. Category chips.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FoodCategory.entries.forEach { category ->
                FilterChip(
                    selected = uiState.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(foodCategoryLabelRes(category))) },
                )
            }
        }

        // 4. Results, then 7. the always-present manual-entry row.
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (uiState.results.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.food_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(items = uiState.results, key = { it.id }) { item ->
                FoodRow(item = item, english = english, onClick = { onItemTapped(item) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                TextButton(
                    onClick = onEnterManually,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.food_manual_entry))
                }
            }
        }
    }
}

@Composable
private fun FoodRow(item: FoodItem, english: Boolean, onClick: () -> Unit) {
    val badge = FoodBadges.forSource(item.source)
    val low = item.kcalLow
    val high = item.kcalHigh
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.displayName(english),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            // The badge is not decoration (plan §17 R4): it is what stops the
            // app asserting that 西多士 is 522 kcal.
            if (badge != null) {
                FoodBadgeChip(badge)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.kcal_value, FoodSearch.prefillKcal(item).roundKcal()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = item.portionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // A range nobody has explained yet (docs/07 §6.3 rule 2): show it.
        if (low != null && high != null) {
            Text(
                text = stringResource(R.string.food_range_kcal, low.roundKcal(), high.roundKcal()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FoodBadgeChip(badge: FoodBadge, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(foodBadgeLabelRes(badge)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun AdjustContent(
    adjust: AdjustState,
    english: Boolean,
    onSelectModifier: (groupId: String, modifierId: String?) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val item = adjust.item
    val badge = FoodBadges.forSource(item.source)
    val low = item.kcalLow
    val high = item.kcalHigh

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.food_back_to_search))
        }

        // 1. The dish and the baseline it is stored at — what the student is
        // adjusting *from*.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.food_adjust_dish_line, item.displayName(english), item.portionLabel),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) FoodBadgeChip(badge)
        }
        if (low != null && high != null) {
            Text(
                text = stringResource(R.string.food_range_kcal, low.roundKcal(), high.roundKcal()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 2. One row of chips per modifier group, in docs/07 §6.3 order.
        adjust.groups.forEach { group ->
            Spacer(modifier = Modifier.height(16.dp))
            ModifierGroupRow(
                group = group,
                selectedId = adjust.selected[group.groupId],
                english = english,
                onSelect = { modifierId -> onSelectModifier(group.groupId, modifierId) },
            )
        }

        // 3. Quantity, for dim sum.
        if (adjust.showQuantity) {
            Spacer(modifier = Modifier.height(16.dp))
            QuantityStepper(quantity = adjust.quantity, onChange = onQuantityChange)
        }

        // 4. The running total, with its own arithmetic. Not collapsed into one
        // number: this screen scrolls instead.
        Spacer(modifier = Modifier.height(20.dp))
        ArithmeticBlock(breakdown = adjust.breakdown, english = english)

        // 5. 記錄
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.food_log_button))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModifierGroupRow(
    group: ModifierGroup,
    selectedId: String?,
    english: Boolean,
    onSelect: (String?) -> Unit,
) {
    val groupName = modifierGroupLabel(group.groupId)
    val defaultName = stringResource(modifierDefaultLabelRes(group.groupId))

    Column {
        Text(text = groupName, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp)
                // The group name rides on the row so TalkBack can say which
                // group a chip belongs to. Plain semantics: never
                // clearAndSetSemantics, which would clear the chips' clicks.
                .semantics { contentDescription = groupName },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The 正常 / 半肥瘦 / 無 option is not a modifier row: it is "none
            // chosen". It is drawn at the position where the deltas cross zero
            // and is the one selected until the student picks something else.
            for (index in 0..group.options.size) {
                if (index == group.defaultIndex) {
                    OptionChip(
                        groupName = groupName,
                        optionName = defaultName,
                        chipLabel = defaultName,
                        selected = selectedId == null,
                        onClick = { onSelect(null) },
                    )
                }
                val option = group.options.getOrNull(index) ?: continue
                val optionName = option.displayName(english)
                OptionChip(
                    groupName = groupName,
                    optionName = optionName,
                    chipLabel = "$optionName ${signedNumber(option.kcalDelta.roundKcal())}",
                    selected = selectedId == option.id,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    }
}

@Composable
private fun OptionChip(
    groupName: String,
    optionName: String,
    chipLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // 「飯量：少飯，已選」 rather than a bare 「少飯」.
    val description = if (selected) {
        stringResource(R.string.food_option_selected, groupName, optionName)
    } else {
        stringResource(R.string.food_option_unselected, groupName, optionName)
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(chipLabel) },
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    val decreaseDescription = stringResource(R.string.food_quantity_decrease)
    val increaseDescription = stringResource(R.string.food_quantity_increase)
    Column {
        Text(stringResource(R.string.food_quantity_label), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onChange(quantity - 1) },
                enabled = quantity > FoodLibraryViewModel.MIN_QUANTITY,
                modifier = Modifier.semantics { contentDescription = decreaseDescription },
            ) { Text("−") }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp),
            )
            OutlinedButton(
                onClick = { onChange(quantity + 1) },
                enabled = quantity < FoodLibraryViewModel.MAX_QUANTITY,
                modifier = Modifier.semantics { contentDescription = increaseDescription },
            ) { Text("+") }
        }
    }
}

/**
 * The running total, showing its own working (plan §8.6):
 *
 * ```
 *  939  基礎（正常飯、半肥瘦、有汁）
 * −120  瘦
 * −130  少飯
 * −100  走汁
 * ─────
 *  589  kcal
 * ```
 *
 * The owner is a trainer teaching students; seeing 走汁 −100 is the lesson
 * (docs/07 §6.4). Every figure is read from [breakdown]; no arithmetic here.
 */
@Composable
private fun ArithmeticBlock(breakdown: ModifiedFoodBreakdown, english: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.food_arith_heading), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        ArithmeticLine(
            number = breakdown.base.kcal.roundKcal().toString(),
            label = stringResource(R.string.food_arith_base, breakdown.base.portionLabel),
        )
        breakdown.modifiers.forEach { modifier ->
            ArithmeticLine(
                number = signedNumber(modifier.kcalDelta.roundKcal()),
                label = modifier.displayName(english),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline)
        ArithmeticLine(
            number = breakdown.perUnit.kcal.roundKcal().toString(),
            label = stringResource(R.string.unit_kcal),
            emphasised = breakdown.quantity == 1,
        )
        if (breakdown.clampedAtZero) {
            Text(
                text = stringResource(R.string.food_arith_clamped),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (breakdown.quantity > 1) {
            ArithmeticLine(
                number = "× ${breakdown.quantity}",
                label = stringResource(R.string.food_quantity_unit),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline)
            ArithmeticLine(
                number = breakdown.totals.kcal.roundKcal().toString(),
                label = stringResource(R.string.unit_kcal),
                emphasised = true,
            )
        }

        // A macro no source published is shown as such, not as a zero.
        Spacer(modifier = Modifier.height(8.dp))
        MacroLine(R.string.macro_protein, breakdown.totals.proteinG)
        MacroLine(R.string.macro_carbs, breakdown.totals.carbsG)
        MacroLine(R.string.macro_fat, breakdown.totals.fatG)
    }
}

@Composable
private fun ArithmeticLine(number: String, label: String, emphasised: Boolean = false) {
    val numberStyle = if (emphasised) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number,
            style = numberStyle,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(88.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MacroLine(@StringRes labelRes: Int, grams: Double?) {
    val label = stringResource(labelRes)
    Text(
        text = if (grams != null) {
            stringResource(R.string.food_macro_value, label, plainNumber(grams, 1))
        } else {
            stringResource(R.string.food_macro_unknown, label)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

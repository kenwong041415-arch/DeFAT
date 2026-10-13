package com.defat.app.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.signedNumber
import com.defat.core.domain.calc.FoodBadge
import com.defat.core.domain.calc.FoodBadges
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.FoodItem
import com.defat.core.domain.model.FoodModifier

@Composable
fun FoodLibraryEditorRoute(
    onBack: () -> Unit,
    viewModel: FoodLibraryEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val english = LocalConfiguration.current.locales[0].language != "zh"
    FoodLibraryEditorScreen(
        uiState = uiState,
        english = english,
        onBack = onBack,
        onEditItem = viewModel::startEditItem,
        onEditModifier = viewModel::startEditModifier,
        onKcalChange = viewModel::setKcal,
        onProteinChange = viewModel::setProtein,
        onCarbsChange = viewModel::setCarbs,
        onFatChange = viewModel::setFat,
        onSave = viewModel::save,
        onCancel = viewModel::cancel,
    )
}

@Composable
fun FoodLibraryEditorScreen(
    uiState: FoodEditorUiState,
    english: Boolean,
    onBack: () -> Unit,
    onEditItem: (FoodItem) -> Unit,
    onEditModifier: (FoodModifier) -> Unit,
    onKcalChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.food_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.food_editor_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            item {
                Text(stringResource(R.string.food_editor_items_heading), style = MaterialTheme.typography.titleMedium)
            }
            items(items = uiState.items, key = { "item-${it.id}" }) { food ->
                EditorRow(
                    title = food.displayName(english),
                    subtitle = food.portionLabel,
                    value = stringResource(R.string.kcal_value, food.kcal.roundKcal()),
                    badge = FoodBadges.forSource(food.source),
                    onClick = { onEditItem(food) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Text(
                    text = stringResource(R.string.food_editor_modifiers_heading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            items(items = uiState.modifiers, key = { "modifier-${it.id}" }) { modifier ->
                EditorRow(
                    title = modifier.displayName(english),
                    subtitle = modifierGroupLabel(modifier.groupId),
                    // The delta is shown signed: 走汁 −100.
                    value = stringResource(R.string.food_editor_delta_value, signedNumber(modifier.kcalDelta.roundKcal())),
                    badge = FoodBadges.forSource(modifier.source),
                    onClick = { onEditModifier(modifier) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    val form = uiState.editing
    if (form != null) {
        val foundName: String? = if (form.isModifier) {
            uiState.modifiers.firstOrNull { it.id == form.id }?.displayName(english)
        } else {
            uiState.items.firstOrNull { it.id == form.id }?.displayName(english)
        }
        val title = foundName ?: form.id
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.food_editor_edit_title, title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Deltas can be negative, and the numeric keyboard may not offer
                    // a minus sign, so those fields take plain text.
                    val keyboard = if (form.isModifier) KeyboardType.Text else KeyboardType.Decimal
                    DecimalField(
                        value = form.kcalText,
                        onValueChange = onKcalChange,
                        label = stringResource(if (form.isModifier) R.string.food_editor_kcal_delta else R.string.meal_kcal),
                        keyboardType = keyboard,
                    )
                    DecimalField(
                        value = form.proteinText,
                        onValueChange = onProteinChange,
                        label = stringResource(if (form.isModifier) R.string.food_editor_protein_delta else R.string.meal_protein),
                        keyboardType = keyboard,
                    )
                    DecimalField(
                        value = form.carbsText,
                        onValueChange = onCarbsChange,
                        label = stringResource(if (form.isModifier) R.string.food_editor_carbs_delta else R.string.meal_carbs),
                        keyboardType = keyboard,
                    )
                    DecimalField(
                        value = form.fatText,
                        onValueChange = onFatChange,
                        label = stringResource(if (form.isModifier) R.string.food_editor_fat_delta else R.string.meal_fat),
                        keyboardType = keyboard,
                    )
                    Text(
                        text = stringResource(R.string.food_editor_blank_means_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onSave, enabled = form.isValid) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun EditorRow(
    title: String,
    subtitle: String,
    value: String,
    badge: FoodBadge?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (badge != null) {
            FoodBadgeChip(badge)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

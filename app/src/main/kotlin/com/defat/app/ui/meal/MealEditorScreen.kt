package com.defat.app.ui.meal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.mealTypeLabel
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.FrequentFood
import com.defat.core.domain.model.MealType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MealEditorRoute(
    mealId: String?,
    onDone: () -> Unit,
    viewModel: MealEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            if (event is MealEditorEvent.Done) onDone()
        }
    }

    MealEditorScreen(
        uiState = uiState,
        onNameChange = viewModel::setName,
        onKcalChange = viewModel::setKcal,
        onProteinChange = viewModel::setProtein,
        onCarbsChange = viewModel::setCarbs,
        onFatChange = viewModel::setFat,
        onMealTypeChange = viewModel::setMealType,
        onDateSelected = viewModel::setDate,
        onTimeSelected = viewModel::setTime,
        onApplyFrequentFood = viewModel::applyFrequentFood,
        onCalculateKcalFromMacros = viewModel::calculateKcalFromMacros,
        onSave = viewModel::save,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onBack = onDone,
    )
}

@Composable
fun MealEditorScreen(
    uiState: MealEditorUiState,
    onNameChange: (String) -> Unit,
    onKcalChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onApplyFrequentFood: (FrequentFood) -> Unit,
    onCalculateKcalFromMacros: () -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditMode) R.string.meal_title_edit else R.string.meal_title_add,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = onRequestDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.meal_type_label), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.mealType == type,
                        onClick = { onMealTypeChange(type) },
                        label = { Text(mealTypeLabel(type)) },
                    )
                }
            }

            if (uiState.frequentFoods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(stringResource(R.string.meal_frequent_title), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.frequentFoods.forEach { food ->
                        val addContentDescription = stringResource(R.string.meal_frequent_add, food.name)
                        AssistChip(
                            onClick = { onApplyFrequentFood(food) },
                            label = {
                                Text(stringResource(R.string.meal_frequent_chip, food.name, food.kcal.roundKcal()))
                            },
                            modifier = Modifier.semantics {
                                contentDescription = addContentDescription
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            DecimalField(
                value = uiState.nameText,
                onValueChange = onNameChange,
                label = stringResource(R.string.meal_name),
                keyboardType = KeyboardType.Text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DecimalField(
                value = uiState.kcalText,
                onValueChange = onKcalChange,
                label = stringResource(R.string.meal_kcal),
                suffix = stringResource(R.string.unit_kcal),
                errorText = if (uiState.showKcalError) stringResource(R.string.meal_kcal_required) else null,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DecimalField(
                value = uiState.proteinText,
                onValueChange = onProteinChange,
                label = stringResource(R.string.meal_protein),
                suffix = stringResource(R.string.unit_g),
            )
            Spacer(modifier = Modifier.height(12.dp))
            DecimalField(
                value = uiState.carbsText,
                onValueChange = onCarbsChange,
                label = stringResource(R.string.meal_carbs),
                suffix = stringResource(R.string.unit_g),
            )
            Spacer(modifier = Modifier.height(12.dp))
            DecimalField(
                value = uiState.fatText,
                onValueChange = onFatChange,
                label = stringResource(R.string.meal_fat),
                suffix = stringResource(R.string.unit_g),
            )

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onCalculateKcalFromMacros) {
                Text(stringResource(R.string.meal_calc_from_macros))
            }

            if (uiState.showMacroMismatchWarning) {
                Text(
                    stringResource(R.string.meal_macro_mismatch),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(stringResource(R.string.common_date), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(uiState.date.toString())
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.meal_time), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(uiState.time.format(DateTimeFormatter.ofPattern("HH:mm")))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSave, enabled = uiState.isSaveEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.time.hour,
            initialMinute = uiState.time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.meal_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MealEditorScreenPreview() {
    DefatTheme {
        MealEditorScreen(
            uiState = MealEditorUiState(
                isLoading = false,
                nameText = "Chicken and rice",
                kcalText = "600",
                proteinText = "30",
                carbsText = "80",
                fatText = "15",
                mealType = MealType.LUNCH,
                frequentFoods = listOf(FrequentFood("雞胸飯", 3, 520.0, 48.0, 52.0, 9.0)),
            ),
            onNameChange = {},
            onKcalChange = {},
            onProteinChange = {},
            onCarbsChange = {},
            onFatChange = {},
            onMealTypeChange = {},
            onDateSelected = {},
            onTimeSelected = {},
            onApplyFrequentFood = {},
            onCalculateKcalFromMacros = {},
            onSave = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onBack = {},
        )
    }
}

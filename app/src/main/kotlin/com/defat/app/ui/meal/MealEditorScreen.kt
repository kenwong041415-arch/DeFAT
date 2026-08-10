package com.defat.app.ui.meal

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.theme.DefatTheme
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
    onCalculateKcalFromMacros: () -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onBack: () -> Unit,
) {
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
            androidx.compose.foundation.layout.Box(
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
            DecimalField(value = uiState.nameText, onValueChange = onNameChange, label = stringResource(R.string.meal_name))
            Spacer(modifier = Modifier.height(12.dp))
            DecimalField(
                value = uiState.kcalText,
                onValueChange = onKcalChange,
                label = stringResource(R.string.meal_kcal),
                suffix = stringResource(R.string.unit_kcal),
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
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSave, enabled = uiState.isSaveEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }
        }
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
            ),
            onNameChange = {},
            onKcalChange = {},
            onProteinChange = {},
            onCarbsChange = {},
            onFatChange = {},
            onCalculateKcalFromMacros = {},
            onSave = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onBack = {},
        )
    }
}

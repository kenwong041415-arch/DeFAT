package com.defat.app.ui.history

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.MacroTileData
import com.defat.app.ui.component.MacroTileRow
import com.defat.app.ui.component.MealTypeSections
import com.defat.app.ui.component.SectionCard
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.TdeeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HistoryRoute(
    onEditMeal: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onEditMeal = onEditMeal,
        onSelectDate = viewModel::selectDate,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onBack = onBack,
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onEditMeal: (String) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            is HistoryUiState.Loading, HistoryUiState.NeedsOnboarding -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HistoryUiState.Content -> {
                HistoryContent(
                    modifier = Modifier.padding(padding),
                    uiState = uiState,
                    onEditMeal = onEditMeal,
                    onSelectDate = onSelectDate,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState.Content,
    onEditMeal: (String) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val rollup = uiState.dayRollup
    val pickDateContentDescription = stringResource(R.string.history_pick_date)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.history_prev_day),
                )
            }
            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.semantics {
                    contentDescription = pickDateContentDescription
                },
            ) {
                Text(uiState.date.toString())
            }
            IconButton(onClick = onNextDay, enabled = uiState.canGoForward) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.history_next_day),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SectionCard(title = stringResource(R.string.history_totals_title)) {
            Text(
                stringResource(
                    R.string.home_eaten_of_target,
                    rollup.intakeKcal.roundKcal(),
                    rollup.target.targetKcal.roundKcal(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MacroTileRow(
                protein = MacroTileData(
                    label = stringResource(R.string.macro_protein),
                    usedG = rollup.proteinG.roundGrams(),
                    targetG = rollup.target.macros.proteinG.roundGrams(),
                    emphasised = true,
                    contentDescription = stringResource(
                        R.string.home_protein_progress,
                        rollup.proteinG.roundGrams(),
                        rollup.target.macros.proteinG.roundGrams(),
                    ),
                ),
                carbs = MacroTileData(
                    label = stringResource(R.string.macro_carbs),
                    usedG = rollup.carbsG.roundGrams(),
                    targetG = rollup.target.macros.carbsG.roundGrams(),
                    emphasised = false,
                    contentDescription = stringResource(
                        R.string.home_carbs_progress,
                        rollup.carbsG.roundGrams(),
                        rollup.target.macros.carbsG.roundGrams(),
                    ),
                ),
                fat = MacroTileData(
                    label = stringResource(R.string.macro_fat),
                    usedG = rollup.fatG.roundGrams(),
                    targetG = rollup.target.macros.fatG.roundGrams(),
                    emphasised = false,
                    contentDescription = stringResource(
                        R.string.home_fat_progress,
                        rollup.fatG.roundGrams(),
                        rollup.target.macros.fatG.roundGrams(),
                    ),
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        MealTypeSections(groups = uiState.mealGroups, onEditMeal = onEditMeal)

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val initialMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onSelectDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
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
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryScreenPreview() {
    val target = DailyTarget(
        bmrKcal = 1666.0,
        bmrMethod = BmrMethod.KATCH_MCARDLE,
        tdeeKcal = 2582.3,
        tdeeSource = TdeeSource.ACTIVITY_MULTIPLIER,
        targetKcal = 2065.84,
        floorApplied = false,
        macros = MacroTargets(proteinG = 132.0, fatG = 48.0, carbsG = 276.46, carbsClamped = false),
    )
    val rollup = DayRollup(
        date = LocalDate.now().minusDays(1),
        target = target,
        intakeKcal = 1050.0,
        proteinG = 70.0,
        carbsG = 110.0,
        fatG = 27.0,
        mealCount = 2,
    )
    DefatTheme {
        HistoryScreen(
            uiState = HistoryUiState.Content(
                date = LocalDate.now().minusDays(1),
                dayRollup = rollup,
                mealGroups = MealGrouping.groupByType(emptyList()),
                canGoForward = true,
            ),
            onEditMeal = {},
            onSelectDate = {},
            onPreviousDay = {},
            onNextDay = {},
            onBack = {},
        )
    }
}

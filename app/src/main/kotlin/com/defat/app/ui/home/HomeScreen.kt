package com.defat.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.CalorieRing
import com.defat.app.ui.component.DefatTab
import com.defat.app.ui.component.DefatTabScaffold
import com.defat.app.ui.component.MacroTileData
import com.defat.app.ui.component.MacroTileRow
import com.defat.app.ui.component.MealTypeSections
import com.defat.app.ui.component.WeekDateStrip
import com.defat.app.ui.component.bmrMethodLabelRes
import com.defat.app.ui.exercise.ExerciseEntryViewModel
import com.defat.app.ui.exercise.ExerciseSheet
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.calc.WeekStrip
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.MealTypeGroup
import com.defat.core.domain.model.TdeeSource
import java.time.LocalDate

@Composable
fun HomeRoute(
    onAddMeal: () -> Unit,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onTabSelected: (DefatTab) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    exerciseViewModel: ExerciseEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exerciseState by exerciseViewModel.uiState.collectAsStateWithLifecycle()
    var showExerciseSheet by rememberSaveable { mutableStateOf(false) }

    // The sheet closes itself once the entry has been written.
    LaunchedEffect(Unit) {
        exerciseViewModel.saved.collect { showExerciseSheet = false }
    }

    HomeScreen(
        uiState = uiState,
        onAddMeal = onAddMeal,
        onEditMeal = onEditMeal,
        onLogWeight = onLogWeight,
        onSelectDay = viewModel::selectDay,
        onPreviousWeek = viewModel::previousWeek,
        onNextWeek = viewModel::nextWeek,
        onAddExercise = {
            exerciseViewModel.reset()
            showExerciseSheet = true
        },
        onDeleteExercise = viewModel::deleteExercise,
        onTapWaterCup = viewModel::tapWaterCup,
        onAddWater = viewModel::addWater,
        onTabSelected = onTabSelected,
    )

    if (showExerciseSheet) {
        ExerciseSheet(
            state = exerciseState,
            onSelectActivity = exerciseViewModel::selectActivity,
            onSelectWeightTrainingLevel = exerciseViewModel::selectWeightTrainingLevel,
            onMinutesChange = exerciseViewModel::setMinutes,
            onSave = exerciseViewModel::save,
            onDismiss = { showExerciseSheet = false },
        )
    }
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddMeal: () -> Unit,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAddExercise: () -> Unit,
    onDeleteExercise: (String) -> Unit,
    onTapWaterCup: (Int) -> Unit,
    onAddWater: (String) -> Boolean,
    onTabSelected: (DefatTab) -> Unit,
) {
    DefatTabScaffold(
        selectedTab = DefatTab.TODAY,
        onTabSelected = onTabSelected,
        onAddMeal = onAddMeal,
        topBar = {
            // The title and the date — nothing else (plan §6.2).
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_title_today))
                        if (uiState is HomeUiState.Content) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.selectedDate.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                // 跟足計劃日數: a count, shown once there is one to show.
                                if (uiState.streakDays > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StreakChip(days = uiState.streakDays)
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            is HomeUiState.Loading, HomeUiState.NeedsOnboarding -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Content -> {
                HomeContent(
                    modifier = Modifier.padding(padding),
                    content = uiState,
                    onEditMeal = onEditMeal,
                    onLogWeight = onLogWeight,
                    onSelectDay = onSelectDay,
                    onPreviousWeek = onPreviousWeek,
                    onNextWeek = onNextWeek,
                    onAddExercise = onAddExercise,
                    onDeleteExercise = onDeleteExercise,
                    onTapWaterCup = onTapWaterCup,
                    onAddWater = onAddWater,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAddExercise: () -> Unit,
    onDeleteExercise: (String) -> Unit,
    onTapWaterCup: (Int) -> Unit,
    onAddWater: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var showMetabolismInfo by remember { mutableStateOf(false) }
    val rollup: DayRollup = content.dayRollup
    val mealGroups: List<MealTypeGroup> = content.mealGroups
    val methodLabel = stringResource(bmrMethodLabelRes(rollup.target.bmrMethod))

    Column(modifier = modifier.fillMaxSize()) {
        WeekDateStrip(
            days = content.weekDays,
            onSelectDay = onSelectDay,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            CalorieRing(
                progressFraction = rollup.progressFraction,
                isOverTarget = rollup.isOverTarget,
                centerNumber = kotlin.math.abs(rollup.remainingKcal).roundKcal().toString(),
                centerLabel = stringResource(
                    if (rollup.isOverTarget) R.string.home_kcal_over else R.string.home_kcal_left,
                ),
                subLabel = stringResource(
                    R.string.home_eaten_of_target,
                    rollup.intakeKcal.roundKcal(),
                    rollup.target.targetKcal.roundKcal(),
                ),
            )

            // 記錄體重 was a drawer row; it is a secondary action under the ring now.
            TextButton(
                onClick = onLogWeight,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.weight_title))
            }

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

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (content.isToday) {
                    stringResource(R.string.home_meals_title)
                } else {
                    stringResource(R.string.home_meals_title_other_day, content.selectedDate.toString())
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (mealGroups.all { it.isEmpty }) {
                Text(
                    stringResource(R.string.home_meals_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MealTypeSections(groups = mealGroups, onEditMeal = onEditMeal)

            SectionDivider()
            ExerciseSection(
                entries = content.exerciseEntries,
                totalKcal = rollup.exerciseKcal,
                onAdd = onAddExercise,
                onDelete = onDeleteExercise,
            )

            SectionDivider()
            WaterSection(
                waterMl = content.waterMl,
                goalMl = content.waterGoalMl,
                onTapCup = onTapWaterCup,
                onAddWater = onAddWater,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.home_metabolism_footnote,
                    rollup.target.bmrKcal.roundKcal(),
                    rollup.target.tdeeKcal.roundKcal(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { showMetabolismInfo = true }
                    .semantics { role = Role.Button },
            )
            // Names the formula in use (plan §5.5) without making the student tap.
            Text(
                text = methodLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Clearance for the centre add button.
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showMetabolismInfo) {
        ModalBottomSheet(onDismissRequest = { showMetabolismInfo = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(stringResource(R.string.home_metabolism_info_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.home_metabolism_info_body))
                Spacer(modifier = Modifier.height(8.dp))
                Text(methodLabel, style = MaterialTheme.typography.titleSmall)
                if (rollup.target.bmrMethod == BmrMethod.HENRY_OXFORD) {
                    Text(
                        stringResource(R.string.bmr_method_note_henry),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
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
        date = LocalDate.now(),
        target = target,
        intakeKcal = 1050.0,
        proteinG = 70.0,
        carbsG = 110.0,
        fatG = 27.0,
        mealCount = 2,
    )
    val today = LocalDate.of(2026, 10, 13)
    DefatTheme {
        HomeScreen(
            uiState = HomeUiState.Content(
                dayRollup = rollup,
                mealGroups = MealGrouping.groupByType(emptyList()),
                selectedDate = today,
                today = today,
                weekDays = WeekStrip.cells(today, today, setOf(today.minusDays(1))),
                previousWeekTarget = today.minusWeeks(1),
                nextWeekTarget = null,
                exerciseEntries = emptyList(),
                waterMl = 1750,
                waterGoalMl = 2800,
                streakDays = 3,
            ),
            onAddMeal = {},
            onEditMeal = {},
            onLogWeight = {},
            onSelectDay = {},
            onPreviousWeek = {},
            onNextWeek = {},
            onAddExercise = {},
            onDeleteExercise = {},
            onTapWaterCup = {},
            onAddWater = { true },
            onTabSelected = {},
        )
    }
}

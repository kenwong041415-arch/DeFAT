package com.defat.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.DefatTab
import com.defat.app.ui.component.DefatTabScaffold
import com.defat.app.ui.component.SectionCard
import com.defat.app.ui.component.bmrMethodLabelRes
import com.defat.app.ui.settings.SettingsViewModel
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.TdeeSource
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ProfileRoute(
    onLogWeight: () -> Unit,
    onOpenFoodLibrary: () -> Unit,
    onOpenSources: () -> Unit,
    onAddMeal: () -> Unit,
    onTabSelected: (DefatTab) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dayStart by settingsViewModel.dayStart.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        dayStart = dayStart,
        dayStartOptions = settingsViewModel.dayStartOptions,
        onDayStartSelected = settingsViewModel::setDayStart,
        onWeightChange = { text -> viewModel.updateForm { it.copy(weightKgText = text) } },
        onHeightChange = { text -> viewModel.updateForm { it.copy(heightCmText = text) } },
        onBodyFatChange = { text -> viewModel.updateForm { it.copy(bodyFatPctText = text) } },
        onGoalWeightChange = { text -> viewModel.updateForm { it.copy(goalTargetWeightText = text) } },
        onSave = viewModel::save,
        onLogWeight = onLogWeight,
        onOpenFoodLibrary = onOpenFoodLibrary,
        onOpenSources = onOpenSources,
        onAddMeal = onAddMeal,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    dayStart: LocalTime,
    dayStartOptions: List<LocalTime>,
    onDayStartSelected: (LocalTime) -> Unit,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onGoalWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    onLogWeight: () -> Unit,
    onOpenFoodLibrary: () -> Unit,
    onOpenSources: () -> Unit,
    onAddMeal: () -> Unit,
    onTabSelected: (DefatTab) -> Unit,
) {
    var showDayStartDialog by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    DefatTabScaffold(
        selectedTab = DefatTab.ME,
        onTabSelected = onTabSelected,
        onAddMeal = onAddMeal,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) },
    ) { padding ->
        when (uiState) {
            ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val target = uiState.dailyTarget
                    if (target != null) {
                        SectionCard(title = stringResource(R.string.summary_target), modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(
                                "${target.targetKcal.roundKcal()} ${stringResource(R.string.unit_kcal)}",
                                style = MaterialTheme.typography.displaySmall,
                            )
                            Text(
                                stringResource(R.string.home_metabolism_line, target.bmrKcal.roundKcal(), target.tdeeKcal.roundKcal()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            // Names the BMR formula in use (plan §5.5).
                            Text(
                                stringResource(bmrMethodLabelRes(target.bmrMethod)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (target.bmrMethod == BmrMethod.HENRY_OXFORD) {
                                Text(
                                    stringResource(R.string.bmr_method_note_henry),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    // 記錄體重 was a drawer row (plan §6.2); it lives here now.
                    OutlinedButton(onClick = onLogWeight, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.weight_title))
                    }

                    // 食物庫 (plan §8.5) and 數據來源 (plan §13).
                    Spacer(modifier = Modifier.height(8.dp))
                    LinkRow(title = stringResource(R.string.food_editor_title), onClick = onOpenFoodLibrary)
                    LinkRow(title = stringResource(R.string.sources_title), onClick = onOpenSources)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { showDayStartDialog = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.day_start_title),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            dayStart.format(timeFormatter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        stringResource(R.string.day_start_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.day_start_helper_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    DecimalField(
                        value = uiState.form.heightCmText,
                        onValueChange = onHeightChange,
                        label = stringResource(R.string.basics_height),
                        suffix = stringResource(R.string.unit_cm),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DecimalField(
                        value = uiState.form.weightKgText,
                        onValueChange = onWeightChange,
                        label = stringResource(R.string.body_weight),
                        suffix = stringResource(R.string.unit_kg),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DecimalField(
                        value = uiState.form.bodyFatPctText,
                        onValueChange = onBodyFatChange,
                        label = stringResource(R.string.body_fat_pct),
                        suffix = "%",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DecimalField(
                        value = uiState.form.goalTargetWeightText,
                        onValueChange = onGoalWeightChange,
                        label = stringResource(R.string.goal_target_weight),
                        suffix = stringResource(R.string.unit_kg),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onSave, enabled = uiState.form.isValid, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_save))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.disclaimer_body), style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.profile_language_hint), style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.profile_version, uiState.appVersion), style = MaterialTheme.typography.labelSmall)

                    // Clearance for the centre add button.
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }

    if (showDayStartDialog) {
        AlertDialog(
            onDismissRequest = { showDayStartDialog = false },
            title = { Text(stringResource(R.string.day_start_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    dayStartOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == dayStart,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onDayStartSelected(option)
                                        showDayStartDialog = false
                                    },
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == dayStart, onClick = null)
                            Text(
                                option.format(timeFormatter),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDayStartDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("\u203A", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    val target = DailyTarget(
        bmrKcal = 1666.0,
        bmrMethod = BmrMethod.KATCH_MCARDLE,
        tdeeKcal = 2582.3,
        tdeeSource = TdeeSource.ACTIVITY_MULTIPLIER,
        targetKcal = 2065.84,
        floorApplied = false,
        macros = MacroTargets(proteinG = 132.0, fatG = 48.0, carbsG = 276.46, carbsClamped = false),
    )
    DefatTheme {
        ProfileScreen(
            uiState = ProfileUiState.Content(
                existingProfile = com.defat.core.domain.model.UserProfile(
                    sex = com.defat.core.domain.model.Sex.MALE,
                    birthDate = java.time.LocalDate.now().minusYears(30),
                    heightCm = 175.0,
                    weightKg = 80.0,
                    bodyFatPct = 25.0,
                    activityLevel = com.defat.core.domain.model.ActivityLevel.MODERATE,
                    goal = com.defat.core.domain.model.Goal(targetWeightKg = 75.0),
                    disclaimerAcceptedAt = java.time.Instant.now(),
                ),
                form = ProfileFormState(
                    heightCmText = "175",
                    weightKgText = "80",
                    bodyFatPctText = "25",
                    goalTargetWeightText = "75",
                ),
                dailyTarget = target,
                appVersion = "0.3.0-phase1.7b",
            ),
            dayStart = LocalTime.of(4, 0),
            dayStartOptions = listOf(LocalTime.of(0, 0), LocalTime.of(4, 0), LocalTime.of(8, 0)),
            onDayStartSelected = {},
            onWeightChange = {},
            onHeightChange = {},
            onBodyFatChange = {},
            onGoalWeightChange = {},
            onSave = {},
            onLogWeight = {},
            onOpenFoodLibrary = {},
            onOpenSources = {},
            onAddMeal = {},
            onTabSelected = {},
        )
    }
}

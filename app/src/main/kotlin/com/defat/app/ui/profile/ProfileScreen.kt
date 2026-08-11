package com.defat.app.ui.profile

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.defat.app.ui.component.SectionCard
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.TdeeSource

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onWeightChange = { text -> viewModel.updateForm { it.copy(weightKgText = text) } },
        onHeightChange = { text -> viewModel.updateForm { it.copy(heightCmText = text) } },
        onBodyFatChange = { text -> viewModel.updateForm { it.copy(bodyFatPctText = text) } },
        onGoalWeightChange = { text -> viewModel.updateForm { it.copy(goalTargetWeightText = text) } },
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onGoalWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
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
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (uiState.dailyTarget != null) {
                        SectionCard(title = stringResource(R.string.summary_target), modifier = Modifier.padding(bottom = 16.dp)) {
                            val target = uiState.dailyTarget
                            Text(
                                "${target.targetKcal.roundKcal()} ${stringResource(R.string.unit_kcal)}",
                                style = MaterialTheme.typography.displaySmall,
                            )
                            Text(
                                stringResource(R.string.home_metabolism_line, target.bmrKcal.roundKcal(), target.tdeeKcal.roundKcal()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

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
                }
            }
        }
    }
}

@Preview(name = "Light")
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
                appVersion = "0.1.0-phase1",
            ),
            onWeightChange = {},
            onHeightChange = {},
            onBodyFatChange = {},
            onGoalWeightChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

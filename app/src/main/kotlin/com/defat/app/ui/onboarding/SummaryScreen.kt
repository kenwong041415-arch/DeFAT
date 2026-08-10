package com.defat.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.SectionCard
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.TdeeSource

@Composable
fun SummaryRoute(
    viewModel: OnboardingViewModel,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SummaryScreen(
        target = uiState.dailyTarget,
        onStart = {
            viewModel.completeOnboarding()
            onStart()
        },
        onBack = onBack,
    )
}

@Composable
fun SummaryScreen(
    target: DailyTarget?,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onStart, enabled = target != null, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.common_start))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(text = stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (target == null) {
                Text(stringResource(R.string.error_required))
                return@Column
            }

            SectionCard(title = null, modifier = Modifier.padding(bottom = 12.dp)) {
                LabeledValue(
                    label = stringResource(R.string.summary_bmr),
                    value = stringResource(R.string.home_metabolism_line, target.bmrKcal.roundKcal(), target.tdeeKcal.roundKcal()),
                )
                Text(
                    text = stringResource(
                        if (target.bmrMethod == BmrMethod.KATCH_MCARDLE) {
                            R.string.summary_method_katch
                        } else {
                            R.string.summary_method_mifflin
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionCard(title = stringResource(R.string.summary_target), modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "${target.targetKcal.roundKcal()} ${stringResource(R.string.unit_kcal)}",
                    style = MaterialTheme.typography.displaySmall,
                )
                if (target.floorApplied) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.summary_floor_applied, target.targetKcal.roundKcal()))
                }
            }

            SectionCard(title = null) {
                MacroRow(macros = target.macros)
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MacroRow(macros: MacroTargets) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MacroValue(stringResource(R.string.meal_protein), macros.proteinG.roundGrams())
        MacroValue(stringResource(R.string.meal_fat), macros.fatG.roundGrams())
        MacroValue(stringResource(R.string.meal_carbs), macros.carbsG.roundGrams())
    }
}

@Composable
private fun MacroValue(label: String, grams: Int) {
    Column {
        Text(text = "$grams${stringResource(R.string.unit_g)}", style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SummaryScreenPreview() {
    DefatTheme {
        SummaryScreen(
            target = DailyTarget(
                bmrKcal = 1666.0,
                bmrMethod = BmrMethod.KATCH_MCARDLE,
                tdeeKcal = 2582.3,
                tdeeSource = TdeeSource.ACTIVITY_MULTIPLIER,
                targetKcal = 2065.84,
                floorApplied = false,
                macros = MacroTargets(proteinG = 132.0, fatG = 48.0, carbsG = 276.46, carbsClamped = false),
            ),
            onStart = {},
            onBack = {},
        )
    }
}

package com.defat.app.ui.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.WizardScaffold
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.RateAssessment
import java.time.LocalDate

@Composable
fun GoalRoute(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GoalScreen(
        uiState = uiState,
        onTargetWeightChange = viewModel::setGoalTargetWeightText,
        onTargetFatChange = viewModel::setGoalTargetFatText,
        onTargetDateChange = viewModel::setGoalTargetDate,
        onNext = onNext,
        onBack = onBack,
    )
}

@Composable
fun GoalScreen(
    uiState: OnboardingUiState,
    onTargetWeightChange: (String) -> Unit,
    onTargetFatChange: (String) -> Unit,
    onTargetDateChange: (LocalDate?) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    WizardScaffold(
        title = stringResource(R.string.goal_title),
        nextLabel = stringResource(R.string.common_next),
        onNext = onNext,
        nextEnabled = uiState.isGoalValid,
        backLabel = stringResource(R.string.common_back),
        onBack = onBack,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        DecimalField(
            value = uiState.goalTargetWeightText,
            onValueChange = onTargetWeightChange,
            label = stringResource(R.string.goal_target_weight),
            suffix = stringResource(R.string.unit_kg),
        )

        Spacer(modifier = Modifier.height(16.dp))
        DecimalField(
            value = uiState.goalTargetFatText,
            onValueChange = onTargetFatChange,
            label = stringResource(R.string.goal_target_fat),
            suffix = "%",
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.goal_target_date), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = {
                // Simple 12-week-out default; a full date-picker dialog
                // mirrors BasicsScreen's pattern and is not required by the
                // Phase 1 acceptance script.
                onTargetDateChange(uiState.goalTargetDate ?: LocalDate.now().plusWeeks(12))
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(uiState.goalTargetDate?.toString() ?: stringResource(R.string.goal_target_date))
        }

        val weightKg = uiState.weightKg
        val goalWeightKg = uiState.goalTargetWeightKg
        if (weightKg != null && goalWeightKg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val totalToLoseKg = weightKg - goalWeightKg
            // Only a loss gets the "kg to lose" wording; a maintain or gain
            // goal would otherwise read as a negative amount to lose.
            if (totalToLoseKg > 0) {
                Text(stringResource(R.string.goal_total_to_lose, totalToLoseKg))
            } else if (totalToLoseKg < 0) {
                Text(stringResource(R.string.goal_total_to_gain, -totalToLoseKg))
            } else {
                Text(stringResource(R.string.goal_maintain))
            }

            // The pace arithmetic lives in AssessGoalRateUseCase (core-domain,
            // unit-tested) — CLAUDE.md keeps formulas out of UI code.
            val rate = uiState.goalRate
            if (rate != null) {
                val (textRes, color) = when (rate.assessment) {
                    RateAssessment.TOO_FAST -> R.string.goal_rate_fast to MaterialTheme.colorScheme.error
                    RateAssessment.TOO_SLOW -> R.string.goal_rate_slow to MaterialTheme.colorScheme.secondary
                    else -> R.string.goal_rate_safe to MaterialTheme.colorScheme.primary
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(textRes, rate.weeklyRatePct), color = color)
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalScreenPreview() {
    DefatTheme {
        GoalScreen(
            uiState = OnboardingUiState(
                weightKgText = "80",
                goalTargetWeightText = "75",
                goalTargetDate = LocalDate.now().plusWeeks(10),
            ),
            onTargetWeightChange = {},
            onTargetFatChange = {},
            onTargetDateChange = {},
            onNext = {},
            onBack = {},
        )
    }
}

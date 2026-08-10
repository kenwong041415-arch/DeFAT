package com.defat.app.ui.onboarding

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.WizardScaffold
import com.defat.app.ui.theme.DefatTheme

@Composable
fun BodyRoute(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BodyScreen(
        uiState = uiState,
        onWeightChange = viewModel::setWeightKgText,
        onBodyFatChange = viewModel::setBodyFatPctText,
        onBodyFatUnknownChange = viewModel::setBodyFatUnknown,
        onNext = onNext,
        onBack = onBack,
    )
}

@Composable
fun BodyScreen(
    uiState: OnboardingUiState,
    onWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onBodyFatUnknownChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    WizardScaffold(
        title = stringResource(R.string.body_title),
        nextLabel = stringResource(R.string.common_next),
        onNext = onNext,
        nextEnabled = uiState.isBodyValid,
        backLabel = stringResource(R.string.common_back),
        onBack = onBack,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        DecimalField(
            value = uiState.weightKgText,
            onValueChange = onWeightChange,
            label = stringResource(R.string.body_weight),
            suffix = stringResource(R.string.unit_kg),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = uiState.bodyFatUnknown, onCheckedChange = onBodyFatUnknownChange)
            Text(stringResource(R.string.body_fat_unknown), modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        DecimalField(
            value = uiState.bodyFatPctText,
            onValueChange = onBodyFatChange,
            label = stringResource(R.string.body_fat_pct),
            suffix = "%",
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.body_fat_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BodyScreenPreview() {
    DefatTheme {
        BodyScreen(
            uiState = OnboardingUiState(weightKgText = "80", bodyFatPctText = "25"),
            onWeightChange = {},
            onBodyFatChange = {},
            onBodyFatUnknownChange = {},
            onNext = {},
            onBack = {},
        )
    }
}

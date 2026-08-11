package com.defat.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.defat.app.ui.component.WizardScaffold
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.model.ActivityLevel

@Composable
fun ActivityRoute(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ActivityScreen(
        selected = uiState.activityLevel,
        onSelected = viewModel::setActivityLevel,
        onNext = onNext,
        onBack = onBack,
    )
}

private data class ActivityOption(val level: ActivityLevel, val titleRes: Int, val descRes: Int)

private val OPTIONS = listOf(
    ActivityOption(ActivityLevel.SEDENTARY, R.string.activity_sedentary, R.string.activity_sedentary_desc),
    ActivityOption(ActivityLevel.LIGHT, R.string.activity_light, R.string.activity_light_desc),
    ActivityOption(ActivityLevel.MODERATE, R.string.activity_moderate, R.string.activity_moderate_desc),
    ActivityOption(ActivityLevel.ACTIVE, R.string.activity_active, R.string.activity_active_desc),
    ActivityOption(ActivityLevel.ATHLETE, R.string.activity_athlete, R.string.activity_athlete_desc),
)

@Composable
fun ActivityScreen(
    selected: ActivityLevel,
    onSelected: (ActivityLevel) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    WizardScaffold(
        title = stringResource(R.string.activity_title),
        nextLabel = stringResource(R.string.common_next),
        onNext = onNext,
        nextEnabled = true,
        backLabel = stringResource(R.string.common_back),
        onBack = onBack,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        OPTIONS.forEach { option ->
            val isSelected = option.level == selected
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .selectable(selected = isSelected, onClick = { onSelected(option.level) }),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = { onSelected(option.level) })
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(option.titleRes), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(option.descRes), style = MaterialTheme.typography.bodySmall)
                        Text(
                            "x${option.level.multiplier}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActivityScreenPreview() {
    DefatTheme {
        ActivityScreen(selected = ActivityLevel.MODERATE, onSelected = {}, onNext = {}, onBack = {})
    }
}

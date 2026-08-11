package com.defat.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.WizardScaffold
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.model.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun BasicsRoute(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BasicsScreen(
        uiState = uiState,
        onSexSelected = viewModel::setSex,
        onBirthDateSelected = viewModel::setBirthDate,
        onHeightChange = viewModel::setHeightCmText,
        onNext = onNext,
        onBack = onBack,
    )
}

@Composable
fun BasicsScreen(
    uiState: OnboardingUiState,
    onSexSelected: (Sex) -> Unit,
    onBirthDateSelected: (LocalDate) -> Unit,
    onHeightChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    WizardScaffold(
        title = stringResource(R.string.basics_title),
        nextLabel = stringResource(R.string.common_next),
        onNext = onNext,
        nextEnabled = uiState.isBasicsValid,
        backLabel = stringResource(R.string.common_back),
        onBack = onBack,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.basics_sex), style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            SexOption(
                label = stringResource(R.string.sex_male),
                selected = uiState.sex == Sex.MALE,
                onClick = { onSexSelected(Sex.MALE) },
            )
            Spacer(modifier = Modifier.height(0.dp))
            SexOption(
                label = stringResource(R.string.sex_female),
                selected = uiState.sex == Sex.FEMALE,
                onClick = { onSexSelected(Sex.FEMALE) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.basics_birth_date), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            val ageSuffix = uiState.ageYears?.let { " · " + stringResource(R.string.basics_age_years, it) } ?: ""
            Text((uiState.birthDate?.toString() ?: "—") + ageSuffix)
        }

        Spacer(modifier = Modifier.height(24.dp))
        DecimalField(
            value = uiState.heightCmText,
            onValueChange = onHeightChange,
            label = stringResource(R.string.basics_height),
            suffix = stringResource(R.string.unit_cm),
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (showDatePicker) {
        val initialMillis = (uiState.birthDate ?: LocalDate.now().minusYears(30))
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onBirthDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
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

@Composable
private fun SexOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.selectable(selected = selected, onClick = onClick).padding(end = 16.dp)) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BasicsScreenPreview() {
    DefatTheme {
        BasicsScreen(
            uiState = OnboardingUiState(sex = Sex.MALE, birthDate = LocalDate.of(1996, 6, 15), heightCmText = "175"),
            onSexSelected = {},
            onBirthDateSelected = {},
            onHeightChange = {},
            onNext = {},
            onBack = {},
        )
    }
}

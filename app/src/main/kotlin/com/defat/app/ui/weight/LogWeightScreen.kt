package com.defat.app.ui.weight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun LogWeightRoute(
    onDone: () -> Unit,
    viewModel: LogWeightViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            if (event is LogWeightEvent.Done) onDone()
        }
    }

    LogWeightScreen(
        uiState = uiState,
        onWeightChange = viewModel::setWeightText,
        onBodyFatChange = viewModel::setBodyFatText,
        onSave = viewModel::save,
        onBack = onDone,
    )
}

@Composable
fun LogWeightScreen(
    uiState: LogWeightUiState,
    onWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            DecimalField(
                value = uiState.weightKgText,
                onValueChange = onWeightChange,
                label = stringResource(R.string.body_weight),
                suffix = stringResource(R.string.unit_kg),
            )
            Spacer(modifier = Modifier.height(12.dp))
            DecimalField(
                value = uiState.bodyFatPctText,
                onValueChange = onBodyFatChange,
                label = stringResource(R.string.body_fat_pct),
                suffix = "%",
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.weight_updates_target), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSave, enabled = uiState.isSaveEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogWeightScreenPreview() {
    DefatTheme {
        LogWeightScreen(
            uiState = LogWeightUiState(weightKgText = "79.4", bodyFatPctText = "24.1"),
            onWeightChange = {},
            onBodyFatChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

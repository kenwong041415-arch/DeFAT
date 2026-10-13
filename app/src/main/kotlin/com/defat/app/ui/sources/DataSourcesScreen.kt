package com.defat.app.ui.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.component.SectionCard
import com.defat.app.ui.theme.DefatTheme

/**
 * 數據來源 (plan §13): plain-language provenance for every number — what it
 * is, where it comes from, and how far to trust it. It is also the honest
 * answer to "where did 850 kcal come from?", and cheaper to keep right than a
 * confidence badge on every screen.
 *
 * The medical disclaimer is the same string resource the onboarding screen
 * shows (docs/06), referenced rather than copied.
 */
@Composable
fun DataSourcesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sources_title)) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.sources_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SourceSection(R.string.sources_bmr_title, R.string.sources_bmr_body)
            SourceSection(R.string.sources_tdee_title, R.string.sources_tdee_body)
            SourceSection(R.string.sources_food_title, R.string.sources_food_body)
            SourceSection(R.string.sources_water_title, R.string.sources_water_body)
            SourceSection(R.string.sources_days_title, R.string.sources_days_body)
            SourceSection(R.string.sources_disclaimer_heading, R.string.disclaimer_body)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SourceSection(titleRes: Int, bodyRes: Int) {
    SectionCard(title = stringResource(titleRes), modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DataSourcesScreenPreview() {
    DefatTheme {
        DataSourcesScreen(onBack = {})
    }
}

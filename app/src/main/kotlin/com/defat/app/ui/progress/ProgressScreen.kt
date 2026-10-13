package com.defat.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.component.DefatTab
import com.defat.app.ui.component.DefatTabScaffold
import com.defat.app.ui.component.ShowChartIcon
import com.defat.app.ui.theme.DefatTheme

/**
 * 進度 tab (plan §6.4): enabled and navigable, but a single centred
 * placeholder. No fake charts — the trend engine is Phase 4.
 */
@Composable
fun ProgressRoute(
    onTabSelected: (DefatTab) -> Unit,
    onAddMeal: () -> Unit,
) {
    ProgressScreen(onTabSelected = onTabSelected, onAddMeal = onAddMeal)
}

@Composable
fun ProgressScreen(
    onTabSelected: (DefatTab) -> Unit,
    onAddMeal: () -> Unit,
) {
    DefatTabScaffold(
        selectedTab = DefatTab.PROGRESS,
        onTabSelected = onTabSelected,
        onAddMeal = onAddMeal,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.progress_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = ShowChartIcon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.progress_soon_heading),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.progress_soon_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProgressScreenPreview() {
    DefatTheme {
        ProgressScreen(onTabSelected = {}, onAddMeal = {})
    }
}

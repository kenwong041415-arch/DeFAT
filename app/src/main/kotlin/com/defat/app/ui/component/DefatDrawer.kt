package com.defat.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.theme.DefatTheme

/**
 * The single navigation surface for the app (plan §2 E2) — Home only, not a
 * global scaffold. No icons anywhere in this drawer (E4): there is no core
 * Material icon for "charts" or "weight", and material-icons-extended is
 * never becoming a dependency.
 */
@Composable
fun DefatDrawerContent(
    dailyTargetKcal: Int?,
    onToday: () -> Unit,
    onLogWeight: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            if (dailyTargetKcal != null) {
                Text(
                    text = stringResource(R.string.drawer_target_line, dailyTargetKcal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.home_title_today)) },
            selected = true,
            onClick = onToday,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.weight_title)) },
            selected = false,
            onClick = onLogWeight,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.history_title)) },
            selected = false,
            onClick = onHistory,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )

        // Deliberately a plain, non-clickable Row, not a NavigationDrawerItem
        // (plan §2 E3): NavigationDrawerItem has no `enabled` param, and
        // onClick = {} would still read as a Button to TalkBack. Do not
        // "tidy" this into a NavigationDrawerItem.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.drawer_charts),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.drawer_coming_soon),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.profile_title)) },
            selected = false,
            onClick = onProfile,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefatDrawerContentPreview() {
    DefatTheme {
        DefatDrawerContent(
            dailyTargetKcal = 2266,
            onToday = {},
            onLogWeight = {},
            onHistory = {},
            onProfile = {},
        )
    }
}

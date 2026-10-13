package com.defat.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.defat.app.R
import com.defat.app.navigation.Routes

/** The four destinations on the bottom bar. The centre add button is not a tab. */
enum class DefatTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    TODAY(Routes.HOME, R.string.nav_today, Icons.Outlined.Home),
    HISTORY(Routes.HISTORY, R.string.nav_history, Icons.Outlined.DateRange),
    PROGRESS(Routes.PROGRESS, R.string.nav_progress, ShowChartIcon),
    ME(Routes.PROFILE, R.string.nav_me, Icons.Outlined.Person),
}

/**
 * Shared frame for the four tab screens (plan §6.1): a [NavigationBar] with
 * 今日 / 歷史 / 進度 / 我, and the add button centred above it.
 *
 * Each tab screen wraps itself in this rather than the NavHost being wrapped
 * once, so the onboarding wizard, the meal editor and the weight screen never
 * show the bar and no Scaffold is nested inside another (nested Scaffolds
 * double-apply system-bar insets).
 *
 * The selected tab is not local state: it is whichever screen is on screen,
 * which the NavController already restores across rotation and process death.
 *
 * The centre button is a real [FloatingActionButton], not a fifth bar item — a
 * disabled-looking tab that is really a button fails TalkBack.
 */
@Composable
fun DefatTabScaffold(
    selectedTab: DefatTab,
    onTabSelected: (DefatTab) -> Unit,
    onAddMeal: () -> Unit,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = {
            NavigationBar {
                DefatTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_meal))
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = content,
    )
}

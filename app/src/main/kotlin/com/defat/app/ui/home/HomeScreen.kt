package com.defat.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.CalorieRing
import com.defat.app.ui.component.DefatDrawerContent
import com.defat.app.ui.component.MacroTileData
import com.defat.app.ui.component.MacroTileRow
import com.defat.app.ui.component.MealTypeSections
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.MealTypeGroup
import com.defat.core.domain.model.TdeeSource
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onAddMeal: () -> Unit,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onAddMeal = onAddMeal,
        onEditMeal = onEditMeal,
        onLogWeight = onLogWeight,
        onOpenHistory = onOpenHistory,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddMeal: () -> Unit,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val dailyTargetKcal = (uiState as? HomeUiState.Content)?.dayRollup?.target?.targetKcal?.roundKcal()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DefatDrawerContent(
                dailyTargetKcal = dailyTargetKcal,
                onToday = { scope.launch { drawerState.close() } },
                onLogWeight = {
                    scope.launch { drawerState.close() }
                    onLogWeight()
                },
                onHistory = {
                    scope.launch { drawerState.close() }
                    onOpenHistory()
                },
                onProfile = {
                    scope.launch { drawerState.close() }
                    onOpenProfile()
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.home_title_today)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_open_menu))
                        }
                    },
                )
            },
            floatingActionButton = {
                if (uiState is HomeUiState.Content) {
                    FloatingActionButton(onClick = onAddMeal) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_meal))
                    }
                }
            },
        ) { padding ->
            when (uiState) {
                is HomeUiState.Loading, HomeUiState.NeedsOnboarding -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HomeUiState.Content -> {
                    HomeContent(
                        modifier = Modifier.padding(padding),
                        rollup = uiState.dayRollup,
                        mealGroups = uiState.mealGroups,
                        onEditMeal = onEditMeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    rollup: DayRollup,
    mealGroups: List<MealTypeGroup>,
    onEditMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMetabolismInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        CalorieRing(
            progressFraction = rollup.progressFraction,
            isOverTarget = rollup.isOverTarget,
            centerNumber = kotlin.math.abs(rollup.remainingKcal).roundKcal().toString(),
            centerLabel = stringResource(
                if (rollup.isOverTarget) R.string.home_kcal_over else R.string.home_kcal_left,
            ),
            subLabel = stringResource(
                R.string.home_eaten_of_target,
                rollup.intakeKcal.roundKcal(),
                rollup.target.targetKcal.roundKcal(),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
        MacroTileRow(
            protein = MacroTileData(
                label = stringResource(R.string.macro_protein),
                usedG = rollup.proteinG.roundGrams(),
                targetG = rollup.target.macros.proteinG.roundGrams(),
                emphasised = true,
                contentDescription = stringResource(
                    R.string.home_protein_progress,
                    rollup.proteinG.roundGrams(),
                    rollup.target.macros.proteinG.roundGrams(),
                ),
            ),
            carbs = MacroTileData(
                label = stringResource(R.string.macro_carbs),
                usedG = rollup.carbsG.roundGrams(),
                targetG = rollup.target.macros.carbsG.roundGrams(),
                emphasised = false,
                contentDescription = stringResource(
                    R.string.home_carbs_progress,
                    rollup.carbsG.roundGrams(),
                    rollup.target.macros.carbsG.roundGrams(),
                ),
            ),
            fat = MacroTileData(
                label = stringResource(R.string.macro_fat),
                usedG = rollup.fatG.roundGrams(),
                targetG = rollup.target.macros.fatG.roundGrams(),
                emphasised = false,
                contentDescription = stringResource(
                    R.string.home_fat_progress,
                    rollup.fatG.roundGrams(),
                    rollup.target.macros.fatG.roundGrams(),
                ),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.home_meals_title), style = MaterialTheme.typography.titleMedium)
        if (mealGroups.all { it.isEmpty }) {
            Text(
                stringResource(R.string.home_meals_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MealTypeSections(groups = mealGroups, onEditMeal = onEditMeal)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.home_metabolism_footnote,
                rollup.target.bmrKcal.roundKcal(),
                rollup.target.tdeeKcal.roundKcal(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable { showMetabolismInfo = true }
                .semantics { role = Role.Button },
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showMetabolismInfo) {
        ModalBottomSheet(onDismissRequest = { showMetabolismInfo = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.home_metabolism_info_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.home_metabolism_info_body))
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    val target = DailyTarget(
        bmrKcal = 1666.0,
        bmrMethod = BmrMethod.KATCH_MCARDLE,
        tdeeKcal = 2582.3,
        tdeeSource = TdeeSource.ACTIVITY_MULTIPLIER,
        targetKcal = 2065.84,
        floorApplied = false,
        macros = MacroTargets(proteinG = 132.0, fatG = 48.0, carbsG = 276.46, carbsClamped = false),
    )
    val rollup = DayRollup(
        date = LocalDate.now(),
        target = target,
        intakeKcal = 1050.0,
        proteinG = 70.0,
        carbsG = 110.0,
        fatG = 27.0,
        mealCount = 2,
    )
    DefatTheme {
        HomeScreen(
            uiState = HomeUiState.Content(
                dayRollup = rollup,
                mealGroups = MealGrouping.groupByType(emptyList()),
            ),
            onAddMeal = {},
            onEditMeal = {},
            onLogWeight = {},
            onOpenHistory = {},
            onOpenProfile = {},
        )
    }
}

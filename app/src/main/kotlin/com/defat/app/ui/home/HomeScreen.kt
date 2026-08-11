package com.defat.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.CalorieRing
import com.defat.app.ui.component.MacroBar
import com.defat.app.ui.component.SectionCard
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.roundGrams
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.BmrMethod
import com.defat.core.domain.model.DailyTarget
import com.defat.core.domain.model.DayRollup
import com.defat.core.domain.model.MacroTargets
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.TdeeSource
import com.defat.core.domain.model.WeightEntry
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    onAddMeal: () -> Unit,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onAddMeal = onAddMeal,
        onEditMeal = onEditMeal,
        onLogWeight = onLogWeight,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddMeal: () -> Unit,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.profile_title))
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
                    meals = uiState.meals,
                    latestWeight = uiState.latestWeight,
                    onEditMeal = onEditMeal,
                    onLogWeight = onLogWeight,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    rollup: DayRollup,
    meals: List<Meal>,
    latestWeight: WeightEntry?,
    onEditMeal: (String) -> Unit,
    onLogWeight: () -> Unit,
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

        Spacer(modifier = Modifier.height(8.dp))
        MacroBar(
            label = stringResource(R.string.home_protein_progress, rollup.proteinG.roundGrams(), rollup.target.macros.proteinG.roundGrams()),
            current = rollup.proteinG,
            target = rollup.target.macros.proteinG,
        )
        MacroBar(
            label = stringResource(R.string.home_carbs_progress, rollup.carbsG.roundGrams(), rollup.target.macros.carbsG.roundGrams()),
            current = rollup.carbsG,
            target = rollup.target.macros.carbsG,
        )
        MacroBar(
            label = stringResource(R.string.home_fat_progress, rollup.fatG.roundGrams(), rollup.target.macros.fatG.roundGrams()),
            current = rollup.fatG,
            target = rollup.target.macros.fatG,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_metabolism_line, rollup.target.bmrKcal.roundKcal(), rollup.target.tdeeKcal.roundKcal()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable { showMetabolismInfo = true },
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.home_meals_title), style = MaterialTheme.typography.titleMedium)
        if (meals.isEmpty()) {
            Text(stringResource(R.string.home_meals_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column {
                meals.forEach { meal ->
                    MealRow(meal = meal, onClick = { onEditMeal(meal.id) })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionCard(title = null) {
            Text(stringResource(R.string.home_log_weight), style = MaterialTheme.typography.titleSmall)
            if (latestWeight != null) {
                Text(
                    "${latestWeight.weightKg} ${stringResource(R.string.unit_kg)}" +
                        (latestWeight.bodyFatPct?.let { " · $it%" } ?: ""),
                )
                Text(
                    stringResource(R.string.home_weight_as_of, latestWeight.date.toString()),
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                Text(stringResource(R.string.home_weight_none))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onLogWeight) { Text(stringResource(R.string.home_log_weight)) }
        }

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

@Composable
private fun MealRow(meal: Meal, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(meal.name)
        Text("${meal.kcal.roundKcal()} ${stringResource(R.string.unit_kcal)}")
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
                meals = listOf(
                    Meal(
                        id = "1",
                        loggedAt = Instant.now(),
                        date = LocalDate.now(),
                        name = "Chicken and rice",
                        kcal = 600.0,
                        proteinG = 30.0,
                        carbsG = 80.0,
                        fatG = 15.0,
                        source = MealSource.MANUAL,
                    ),
                ),
                latestWeight = WeightEntry(
                    date = LocalDate.now(),
                    weightKg = 79.4,
                    bodyFatPct = 24.1,
                    recordedAt = Instant.now(),
                ),
            ),
            onAddMeal = {},
            onEditMeal = {},
            onLogWeight = {},
            onOpenProfile = {},
        )
    }
}

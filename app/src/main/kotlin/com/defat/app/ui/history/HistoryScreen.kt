package com.defat.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defat.app.R
import com.defat.app.ui.component.DefatTab
import com.defat.app.ui.component.DefatTabScaffold
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.DaySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HistoryRoute(
    onOpenToday: () -> Unit,
    onAddMeal: () -> Unit,
    onTabSelected: (DefatTab) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onOpenDay = { date ->
            viewModel.openDay(date)
            onOpenToday()
        },
        onAddMeal = onAddMeal,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onOpenDay: (LocalDate) -> Unit,
    onAddMeal: () -> Unit,
    onTabSelected: (DefatTab) -> Unit,
) {
    DefatTabScaffold(
        selectedTab = DefatTab.HISTORY,
        onTabSelected = onTabSelected,
        onAddMeal = onAddMeal,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
    ) { padding ->
        when (uiState) {
            is HistoryUiState.Loading, HistoryUiState.NeedsOnboarding -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HistoryUiState.Content -> {
                HistoryContent(
                    modifier = Modifier.padding(padding),
                    days = uiState.days,
                    onOpenDay = onOpenDay,
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    days: List<DaySummary>,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.history_pick_date))
        }

        if (days.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Bottom clearance for the centre add button.
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(items = days, key = { it.date.toString() }) { day ->
                    Column {
                        DayRow(day = day, onClick = { onOpenDay(day.date) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onOpenDay(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
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
private fun DayRow(day: DaySummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(day.date.toString(), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(
                    R.string.home_eaten_of_target,
                    day.intakeKcal.roundKcal(),
                    day.targetKcal.roundKcal(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        // 達標 / 超標 — a plain label, not a Chip: it is not interactive.
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (day.isOverTarget) colors.errorContainer else colors.secondaryContainer,
            contentColor = if (day.isOverTarget) colors.onErrorContainer else colors.onSecondaryContainer,
        ) {
            Text(
                text = stringResource(
                    if (day.isOverTarget) R.string.history_over_target else R.string.history_on_target,
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryScreenPreview() {
    DefatTheme {
        HistoryScreen(
            uiState = HistoryUiState.Content(
                days = listOf(
                    DaySummary(LocalDate.of(2026, 10, 12), intakeKcal = 2050.0, targetKcal = 2134.0, mealCount = 4),
                    DaySummary(LocalDate.of(2026, 10, 11), intakeKcal = 2400.0, targetKcal = 2134.0, mealCount = 5),
                ),
            ),
            onOpenDay = {},
            onAddMeal = {},
            onTabSelected = {},
        )
    }
}

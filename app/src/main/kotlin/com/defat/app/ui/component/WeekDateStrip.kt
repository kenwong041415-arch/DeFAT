package com.defat.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.WeekStrip
import com.defat.core.domain.model.WeekDayCell
import java.time.LocalDate

private val SwipeThreshold = 48.dp
private val DayCircleSize = 36.dp

/**
 * Seven days, Monday-first (plan §6.3). The selected day is a filled pill in
 * `primary`; today, when not selected, gets a 2 dp `primary` outline; a day
 * with at least one logged meal gets a dot; future days are shown but
 * disabled. Swiping the strip moves a week; the same two moves are exposed to
 * TalkBack as custom actions because a swipe on a strip is not discoverable.
 *
 * Each cell is a real `selectable` — never wrapped in `clearAndSetSemantics`,
 * which would drop its click action (the Phase 1.6 accessibility regression).
 */
@Composable
fun WeekDateStrip(
    days: List<WeekDayCell>,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initials = stringArrayResource(R.array.week_day_initials)
    val previousWeekLabel = stringResource(R.string.week_previous)
    val nextWeekLabel = stringResource(R.string.week_next)
    // pointerInput(Unit) never restarts, so read the lambdas through State.
    val currentOnPreviousWeek by rememberUpdatedState(onPreviousWeek)
    val currentOnNextWeek by rememberUpdatedState(onNextWeek)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                val thresholdPx = SwipeThreshold.toPx()
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > thresholdPx) {
                            currentOnPreviousWeek()
                        } else if (total < -thresholdPx) {
                            currentOnNextWeek()
                        }
                    },
                    onDragCancel = { total = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                )
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(previousWeekLabel) {
                        currentOnPreviousWeek()
                        true
                    },
                    CustomAccessibilityAction(nextWeekLabel) {
                        currentOnNextWeek()
                        true
                    },
                )
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEachIndexed { index, cell ->
            WeekDayCellView(
                cell = cell,
                initial = initials.getOrElse(index) { "" },
                onSelect = onSelectDay,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekDayCellView(
    cell: WeekDayCell,
    initial: String,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val mutedAlpha = if (cell.isFuture) 0.38f else 1f
    val dotDescription = stringResource(R.string.week_day_has_entries)

    Column(
        modifier = modifier
            .selectable(
                selected = cell.isSelected,
                enabled = !cell.isFuture,
                role = Role.Button,
                onClick = { onSelect(cell.date) },
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant.copy(alpha = mutedAlpha),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(DayCircleSize)
                .clip(CircleShape)
                .background(if (cell.isSelected) colors.primary else Color.Transparent)
                .then(
                    if (cell.isToday && !cell.isSelected) {
                        Modifier.border(BorderStroke(2.dp, colors.primary), CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cell.isSelected) colors.onPrimary else colors.onSurface.copy(alpha = mutedAlpha),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (cell.hasEntries) colors.secondary else Color.Transparent)
                .then(
                    if (cell.hasEntries) {
                        Modifier.semantics { contentDescription = dotDescription }
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

@Preview(name = "Light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeekDateStripPreview() {
    val today = LocalDate.of(2026, 10, 13)
    DefatTheme {
        WeekDateStrip(
            days = WeekStrip.cells(
                selected = LocalDate.of(2026, 10, 12),
                today = today,
                loggedDates = setOf(LocalDate.of(2026, 10, 12)),
            ),
            onSelectDay = {},
            onPreviousWeek = {},
            onNextWeek = {},
        )
    }
}

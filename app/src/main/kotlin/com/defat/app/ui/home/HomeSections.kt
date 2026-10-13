package com.defat.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.exercise.exerciseActivityName
import com.defat.core.domain.calc.WaterCups
import com.defat.core.domain.calc.roundKcal
import com.defat.core.domain.model.ExerciseEntry
import java.text.NumberFormat

/**
 * 跟足計劃日數 (plan §12): a small chip next to Home's date with the number
 * and the label. No flames, no congratulation copy.
 */
@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.streak_chip, days),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

/** The 運動 section under the meals (plan §10.4): the day's total and a 加運動 row. */
@Composable
fun ExerciseSection(
    entries: List<ExerciseEntry>,
    totalKcal: Double,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.exercise_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.exercise_section_total, totalKcal.roundKcal()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.exercise_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        entries.forEach { entry ->
            val deleteDescription = stringResource(R.string.exercise_delete)
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.exercise_entry_line,
                        exerciseActivityName(entry.activityKey),
                        entry.minutes,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.kcal_value, entry.kcal.roundKcal()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { pendingDeleteId = entry.id }) {
                    Icon(Icons.Filled.Delete, contentDescription = deleteDescription)
                }
            }
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.exercise_add))
        }
    }

    val deleteId = pendingDeleteId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.exercise_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deleteId)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/**
 * The water row (plan §11): eight 250 ml cups, a 自訂 field for other amounts,
 * and `1,750 / 2,400 ml`. The goal is a 建議 (35 ml per kg), not a requirement.
 */
@Composable
fun WaterSection(
    waterMl: Int,
    goalMl: Int,
    onTapCup: (Int) -> Unit,
    onAddWater: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var customText by remember { mutableStateOf("") }
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val filledCups = WaterCups.filledCups(waterMl)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.water_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.water_progress,
                    numberFormat.format(waterMl.toLong()),
                    numberFormat.format(goalMl.toLong()),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.water_goal_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (cupNumber in 1..WaterCups.CUP_COUNT) {
                WaterCup(
                    number = cupNumber,
                    filled = cupNumber <= filledCups,
                    onClick = { onTapCup(cupNumber) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            DecimalField(
                value = customText,
                onValueChange = { text -> customText = text.filter { char -> char.isDigit() } },
                label = stringResource(R.string.water_custom_label),
                suffix = stringResource(R.string.water_unit_ml),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { if (onAddWater(customText)) customText = "" },
                enabled = customText.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.water_add))
            }
        }
    }
}

@Composable
private fun WaterCup(number: Int, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val description = stringResource(
        if (filled) R.string.water_cup_filled else R.string.water_cup_empty,
        number,
    )
    Box(
        modifier = modifier
            // 48 dp is the minimum comfortable touch target; the cup drawing is smaller.
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 32.dp)
                .clip(shape)
                .background(if (filled) colors.primary else colors.surfaceVariant)
                .border(width = 1.5.dp, color = colors.primary, shape = shape),
        )
    }
}

/** A thin rule between Home's stacked sections. */
@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

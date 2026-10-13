package com.defat.app.ui.exercise

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.component.DecimalField
import com.defat.app.ui.component.plainNumber
import com.defat.core.domain.calc.MetTable
import com.defat.core.domain.calc.roundKcal

/**
 * The 加運動 sheet (plan §10.4): an activity picker, 輕 / 中 / 重 for weight
 * training, a minutes field, and — the point of it — the formula with the
 * student's own numbers substituted. The owner is a trainer teaching students;
 * a visible formula is the product, not clutter.
 */
@Composable
fun ExerciseSheet(
    state: ExerciseSheetState,
    onSelectActivity: (String) -> Unit,
    onSelectWeightTrainingLevel: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val preview = state.preview
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.exercise_title), style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.exercise_activity_label), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetTable.ACTIVITIES.forEach { activity ->
                    FilterChip(
                        selected = state.activityKey == activity.key,
                        onClick = { onSelectActivity(activity.key) },
                        label = { Text(exerciseActivityName(activity.key)) },
                    )
                }
            }

            // docs/04 gives weight training as a 3.5-6 range; offer 輕 / 中 / 重
            // rather than silently picking one number.
            if (state.isWeightTraining) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.exercise_level_label), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetTable.WEIGHT_TRAINING_LEVELS.forEach { level ->
                        FilterChip(
                            selected = state.weightTrainingKey == level.key,
                            onClick = { onSelectWeightTrainingLevel(level.key) },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.exercise_level_chip,
                                        stringResource(weightTrainingLevelLabelRes(level.key)),
                                        plainNumber(level.met, 1),
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DecimalField(
                value = state.minutesText,
                onValueChange = onMinutesChange,
                label = stringResource(R.string.exercise_minutes),
                suffix = stringResource(R.string.exercise_minutes_unit),
                keyboardType = KeyboardType.Number,
            )

            // The working, always visible: MET x 體重(kg) x 小時 = kcal.
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.exercise_formula),
                style = MaterialTheme.typography.titleSmall,
            )
            val met = state.met
            val weight = state.weightKg
            if (met != null && weight != null) {
                Text(
                    text = stringResource(R.string.exercise_inputs_line, plainNumber(met, 1), plainNumber(weight, 1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (preview != null) {
                Text(
                    text = stringResource(
                        R.string.exercise_formula_values,
                        plainNumber(preview.met, 1),
                        plainNumber(preview.weightKg, 1),
                        plainNumber(preview.hours, 3),
                        preview.kcal.roundKcal(),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.exercise_enter_minutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Why the day's target does not always rise: docs/04 and plan §10.1.
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.exercise_target_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

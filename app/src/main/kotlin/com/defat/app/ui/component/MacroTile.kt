package com.defat.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.theme.DefatTheme

/** One macro tile's numbers, already resolved to plain ints for display. */
data class MacroTileData(
    val label: String,
    val usedG: Int,
    val targetG: Int,
    val emphasised: Boolean, // protein
    val contentDescription: String,
)

/** The three-tile row shown on Home and History (plan §8.2 S7 / §8.4). */
@Composable
fun MacroTileRow(
    protein: MacroTileData,
    carbs: MacroTileData,
    fat: MacroTileData,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroTile(data = protein, modifier = Modifier.weight(1f))
        MacroTile(data = carbs, modifier = Modifier.weight(1f))
        MacroTile(data = fat, modifier = Modifier.weight(1f))
    }
}

@Composable
fun MacroTile(data: MacroTileData, modifier: Modifier = Modifier) {
    val accent = if (data.emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val border = if (data.emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fraction = if (data.targetG <= 0) {
        0f
    } else {
        (data.usedG.toFloat() / data.targetG).coerceIn(0f, 1f)
    }
    val usedOfTargetText = stringResource(R.string.macro_used_of_target, data.usedG, data.targetG)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .background(
                if (data.emphasised) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = data.contentDescription },
    ) {
        Text(
            text = data.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (data.emphasised) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = usedOfTargetText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            // At zero, nothing is drawn — the fix for the owner's "full at
            // zero" complaint (plan §2 E5 / T15).
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
                )
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MacroTileRowPreview() {
    DefatTheme {
        MacroTileRow(
            protein = MacroTileData(
                label = "Protein",
                usedG = 70,
                targetG = 132,
                emphasised = true,
                contentDescription = "Protein 70 of 132 grams",
            ),
            carbs = MacroTileData(
                label = "Carbs",
                usedG = 0,
                targetG = 276,
                emphasised = false,
                contentDescription = "Carbs 0 of 276 grams",
            ),
            fat = MacroTileData(
                label = "Fat",
                usedG = 27,
                targetG = 48,
                emphasised = false,
                contentDescription = "Fat 27 of 48 grams",
            ),
        )
    }
}

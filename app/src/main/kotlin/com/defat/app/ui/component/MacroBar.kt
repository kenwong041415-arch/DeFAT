package com.defat.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** LinearProgressIndicator + label, e.g. "Protein 70 / 132 g" (plan §8.2 S7). */
@Composable
fun MacroBar(
    label: String,
    current: Double,
    target: Double,
    modifier: Modifier = Modifier,
) {
    val fraction = if (target <= 0.0) 0f else (current / target).toFloat().coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

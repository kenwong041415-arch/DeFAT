package com.defat.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Target-vs-intake ring. 270 degree sweep starting at 135 degrees, rounded
 * caps, animated progress. Centre content is supplied by the caller so the
 * "kcal left" vs "kcal over" strings stay in the app layer (plan §8.2 S7).
 */
@Composable
fun CalorieRing(
    progressFraction: Float,
    isOverTarget: Boolean,
    centerNumber: String,
    centerLabel: String,
    subLabel: String,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "calorie-ring-progress",
    )

    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = if (isOverTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val strokeWidthPx = 20.dp.toPx()
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            val sweepTotal = 270f
            val startAngle = 135f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = sweepTotal * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerNumber,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

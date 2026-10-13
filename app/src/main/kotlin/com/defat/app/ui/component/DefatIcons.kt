package com.defat.app.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The Material "show chart" glyph, drawn by hand.
 *
 * The plan (§6.1) names `Icons.Outlined.ShowChart`, but that icon lives in
 * `material-icons-extended`, which is deliberately not a dependency (Phase 1.6
 * plan E4 — it is a very large library and the project never needs it for
 * more than one glyph). This is the same shape as a plain vector, so no
 * dependency is added. Tinted by the surrounding [androidx.compose.material3.Icon].
 */
val ShowChartIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ShowChart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(3.5f, 18.49f)
        lineTo(9.5f, 12.48f)
        lineTo(13.5f, 16.48f)
        lineTo(22f, 6.92f)
        lineTo(20.59f, 5.51f)
        lineTo(13.5f, 13.48f)
        lineTo(9.5f, 9.48f)
        lineTo(2f, 16.99f)
        close()
    }.build()
}

package com.defat.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default Material 3 type scale, with an enlarged displayMedium for the
// CalorieRing's centre number (plan §8.3).
val DefatTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

internal val CalorieRingCenterNumberStyle: TextStyle
    get() = DefatTypography.displayMedium

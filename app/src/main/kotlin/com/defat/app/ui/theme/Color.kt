package com.defat.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Fixed pine/jade brand palette (plan §7 E14/E15 — no Material You dynamic
 * colour). Every colour lands in a standard M3 slot so
 * MaterialTheme.colorScheme.* stays the only way to get a colour.
 *
 * | M3 slot          | Meaning in DeFAT                | Used by                              |
 * |------------------|----------------------------------|---------------------------------------|
 * | primary          | pine — brand, emphasis           | FAB, filled buttons, top bar, protein tile |
 * | secondary        | jade — progress / done           | calorie ring arc, carbs & fat meter fills |
 * | tertiary         | clay — caution, not an error     | macro-mismatch hint, over-budget nudges |
 * | error            | brick — genuinely wrong          | validation errors, ring when over target |
 * | outlineVariant   | hairline                         | tile borders, dividers, meter tracks  |
 * | onSurfaceVariant | muted text                       | labels, subtotals, footnotes          |
 *
 * Hexes and contrast ratios are specified exactly in
 * docs/plans/phase1.6-ui-revision-plan.md §7.2/§7.3 — copied verbatim here,
 * not recomputed.
 */

// Light scheme
val LightPrimary = Color(0xFF14574A)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFCFE7E0)
val LightOnPrimaryContainer = Color(0xFF0A3229)
val LightSecondary = Color(0xFF2E9E7B)
val LightOnSecondary = Color(0xFF08251C)
val LightSecondaryContainer = Color(0xFFCDEDE0)
val LightOnSecondaryContainer = Color(0xFF0B3B2C)
val LightTertiary = Color(0xFFB4762A)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF7E4C6)
val LightOnTertiaryContainer = Color(0xFF5A3A0E)
val LightError = Color(0xFFA8402F)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF7DAD4)
val LightOnErrorContainer = Color(0xFF5A1E14)
val LightBackground = Color(0xFFEFF3F1)
val LightOnBackground = Color(0xFF16211E)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF16211E)
val LightSurfaceVariant = Color(0xFFDFE7E3)
val LightOnSurfaceVariant = Color(0xFF61756F)
val LightOutline = Color(0xFF8FA29C)
val LightOutlineVariant = Color(0xFFC9D5D0)

// Dark scheme
val DarkPrimary = Color(0xFF8FC9BC)
val DarkOnPrimary = Color(0xFF06322A)
val DarkPrimaryContainer = Color(0xFF1E4A41)
val DarkOnPrimaryContainer = Color(0xFFCFE7E0)
val DarkSecondary = Color(0xFF4FC49E)
val DarkOnSecondary = Color(0xFF00382B)
val DarkSecondaryContainer = Color(0xFF17453A)
val DarkOnSecondaryContainer = Color(0xFFCDEDE0)
val DarkTertiary = Color(0xFFF0B860)
val DarkOnTertiary = Color(0xFF3B2400)
val DarkTertiaryContainer = Color(0xFF553A12)
val DarkOnTertiaryContainer = Color(0xFFF7E4C6)
val DarkError = Color(0xFFF5A092)
val DarkOnError = Color(0xFF5F160A)
val DarkErrorContainer = Color(0xFF77291B)
val DarkOnErrorContainer = Color(0xFFF7DAD4)
val DarkBackground = Color(0xFF0C1614)
val DarkOnBackground = Color(0xFFE3EBE8)
val DarkSurface = Color(0xFF14211E)
val DarkOnSurface = Color(0xFFE3EBE8)
val DarkSurfaceVariant = Color(0xFF1E2E2A)
val DarkOnSurfaceVariant = Color(0xFF8CA39C)
val DarkOutline = Color(0xFF5C716B)
val DarkOutlineVariant = Color(0xFF2A3B36)

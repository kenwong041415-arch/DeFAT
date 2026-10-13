package com.defat.core.domain.model

/**
 * Calories plus the three macros. A null macro means "unknown" — some number
 * in the chain (the base food or an applied modifier) was never published —
 * and is deliberately distinct from 0.
 */
data class MacroTotals(
    val kcal: Double,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
)

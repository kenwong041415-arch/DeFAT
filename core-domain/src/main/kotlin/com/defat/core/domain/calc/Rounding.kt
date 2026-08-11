package com.defat.core.domain.calc

import kotlin.math.roundToInt

/**
 * Presentation-only rounding helpers. Calculators never round internally —
 * only the UI layer calls these, on the final displayed value.
 */
fun Double.roundKcal(): Int = this.roundToInt()

fun Double.roundGrams(): Int = this.roundToInt()

/** 1 decimal place, half-up. */
fun Double.roundKg1dp(): Double = halfUp1dp(this)

/** 1 decimal place, half-up. */
fun Double.roundPct1dp(): Double = halfUp1dp(this)

private fun halfUp1dp(value: Double): Double {
    val sign = if (value < 0) -1.0 else 1.0
    return sign * (kotlin.math.abs(value) * 10.0 + 0.5).let { kotlin.math.floor(it) } / 10.0
}

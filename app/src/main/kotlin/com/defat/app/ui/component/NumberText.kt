package com.defat.app.ui.component

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A number as plain text, with at most [maxDecimals] decimals and no trailing
 * zeros: 80.0 -> "80", 0.75 -> "0.75", 6.8 -> "6.8". Display only; nothing is
 * calculated here.
 */
fun plainNumber(value: Double, maxDecimals: Int = 2): String =
    BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** A signed whole number with a real minus sign: +95, −120, 0. */
fun signedNumber(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

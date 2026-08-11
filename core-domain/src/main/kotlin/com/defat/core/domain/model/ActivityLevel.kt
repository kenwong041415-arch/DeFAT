package com.defat.core.domain.model

/**
 * Classic TDEE activity multipliers (docs/04 — TDEE, no-wearable-data mode).
 */
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    ATHLETE(1.9),
}

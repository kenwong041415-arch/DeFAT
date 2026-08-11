package com.defat.core.domain.calc

/** D5 — implemented and tested in Phase 1; no UI wired to it yet. */
object MetCalculator {
    /** kcal = MET x weightKg x hours (docs/04 "Manual exercise fallback"). */
    fun kcal(met: Double, weightKg: Double, hours: Double): Double {
        require(met > 0) { "met must be > 0, was $met" }
        require(weightKg > 0) { "weightKg must be > 0, was $weightKg" }
        require(hours >= 0) { "hours must be >= 0, was $hours" }
        return met * weightKg * hours
    }
}

data class MetActivity(val key: String, val met: Double)

/**
 * Curated MET table (docs/04 "Manual exercise fallback"). Keys are stable
 * string ids, also used as string-resource suffixes.
 *
 * NOTE: docs/04 gives weight training as a 3.5-6 range; this uses the
 * midpoint 5.0 pending owner review (see plan §13 item 4).
 */
object MetTable {
    val ACTIVITIES: List<MetActivity> = listOf(
        MetActivity("walking", 3.5),
        MetActivity("jogging", 7.0),
        MetActivity("cycling_moderate", 6.8),
        MetActivity("swimming", 6.0),
        MetActivity("weight_training", 5.0),
        MetActivity("hiit", 8.0),
        MetActivity("hiking", 6.0),
        MetActivity("badminton", 5.5),
        MetActivity("basketball", 6.5),
        MetActivity("yoga", 2.5),
    )

    fun metFor(key: String): Double? = ACTIVITIES.firstOrNull { it.key == key }?.met
}

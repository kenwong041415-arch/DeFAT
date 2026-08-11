package com.defat.core.domain.model

data class FrequentFood(
    /** Display name — the spelling used in the most recent matching entry. */
    val name: String,
    val timesLogged: Int,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

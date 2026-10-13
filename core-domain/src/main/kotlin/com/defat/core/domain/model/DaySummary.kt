package com.defat.core.domain.model

import java.time.LocalDate

/** One row of the 歷史 tab: a logical day that has at least one meal. */
data class DaySummary(
    val date: LocalDate,
    val intakeKcal: Double,
    val targetKcal: Double,
    val mealCount: Int,
) {
    /** 超標 when strictly over the target; exactly on it counts as 達標. */
    val isOverTarget: Boolean get() = intakeKcal > targetKcal
}

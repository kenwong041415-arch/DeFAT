package com.defat.core.domain.model

import java.time.LocalDate

/** One cell of Home's week date strip (plan §6.3). */
data class WeekDayCell(
    val date: LocalDate,
    val isSelected: Boolean,
    val isToday: Boolean,
    /** At least one meal is logged on this logical day. */
    val hasEntries: Boolean,
    /** After today's logical date — shown, but not selectable. */
    val isFuture: Boolean,
)

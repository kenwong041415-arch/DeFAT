package com.defat.core.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** One total per logical day (plan §11): `water_log(dateEpochDay, ml)`. */
interface WaterRepository {
    /** The day's total in ml; 0 when nothing is logged. */
    fun waterMl(date: LocalDate): Flow<Int>

    /** Replaces the day's total. */
    suspend fun setWaterMl(date: LocalDate, ml: Int)
}

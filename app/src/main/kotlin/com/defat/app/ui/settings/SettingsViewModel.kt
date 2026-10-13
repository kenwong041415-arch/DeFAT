package com.defat.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The 我 tab's day-start setting (plan §7.4). Changing it never rewrites a
 * stored meal — meals keep their instant and only the bucketing into days
 * changes — so there is nothing to migrate and nothing to confirm.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val dayStart: StateFlow<LocalTime> = settingsRepository.dayStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayBoundary.DEFAULT_DAY_START)

    /** 00:00, 00:30, ... 08:00 — the only values the setting accepts. */
    val dayStartOptions: List<LocalTime> =
        (DayBoundary.MIN_DAY_START_MINUTES..DayBoundary.MAX_DAY_START_MINUTES step DayBoundary.DAY_START_STEP_MINUTES)
            .map { DayBoundary.dayStartFromMinutes(it) }

    /** Anything outside 00:00..08:00 or off the 30-minute grid is normalised, not rejected. */
    fun setDayStart(time: LocalTime) {
        val normalised = DayBoundary.dayStartFromMinutes(DayBoundary.toMinutes(time))
        viewModelScope.launch { settingsRepository.setDayStart(normalised) }
    }
}

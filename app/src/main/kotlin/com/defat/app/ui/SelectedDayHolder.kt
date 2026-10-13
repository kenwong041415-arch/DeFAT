package com.defat.app.ui

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one place that remembers which day the student is looking at (plan
 * §6.3/§6.5): Home renders it, the week strip and the 歷史 list set it, and
 * the centre add button opens the meal editor on it — "one place renders a
 * day, not two".
 *
 * `null` means "follow today": choosing today in the week strip stores null
 * rather than a fixed date, so it is not pinned to a day that has since ended.
 * It is in memory only: after process death the app opens on today, which is
 * the right default.
 */
@Singleton
class SelectedDayHolder @Inject constructor() {

    private val _selected = MutableStateFlow<LocalDate?>(null)
    val selected: StateFlow<LocalDate?> = _selected.asStateFlow()

    fun select(date: LocalDate?) {
        _selected.value = date
    }
}

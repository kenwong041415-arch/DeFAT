package com.defat.app.fake

import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.repository.SettingsRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Deliberately stores whatever it is given. The 00:00..08:00 / 30-minute
 * normalisation is the ViewModel's job (and DayBoundary's, tested in
 * :core-domain); a fake that normalised would make SettingsViewModelTest pass
 * even if the ViewModel did not.
 */
class FakeSettingsRepository(initial: LocalTime = DayBoundary.DEFAULT_DAY_START) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    var lastSet: LocalTime? = null
        private set

    override val dayStart: Flow<LocalTime> = state

    override suspend fun setDayStart(time: LocalTime) {
        lastSet = time
        state.value = time
    }
}

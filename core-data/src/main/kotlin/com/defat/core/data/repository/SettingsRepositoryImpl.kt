package com.defat.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.defat.core.data.datastore.ProfileKeys
import com.defat.core.data.di.IoDispatcher
import com.defat.core.domain.calc.DayBoundary
import com.defat.core.domain.repository.SettingsRepository
import java.io.IOException
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Day-start time in the profile DataStore (plan §7.4). Reads and writes both
 * go through [DayBoundary.dayStartFromMinutes], so a stale or hand-edited
 * value can never leave the 00:00..08:00, 30-minute grid.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {

    override val dayStart: Flow<LocalTime> =
        dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs ->
                val minutes = prefs[ProfileKeys.DAY_START_MINUTES]
                    ?: DayBoundary.toMinutes(DayBoundary.DEFAULT_DAY_START)
                DayBoundary.dayStartFromMinutes(minutes)
            }
            .distinctUntilChanged()
            .flowOn(io)

    override suspend fun setDayStart(time: LocalTime) = withContext(io) {
        val minutes = DayBoundary.toMinutes(DayBoundary.dayStartFromMinutes(DayBoundary.toMinutes(time)))
        dataStore.edit { prefs ->
            prefs[ProfileKeys.DAY_START_MINUTES] = minutes
            // The day-start setting travels in the profile document, so it is a
            // profile write as far as sync is concerned.
            prefs[ProfileKeys.PROFILE_UPDATED_AT] = clock.millis()
        }
        Unit
    }
}

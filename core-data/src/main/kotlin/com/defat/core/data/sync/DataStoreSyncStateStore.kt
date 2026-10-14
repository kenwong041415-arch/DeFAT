package com.defat.core.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.defat.core.data.di.IoDispatcher
import com.defat.core.domain.sync.SYNCED_SUBCOLLECTIONS
import com.defat.core.domain.sync.SyncCollections
import com.defat.core.domain.sync.SyncStateStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Sync facts in the same DataStore file as the profile (one DataStore, as for
 * the day-start setting). Keys are prefixed `sync_` so they are visibly a
 * different group from ProfileKeys.
 */
@Singleton
class DataStoreSyncStateStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncStateStore {

    private val data: Flow<Preferences> =
        dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val lastSignedInUid: Flow<String?> = data.map { it[LAST_UID] }.distinctUntilChanged()
    override val lastSyncAt: Flow<Long?> = data.map { it[LAST_SYNC_AT] }.distinctUntilChanged()

    override suspend fun getLastSignedInUid(): String? = withContext(io) { data.first()[LAST_UID] }

    override suspend fun setLastSignedInUid(uid: String) = withContext(io) {
        dataStore.edit { it[LAST_UID] = uid }
        Unit
    }

    override suspend fun pullCursor(collection: String): Long = withContext(io) {
        data.first()[cursorKey(collection)] ?: 0L
    }

    override suspend fun setPullCursor(collection: String, millis: Long) = withContext(io) {
        dataStore.edit { it[cursorKey(collection)] = millis }
        Unit
    }

    override suspend fun resetPullCursors() = withContext(io) {
        dataStore.edit { prefs -> allCollections.forEach { prefs.remove(cursorKey(it)) } }
        Unit
    }

    override suspend fun setLastSyncAt(millis: Long) = withContext(io) {
        dataStore.edit { it[LAST_SYNC_AT] = millis }
        Unit
    }

    override suspend fun clearAll() = withContext(io) {
        dataStore.edit { prefs ->
            prefs.remove(LAST_UID)
            prefs.remove(LAST_SYNC_AT)
            allCollections.forEach { prefs.remove(cursorKey(it)) }
        }
        Unit
    }

    private val allCollections: List<String> = SYNCED_SUBCOLLECTIONS + SyncCollections.PROFILE

    private fun cursorKey(collection: String) = longPreferencesKey("sync_cursor_$collection")

    private companion object {
        val LAST_UID = stringPreferencesKey("sync_last_signed_in_uid")
        val LAST_SYNC_AT = longPreferencesKey("sync_last_sync_at")
    }
}

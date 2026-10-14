package com.defat.core.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.defat.core.data.datastore.ProfileKeys
import com.defat.core.data.di.IoDispatcher
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Sex
import com.defat.core.domain.sync.SyncCollections
import com.defat.core.domain.sync.SyncDoc
import com.defat.core.domain.sync.SyncStamp
import com.defat.core.domain.sync.SyncableCollection
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * The profile document, `users/{uid}` itself. It lives in the profile DataStore
 * rather than Room, so its `updatedAt` / `syncedAt` are two DataStore keys
 * (ProfileKeys.PROFILE_UPDATED_AT / PROFILE_SYNCED_AT) with the same meaning as
 * the Room columns.
 *
 * Fields follow phase1.5 plan §4: sex, birthDate, heightCm, activityLevel,
 * goal{...}, dayStartMinutes, role — plus the current weight, body-fat % and
 * disclaimer time, which the app needs to rebuild a complete profile on a
 * second device. `role` is always "student" here; nothing in this phase reads
 * it (see the review notes: it must never become an authorisation input while a
 * client can write it).
 */
@Singleton
class ProfileSyncSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncableCollection {
    override val name = SyncCollections.PROFILE

    private suspend fun currentPrefs(): Preferences =
        dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.first()

    private fun Preferences.hasProfile(): Boolean = this[ProfileKeys.ONBOARDING_COMPLETE] == true

    override suspend fun hasRows() = withContext(io) { currentPrefs().hasProfile() }

    override suspend fun pending(): List<SyncDoc> = withContext(io) {
        val prefs = currentPrefs()
        val doc = prefs.toSyncDoc() ?: return@withContext emptyList()
        val synced = prefs[ProfileKeys.PROFILE_SYNCED_AT]
        if (synced == null || synced < doc.updatedAt) listOf(doc) else emptyList()
    }

    /** Conditional on the profile not having been edited since [doc] was read. */
    override suspend fun markSynced(doc: SyncDoc) = withContext(io) {
        dataStore.edit { prefs ->
            if ((prefs[ProfileKeys.PROFILE_UPDATED_AT] ?: 0L) == doc.updatedAt) {
                prefs[ProfileKeys.PROFILE_SYNCED_AT] = doc.updatedAt
            }
        }
        Unit
    }

    override suspend fun stamp(id: String): SyncStamp? = withContext(io) {
        val prefs = currentPrefs()
        if (prefs.hasProfile()) SyncStamp(prefs[ProfileKeys.PROFILE_UPDATED_AT] ?: 0L) else null
    }

    override suspend fun applyRemote(doc: SyncDoc) = withContext(io) {
        val f = doc.fields
        val sex = (f["sex"] as? String)?.let { runCatching { Sex.valueOf(it) }.getOrNull() }
        val activity = (f["activityLevel"] as? String)?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() }
        val birthDate = (f["birthDate"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val height = (f["heightCm"] as? Number)?.toDouble()
        val weight = (f["weightKg"] as? Number)?.toDouble()
        val goal = f["goal"] as? Map<*, *>
        val goalWeight = (goal?.get("targetWeightKg") as? Number)?.toDouble()
        // A profile missing anything the app needs would leave onboarding
        // "complete" but unreadable; skip it rather than store half of one.
        if (sex == null || activity == null || birthDate == null || height == null || weight == null || goalWeight == null) {
            return@withContext
        }
        val bodyFat = (f["bodyFatPct"] as? Number)?.toDouble()
        val goalFat = (goal?.get("targetBodyFatPct") as? Number)?.toDouble()
        val goalDate = (goal?.get("targetDate") as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val acceptedAt = (f["disclaimerAcceptedAt"] as? Number)?.toLong()
        val dayStart = (f["dayStartMinutes"] as? Number)?.toInt()

        dataStore.edit { prefs ->
            prefs[ProfileKeys.SEX] = sex.name
            prefs[ProfileKeys.BIRTH_DATE] = birthDate.toString()
            prefs[ProfileKeys.HEIGHT_CM] = height
            prefs[ProfileKeys.WEIGHT_KG] = weight
            if (bodyFat != null) prefs[ProfileKeys.BODY_FAT_PCT] = bodyFat else prefs.remove(ProfileKeys.BODY_FAT_PCT)
            prefs[ProfileKeys.ACTIVITY_LEVEL] = activity.name
            prefs[ProfileKeys.GOAL_TARGET_WEIGHT_KG] = goalWeight
            if (goalFat != null) prefs[ProfileKeys.GOAL_TARGET_FAT_PCT] = goalFat else prefs.remove(ProfileKeys.GOAL_TARGET_FAT_PCT)
            if (goalDate != null) prefs[ProfileKeys.GOAL_TARGET_DATE] = goalDate.toString() else prefs.remove(ProfileKeys.GOAL_TARGET_DATE)
            if (acceptedAt != null) prefs[ProfileKeys.DISCLAIMER_ACCEPTED_AT] = acceptedAt else prefs.remove(ProfileKeys.DISCLAIMER_ACCEPTED_AT)
            if (dayStart != null) prefs[ProfileKeys.DAY_START_MINUTES] = dayStart
            prefs[ProfileKeys.ONBOARDING_COMPLETE] = true
            prefs[ProfileKeys.PROFILE_UPDATED_AT] = doc.updatedAt
            prefs[ProfileKeys.PROFILE_SYNCED_AT] = doc.updatedAt
        }
        Unit
    }

    /** Case A: forget that the profile was ever uploaded. */
    override suspend fun markAllPending() = withContext(io) {
        dataStore.edit { prefs -> prefs.remove(ProfileKeys.PROFILE_SYNCED_AT) }
        Unit
    }

    /**
     * Removes the profile keys only. The sync-state keys (last uid, cursors) are
     * a different group and are cleared separately by SyncStateStore.clearAll,
     * because Case C "Replace" must keep them and account deletion must not.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun deleteAllLocal() = withContext(io) {
        dataStore.edit { prefs ->
            ProfileKeys.ALL_PROFILE_KEYS.forEach { key -> prefs.remove(key as Preferences.Key<Any>) }
        }
        Unit
    }

    override suspend fun liveDocs(): List<SyncDoc> = withContext(io) {
        listOfNotNull(currentPrefs().toSyncDoc())
    }

    /** The profile as a document, or null if onboarding has not produced one. */
    private fun Preferences.toSyncDoc(): SyncDoc? {
        if (!hasProfile()) return null
        val sex = this[ProfileKeys.SEX] ?: return null
        val birthDate = this[ProfileKeys.BIRTH_DATE] ?: return null
        val height = this[ProfileKeys.HEIGHT_CM] ?: return null
        val weight = this[ProfileKeys.WEIGHT_KG] ?: return null
        val activity = this[ProfileKeys.ACTIVITY_LEVEL] ?: return null
        val goalWeight = this[ProfileKeys.GOAL_TARGET_WEIGHT_KG] ?: return null
        return SyncDoc(
            id = PROFILE_DOC_ID,
            updatedAt = this[ProfileKeys.PROFILE_UPDATED_AT] ?: 0L,
            deletedAt = null,
            fields = linkedMapOf(
                "sex" to sex,
                "birthDate" to birthDate,
                "heightCm" to height,
                "weightKg" to weight,
                "bodyFatPct" to this[ProfileKeys.BODY_FAT_PCT],
                "activityLevel" to activity,
                "goal" to linkedMapOf(
                    "targetWeightKg" to goalWeight,
                    "targetBodyFatPct" to this[ProfileKeys.GOAL_TARGET_FAT_PCT],
                    "targetDate" to this[ProfileKeys.GOAL_TARGET_DATE],
                ),
                "disclaimerAcceptedAt" to this[ProfileKeys.DISCLAIMER_ACCEPTED_AT],
                "dayStartMinutes" to this[ProfileKeys.DAY_START_MINUTES],
                // No `role` field. The Phase 1.5 plan put one here, defaulting to
                // "student", so Phase 5 would not need a migration. But the rules
                // let a user write their own profile document, so a client-written
                // `role` is a field the user controls — and a Phase 5 that granted
                // trainer access from it would be handing out that access on
                // request. Leaving it out means there is nothing to mistake for an
                // authority. Phase 5 sets the role server-side (console, or a
                // Cloud Function the client cannot call), on a document the user
                // cannot write. See docs/06.
            ),
        )
    }

    private companion object {
        const val PROFILE_DOC_ID = "profile"
    }
}

package com.defat.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.defat.core.domain.model.ActivityLevel
import com.defat.core.domain.model.Goal
import com.defat.core.domain.model.Sex
import com.defat.core.domain.model.UserProfile
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Reads/writes the profile from Preferences DataStore. Emits null from
 * [profile] when any field required to build a complete [UserProfile] is
 * missing (i.e. onboarding is not finished yet).
 */
class ProfileLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val profile: Flow<UserProfile?> =
        dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs -> prefs.toUserProfileOrNull() }

    val onboardingComplete: Flow<Boolean> =
        dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs -> prefs[ProfileKeys.ONBOARDING_COMPLETE] ?: false }

    suspend fun save(profile: UserProfile) {
        // Nullable properties are read into locals first: they belong to a class in
        // another module, so Kotlin cannot smart-cast them after a null check.
        val bodyFatPct = profile.bodyFatPct
        val goalFatPct = profile.goal.targetBodyFatPct
        val goalDate = profile.goal.targetDate
        val acceptedAt = profile.disclaimerAcceptedAt

        dataStore.edit { prefs ->
            prefs[ProfileKeys.SEX] = profile.sex.name
            prefs[ProfileKeys.BIRTH_DATE] = profile.birthDate.toString()
            prefs[ProfileKeys.HEIGHT_CM] = profile.heightCm
            prefs[ProfileKeys.WEIGHT_KG] = profile.weightKg
            if (bodyFatPct != null) {
                prefs[ProfileKeys.BODY_FAT_PCT] = bodyFatPct
            } else {
                prefs.remove(ProfileKeys.BODY_FAT_PCT)
            }
            prefs[ProfileKeys.ACTIVITY_LEVEL] = profile.activityLevel.name
            prefs[ProfileKeys.GOAL_TARGET_WEIGHT_KG] = profile.goal.targetWeightKg
            if (goalFatPct != null) {
                prefs[ProfileKeys.GOAL_TARGET_FAT_PCT] = goalFatPct
            } else {
                prefs.remove(ProfileKeys.GOAL_TARGET_FAT_PCT)
            }
            if (goalDate != null) {
                prefs[ProfileKeys.GOAL_TARGET_DATE] = goalDate.toString()
            } else {
                prefs.remove(ProfileKeys.GOAL_TARGET_DATE)
            }
            if (acceptedAt != null) {
                prefs[ProfileKeys.DISCLAIMER_ACCEPTED_AT] = acceptedAt.toEpochMilli()
            } else {
                prefs.remove(ProfileKeys.DISCLAIMER_ACCEPTED_AT)
            }
            prefs[ProfileKeys.ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun updateBody(weightKg: Double, bodyFatPct: Double?) {
        dataStore.edit { prefs ->
            prefs[ProfileKeys.WEIGHT_KG] = weightKg
            if (bodyFatPct != null) {
                prefs[ProfileKeys.BODY_FAT_PCT] = bodyFatPct
            } else {
                prefs.remove(ProfileKeys.BODY_FAT_PCT)
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toUserProfileOrNull(): UserProfile? {
        val sexName = this[ProfileKeys.SEX] ?: return null
        val birthDateStr = this[ProfileKeys.BIRTH_DATE] ?: return null
        val heightCm = this[ProfileKeys.HEIGHT_CM] ?: return null
        val weightKg = this[ProfileKeys.WEIGHT_KG] ?: return null
        val activityLevelName = this[ProfileKeys.ACTIVITY_LEVEL] ?: return null
        val goalTargetWeightKg = this[ProfileKeys.GOAL_TARGET_WEIGHT_KG] ?: return null

        val sex = runCatching { Sex.valueOf(sexName) }.getOrNull() ?: return null
        val activityLevel = runCatching { ActivityLevel.valueOf(activityLevelName) }.getOrNull() ?: return null
        val birthDate = runCatching { LocalDate.parse(birthDateStr) }.getOrNull() ?: return null

        val bodyFatPct = this[ProfileKeys.BODY_FAT_PCT]
        val goalTargetFatPct = this[ProfileKeys.GOAL_TARGET_FAT_PCT]
        val goalTargetDate = this[ProfileKeys.GOAL_TARGET_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val disclaimerAcceptedAt = this[ProfileKeys.DISCLAIMER_ACCEPTED_AT]?.let { Instant.ofEpochMilli(it) }

        return UserProfile(
            sex = sex,
            birthDate = birthDate,
            heightCm = heightCm,
            weightKg = weightKg,
            bodyFatPct = bodyFatPct,
            activityLevel = activityLevel,
            goal = Goal(
                targetWeightKg = goalTargetWeightKg,
                targetBodyFatPct = goalTargetFatPct,
                targetDate = goalTargetDate,
            ),
            disclaimerAcceptedAt = disclaimerAcceptedAt,
        )
    }
}

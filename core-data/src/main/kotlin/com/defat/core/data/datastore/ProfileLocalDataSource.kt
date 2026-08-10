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
        dataStore.edit { prefs ->
            prefs[ProfileKeys.SEX] = profile.sex.name
            prefs[ProfileKeys.BIRTH_DATE] = profile.birthDate.toString()
            prefs[ProfileKeys.HEIGHT_CM] = profile.heightCm
            prefs[ProfileKeys.WEIGHT_KG] = profile.weightKg
            if (profile.bodyFatPct != null) {
                prefs[ProfileKeys.BODY_FAT_PCT] = profile.bodyFatPct
            } else {
                prefs.remove(ProfileKeys.BODY_FAT_PCT)
            }
            prefs[ProfileKeys.ACTIVITY_LEVEL] = profile.activityLevel.name
            prefs[ProfileKeys.GOAL_TARGET_WEIGHT_KG] = profile.goal.targetWeightKg
            if (profile.goal.targetBodyFatPct != null) {
                prefs[ProfileKeys.GOAL_TARGET_FAT_PCT] = profile.goal.targetBodyFatPct
            } else {
                prefs.remove(ProfileKeys.GOAL_TARGET_FAT_PCT)
            }
            if (profile.goal.targetDate != null) {
                prefs[ProfileKeys.GOAL_TARGET_DATE] = profile.goal.targetDate.toString()
            } else {
                prefs.remove(ProfileKeys.GOAL_TARGET_DATE)
            }
            if (profile.disclaimerAcceptedAt != null) {
                prefs[ProfileKeys.DISCLAIMER_ACCEPTED_AT] = profile.disclaimerAcceptedAt.toEpochMilli()
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

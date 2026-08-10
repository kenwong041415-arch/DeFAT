package com.defat.core.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Preferences DataStore keys — file "profile.preferences_pb". */
object ProfileKeys {
    val SEX = stringPreferencesKey("sex") // Sex.name
    val BIRTH_DATE = stringPreferencesKey("birth_date") // ISO yyyy-MM-dd
    val HEIGHT_CM = doublePreferencesKey("height_cm")
    val WEIGHT_KG = doublePreferencesKey("weight_kg") // current weight; updated by LogWeightUseCase
    val BODY_FAT_PCT = doublePreferencesKey("body_fat_pct") // absent = unknown
    val ACTIVITY_LEVEL = stringPreferencesKey("activity_level") // ActivityLevel.name
    val GOAL_TARGET_WEIGHT_KG = doublePreferencesKey("goal_target_weight_kg")
    val GOAL_TARGET_FAT_PCT = doublePreferencesKey("goal_target_fat_pct") // optional
    val GOAL_TARGET_DATE = stringPreferencesKey("goal_target_date") // optional ISO date
    val DISCLAIMER_ACCEPTED_AT = longPreferencesKey("disclaimer_accepted_at") // epoch millis
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
}

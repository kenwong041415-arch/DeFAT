package com.defat.app.ui.exercise

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.defat.app.R

/**
 * The display name for a stored activity key (MetTable keys, plus the three
 * weight-training levels). Null for a key this build has no name for, so the
 * caller can fall back to the raw key instead of crashing on a row written by
 * a newer build.
 */
@StringRes
fun exerciseActivityLabelRes(key: String): Int? = when (key) {
    "walking" -> R.string.exercise_activity_walking
    "jogging" -> R.string.exercise_activity_jogging
    "cycling_moderate" -> R.string.exercise_activity_cycling_moderate
    "swimming" -> R.string.exercise_activity_swimming
    "weight_training" -> R.string.exercise_activity_weight_training
    "weight_training_light" -> R.string.exercise_activity_weight_training_light
    "weight_training_heavy" -> R.string.exercise_activity_weight_training_heavy
    "hiit" -> R.string.exercise_activity_hiit
    "hiking" -> R.string.exercise_activity_hiking
    "badminton" -> R.string.exercise_activity_badminton
    "basketball" -> R.string.exercise_activity_basketball
    "yoga" -> R.string.exercise_activity_yoga
    else -> null
}

/** 輕 / 中 / 重 for the weight-training intensity chips. */
@StringRes
fun weightTrainingLevelLabelRes(key: String): Int = when (key) {
    "weight_training_light" -> R.string.exercise_level_light
    "weight_training_heavy" -> R.string.exercise_level_heavy
    else -> R.string.exercise_level_medium
}

/** The display name for [key], or the raw key when this build has no name for it. */
@Composable
fun exerciseActivityName(key: String): String {
    val res = exerciseActivityLabelRes(key)
    return if (res != null) stringResource(res) else key
}

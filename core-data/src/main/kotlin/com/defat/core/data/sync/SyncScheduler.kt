package com.defat.core.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val UNIQUE_NAME = "defat-periodic-sync"
    private const val UNIQUE_CATCH_UP = "defat-catch-up-sync"
    private const val PERIOD_HOURS = 6L

    private fun connected() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Idempotent: KEEP leaves an already-scheduled job alone. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_HOURS, TimeUnit.HOURS)
            .setConstraints(connected())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Queues one sync for the moment the phone has a network again.
     *
     * Called when a sync pass fails. The periodic job alone does not satisfy
     * the phase's exit test — "logs a meal with the network off and watches it
     * appear in Firestore once the network returns" — because a 6-hour timer
     * can leave a meal unsynced for most of a day. WorkManager holds this
     * request until its CONNECTED constraint is met, which is the signal we
     * actually want, and survives the app being killed.
     *
     * REPLACE, not KEEP: a newer failure should reset the backoff rather than
     * queue behind a stale attempt.
     */
    fun scheduleCatchUp(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(connected())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_CATCH_UP, ExistingWorkPolicy.REPLACE, request)
    }
}

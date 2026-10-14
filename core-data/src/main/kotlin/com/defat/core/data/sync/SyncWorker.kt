package com.defat.core.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.defat.core.domain.sync.SyncCoordinator
import com.defat.core.domain.sync.SyncOutcome
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * The periodic push (phase1.5 plan D6). It runs the same pass as every other
 * trigger; in particular a pass that meets another account's records (Case C)
 * does nothing and reports success, because only the sign-in screen may ask the
 * user whether to replace them.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun syncCoordinator(): SyncCoordinator
    }

    override suspend fun doWork(): Result {
        val coordinator = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .syncCoordinator()
        return when (coordinator.syncNow()) {
            // Retry a few times with WorkManager's backoff, then wait for the next period.
            is SyncOutcome.Failed -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}

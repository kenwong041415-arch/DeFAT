package com.defat.app

import android.app.Application
import com.defat.app.ui.profile.ExportShare
import com.defat.core.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DefatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // An export file is the user's health data in a cache directory; never
        // let one outlive the session that created it.
        ExportShare.purge(this)
        // The 6-hourly push (phase1.5 plan D6). Idempotent.
        SyncScheduler.schedule(this)
    }
}

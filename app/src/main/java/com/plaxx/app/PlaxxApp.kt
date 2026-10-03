package com.plaxx.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.plaxx.app.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

class PlaxxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // WorkManager rejestruje sie zawsze - worker sam wyjdzie natychmiast
        // jesli nie ma zapisanych plan_ids w inputData (patrz SyncWorker).
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS).build()
        )
    }
}

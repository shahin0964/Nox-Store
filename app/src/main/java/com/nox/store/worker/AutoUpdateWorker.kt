package com.nox.store.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nox.store.NoxStoreApp
import java.util.concurrent.TimeUnit

class AutoUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic background update check...")

        val settingsRepo = NoxStoreApp.instance.settingsRepository
        val appsRepo = NoxStoreApp.instance.appsRepository
        val downloadManager = NoxStoreApp.instance.downloadManager

        val settings = settingsRepo.settings.value
        if (!settings.autoUpdateEnabled) {
            Log.d(TAG, "Auto-update is disabled by user.")
            return Result.success()
        }

        // Refresh installed packages and sync real releases from GitHub
        appsRepo.refreshInstalledStatus()
        appsRepo.syncAllFromGitHub()

        val apps = appsRepo.apps.value
        val updates = apps.filter { it.hasUpdate }

        if (updates.isEmpty()) {
            Log.i(TAG, "All apps are up to date.")
            return Result.success()
        }

        Log.i(TAG, "Found ${updates.size} updates: ${updates.joinToString { it.name }}")

        if (settings.notifyOnUpdate) {
            val names = updates.joinToString(", ") { it.name }
            NotificationHelper.showUpdatesAvailable(context, updates.size, names)
        }

        if (settings.unattendedMode) {
            // Android 12+ Silent background update
            for (app in updates) {
                Log.i(TAG, "Initiating silent background update for ${app.name}...")
                downloadManager.startDownload(
                    app = app,
                    autoInstall = true,
                    unattended = true
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "AutoUpdateWorker"
        const val WORK_NAME = "nox_store_periodic_update"

        fun schedule(context: Context, intervalHours: Long, wifiOnly: Boolean) {
            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoUpdateWorker>(
                intervalHours.coerceAtLeast(1L),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.i(TAG, "Scheduled auto-update worker every $intervalHours hours (wifiOnly=$wifiOnly)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled auto-update worker")
        }
    }
}

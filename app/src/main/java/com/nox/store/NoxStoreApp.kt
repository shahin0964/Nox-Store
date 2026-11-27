package com.nox.store

import android.app.Application
import com.nox.store.data.repository.AppsRepository
import com.nox.store.data.repository.DownloadManager
import com.nox.store.data.repository.SettingsRepository
import com.nox.store.worker.AutoUpdateWorker
import com.nox.store.worker.NotificationHelper

class NoxStoreApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var appsRepository: AppsRepository
        private set

    lateinit var downloadManager: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = SettingsRepository(this)
        appsRepository = AppsRepository(this)
        downloadManager = DownloadManager(this)

        NotificationHelper.createChannels(this)

        val settings = settingsRepository.settings.value
        if (settings.autoUpdateEnabled) {
            AutoUpdateWorker.schedule(
                context = this,
                intervalHours = settings.autoUpdateIntervalHours,
                wifiOnly = settings.wifiOnly
            )
        }
    }

    companion object {
        lateinit var instance: NoxStoreApp
            private set
    }
}

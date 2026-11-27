package com.nox.store.data.model

data class StoreSettings(
    val repoUrl: String = "https://raw.githubusercontent.com/shahin0964/Nox-Store/main/apps.json",
    val autoUpdateEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val notifyOnUpdate: Boolean = true,
    val unattendedMode: Boolean = true, // Android 12+ PackageInstaller without user interaction
    val autoUpdateIntervalHours: Long = 12L,
    val darkTheme: Boolean = true
)

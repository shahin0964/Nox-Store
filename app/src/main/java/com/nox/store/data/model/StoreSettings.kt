package com.nox.store.data.model

enum class AppThemeMode(val label: String, val subtitle: String) {
    SYSTEM("System", "Follow phone light or dark mode"),
    LIGHT("Light", "Clean pure white appearance"),
    DARK("Dark", "Nox midnight slate theme"),
    AMOLED("AMOLED", "Pure #000000 black for OLED")
}

data class StoreSettings(
    val repoUrl: String = "https://raw.githubusercontent.com/shahin0964/Nox-Store/main/apps.json",
    val autoUpdateEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val notifyOnUpdate: Boolean = true,
    val unattendedMode: Boolean = true, // Android 12+ PackageInstaller without user interaction
    val autoUpdateIntervalHours: Long = 12L,
    val darkTheme: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColors: Boolean = true
)

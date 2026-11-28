package com.nox.store.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nox.store.data.model.AppThemeMode
import com.nox.store.data.model.StoreSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nox_store_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<StoreSettings> = _settings.asStateFlow()

    private fun loadSettings(): StoreSettings {
        val savedThemeName = prefs.getString(KEY_THEME_MODE, null)
        val themeMode = if (savedThemeName != null) {
            AppThemeMode.entries.firstOrNull { it.name == savedThemeName } ?: AppThemeMode.SYSTEM
        } else {
            AppThemeMode.SYSTEM
        }
        return StoreSettings(
            repoUrl = prefs.getString(
                KEY_REPO_URL,
                "https://raw.githubusercontent.com/shahin0964/Nox-Store/main/apps.json"
            ) ?: "https://raw.githubusercontent.com/shahin0964/Nox-Store/main/apps.json",
            autoUpdateEnabled = prefs.getBoolean(KEY_AUTO_UPDATE, true),
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            notifyOnUpdate = prefs.getBoolean(KEY_NOTIFY_UPDATE, true),
            unattendedMode = prefs.getBoolean(KEY_UNATTENDED, true),
            autoUpdateIntervalHours = prefs.getLong(KEY_INTERVAL, 12L),
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
            themeMode = themeMode,
            dynamicColors = prefs.getBoolean(KEY_DYNAMIC_COLORS, true)
        )
    }

    fun updateSettings(newSettings: StoreSettings) {
        prefs.edit()
            .putString(KEY_REPO_URL, newSettings.repoUrl)
            .putBoolean(KEY_AUTO_UPDATE, newSettings.autoUpdateEnabled)
            .putBoolean(KEY_WIFI_ONLY, newSettings.wifiOnly)
            .putBoolean(KEY_NOTIFY_UPDATE, newSettings.notifyOnUpdate)
            .putBoolean(KEY_UNATTENDED, newSettings.unattendedMode)
            .putLong(KEY_INTERVAL, newSettings.autoUpdateIntervalHours)
            .putBoolean(KEY_DARK_THEME, newSettings.themeMode != AppThemeMode.LIGHT)
            .putString(KEY_THEME_MODE, newSettings.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLORS, newSettings.dynamicColors)
            .apply()
        _settings.value = newSettings
    }

    companion object {
        private const val KEY_REPO_URL = "repo_url"
        private const val KEY_AUTO_UPDATE = "auto_update_enabled"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_NOTIFY_UPDATE = "notify_on_update"
        private const val KEY_UNATTENDED = "unattended_mode"
        private const val KEY_INTERVAL = "interval_hours"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
    }
}

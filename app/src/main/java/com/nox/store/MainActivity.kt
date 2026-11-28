package com.nox.store

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.nox.store.ui.screens.MainScaffold
import com.nox.store.ui.theme.NoxStoreTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission callback
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and ask for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = NoxStoreApp.instance

        setContent {
            val settings by app.settingsRepository.settings.collectAsState()
            NoxStoreTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColors
            ) {
                MainScaffold(
                    appsRepository = app.appsRepository,
                    downloadManager = app.downloadManager,
                    settingsRepository = app.settingsRepository,
                    developerRepository = app.developerRepository,
                    adminRepository = app.adminRepository
                )
            }
        }
    }
}

package com.nox.store.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.data.repository.AdminRepository
import com.nox.store.data.repository.AppsRepository
import com.nox.store.data.repository.DeveloperRepository
import com.nox.store.data.repository.DownloadManager
import com.nox.store.data.repository.SettingsRepository
import com.nox.store.ui.theme.NoxAccentPink

enum class StoreTab(val title: String, val icon: ImageVector) {
    HOME("Apps", Icons.Rounded.Apps),
    UPDATES("Updates", Icons.Rounded.SystemUpdate),
    DOWNLOADS("Downloads", Icons.Rounded.Download),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun MainScaffold(
    appsRepository: AppsRepository,
    downloadManager: DownloadManager,
    settingsRepository: SettingsRepository,
    developerRepository: DeveloperRepository,
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val apps by appsRepository.apps.collectAsState()
    val isRefreshing by appsRepository.isRefreshing.collectAsState()
    val downloadStates by downloadManager.downloadStates.collectAsState()
    val settings by settingsRepository.settings.collectAsState()
    val storeNotice by adminRepository.storeNotice.collectAsState()
    val developerProfile by developerRepository.developerProfile.collectAsState()

    var currentTab by remember { mutableStateOf(StoreTab.HOME) }
    var selectedAppDetail by remember { mutableStateOf<AppItem?>(null) }
    var showDeveloperStudio by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }

    // Real-time refresh whenever the user returns to Nox Store (e.g., after uninstalling or installing an app)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appsRepository.refreshInstalledStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Real-time BroadcastReceiver for package install / uninstall / update events
    DisposableEffect(context) {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.data?.schemeSpecificPart
                appsRepository.refreshInstalledStatus()
                if (pkg != null && intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                    downloadManager.clearState(pkg)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(packageReceiver)
            } catch (_: Exception) {}
        }
    }

    // Keep download states synchronized with actual installed status (clears stale Installed states after uninstall)
    LaunchedEffect(apps) {
        apps.forEach { app ->
            val st = downloadStates[app.packageName]
            if (!app.isInstalled && st is DownloadState.Installed) {
                downloadManager.clearState(app.packageName)
            }
        }
    }

    val updatesCount = apps.count { it.hasUpdate }

    // Intercept back button
    BackHandler(enabled = selectedAppDetail != null || showDeveloperStudio || showAdminPanel) {
        if (showAdminPanel) {
            showAdminPanel = false
        } else if (selectedAppDetail != null) {
            selectedAppDetail = null
        } else if (showDeveloperStudio) {
            showDeveloperStudio = false
        }
    }

    val handleAppAction: (AppItem) -> Unit = { app ->
        if (app.isInstalled && !app.hasUpdate) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        } else {
            downloadManager.startDownload(
                app = app,
                autoInstall = true,
                unattended = settings.unattendedMode
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selectedAppDetail == null && !showDeveloperStudio && !showAdminPanel) {
                StoreBottomNav(
                    currentTab = currentTab,
                    updatesCount = updatesCount,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                showAdminPanel -> {
                    AdminPanelScreen(
                        adminRepository = adminRepository,
                        appsRepository = appsRepository,
                        ownerToken = developerProfile?.token,
                        onBackClick = { showAdminPanel = false }
                    )
                }

                showDeveloperStudio -> {
                    DeveloperStudioScreen(
                        developerRepository = developerRepository,
                        appsRepository = appsRepository,
                        onBackClick = { showDeveloperStudio = false },
                        onOpenAdminPanel = { showAdminPanel = true }
                    )
                }

                selectedAppDetail != null -> {
                    val app = selectedAppDetail!!
                    val currentAppInList = apps.firstOrNull {
                        it.id == app.id || it.packageName == app.packageName
                    } ?: app
                    val downloadState = downloadStates[currentAppInList.packageName]
                        ?: downloadStates[app.packageName]
                        ?: DownloadState.Idle

                    AppDetailScreen(
                        app = currentAppInList,
                        downloadState = downloadState,
                        onBackClick = { selectedAppDetail = null },
                        onActionClick = { handleAppAction(currentAppInList) },
                        onCancelDownload = { downloadManager.cancelDownload(currentAppInList.packageName) }
                    )
                }

                currentTab == StoreTab.HOME -> {
                    HomeScreen(
                        apps = apps,
                        downloadStates = downloadStates,
                        onAppClick = { selectedAppDetail = it },
                        onAppAction = handleAppAction,
                        onNavigateToUpdates = { currentTab = StoreTab.UPDATES },
                        onProfileClick = { showDeveloperStudio = true },
                        adminNotice = storeNotice
                    )
                }

                currentTab == StoreTab.UPDATES -> {
                    UpdatesScreen(
                        apps = apps,
                        downloadStates = downloadStates,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            appsRepository.refreshInstalledStatus()
                            appsRepository.fetchRemoteCentralRegistry()
                        },
                        onAppClick = { selectedAppDetail = it },
                        onUpdateApp = handleAppAction,
                        onUpdateAll = {
                            val updates = apps.filter { it.hasUpdate }
                            updates.forEach { updateApp ->
                                downloadManager.startDownload(
                                    app = updateApp,
                                    autoInstall = true,
                                    unattended = settings.unattendedMode
                                )
                            }
                        }
                    )
                }

                currentTab == StoreTab.DOWNLOADS -> {
                    DownloadsScreen(
                        apps = apps,
                        downloadStates = downloadStates,
                        onCancelDownload = { downloadManager.cancelDownload(it) },
                        onRetryDownload = { handleAppAction(it) }
                    )
                }

                currentTab == StoreTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onSettingsChanged = { settingsRepository.updateSettings(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun StoreBottomNav(
    currentTab: StoreTab,
    updatesCount: Int,
    onTabSelected: (StoreTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                val contentColor = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BadgedBox(
                        badge = {
                            if (tab == StoreTab.UPDATES && updatesCount > 0) {
                                Badge(
                                    containerColor = NoxAccentPink,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "$updatesCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}

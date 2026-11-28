package com.nox.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.ui.components.AppIcon
import com.nox.store.ui.theme.NoxInstallGreen

@Composable
fun DownloadsScreen(
    apps: List<AppItem>,
    downloadStates: Map<String, DownloadState>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeDownloads = downloadStates.filter { (_, state) ->
        state !is DownloadState.Idle
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Fixed / Sticky Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Downloads Manager",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${activeDownloads.size} active operations",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        // Scrollable download list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (activeDownloads.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceVariant)
                                    .border(1.dp, colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Active Downloads",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Apps you install or update will show progress here.",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(activeDownloads.entries.toList()) { (pkg, state) ->
                    val app = apps.firstOrNull { it.packageName == pkg }
                    val appName = app?.name ?: pkg

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.surfaceVariant)
                            .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        when (state) {
                            is DownloadState.Downloading -> {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIcon(
                                            iconUrl = app?.iconUrl ?: "",
                                            appName = appName,
                                            sizeDp = 42,
                                            packageName = app?.packageName
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = appName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${state.formattedProgress} • ${state.formattedSpeed}",
                                                fontSize = 11.sp,
                                                color = colorScheme.secondary
                                            )
                                        }
                                        IconButton(onClick = { onCancelDownload(pkg) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Cancel",
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = colorScheme.secondary,
                                        trackColor = colorScheme.outline
                                    )
                                }
                            }

                            is DownloadState.Connecting -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(iconUrl = app?.iconUrl ?: "", appName = appName, sizeDp = 42, packageName = app?.packageName)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Connecting to CDN...",
                                            fontSize = 11.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is DownloadState.Downloaded -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(iconUrl = app?.iconUrl ?: "", appName = appName, sizeDp = 42, packageName = app?.packageName)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Downloaded — ready to install",
                                            fontSize = 11.sp,
                                            color = NoxInstallGreen
                                        )
                                    }
                                    if (app != null) {
                                        IconButton(onClick = { onRetryDownload(app) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Install",
                                                tint = NoxInstallGreen
                                            )
                                        }
                                    }
                                }
                            }

                            is DownloadState.Installing -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(iconUrl = app?.iconUrl ?: "", appName = appName, sizeDp = 42, packageName = app?.packageName)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Installing package in background...",
                                            fontSize = 11.sp,
                                            color = colorScheme.primary
                                        )
                                    }
                                }
                            }

                            is DownloadState.Installed -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(iconUrl = app?.iconUrl ?: "", appName = appName, sizeDp = 42, packageName = app?.packageName)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Installed successfully!",
                                            fontSize = 11.sp,
                                            color = NoxInstallGreen
                                        )
                                    }
                                }
                            }

                            is DownloadState.Failed -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(iconUrl = app?.iconUrl ?: "", appName = appName, sizeDp = 42, packageName = app?.packageName)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = state.error,
                                            fontSize = 11.sp,
                                            color = colorScheme.error
                                        )
                                    }
                                    if (app != null) {
                                        IconButton(onClick = { onRetryDownload(app) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Retry",
                                                tint = colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

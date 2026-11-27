package com.nox.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.ui.components.AppCardRow
import com.nox.store.ui.components.AppIcon
import com.nox.store.ui.components.SectionHeader
import com.nox.store.ui.theme.NoxAccentCyan
import com.nox.store.ui.theme.NoxCard
import com.nox.store.ui.theme.NoxCardBorder
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxPrimary
import com.nox.store.ui.theme.NoxUpdateBlue
import com.nox.store.ui.theme.TextMuted

@Composable
fun UpdatesScreen(
    apps: List<AppItem>,
    downloadStates: Map<String, DownloadState>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onUpdateApp: (AppItem) -> Unit,
    onUpdateAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingUpdates = apps.filter { it.hasUpdate }
    val upToDateApps = apps.filter { it.isInstalled && !it.hasUpdate }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Updates",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${pendingUpdates.size} updates pending",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(NoxCard)
                        .border(1.dp, NoxCardBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isRefreshing) { onRefresh() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NoxAccentCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Check for updates",
                                tint = NoxAccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Check now",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NoxAccentCyan
                            )
                        }
                    }
                }
            }
        }

        // Automatic Background Update Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NoxCard)
                    .border(1.dp, NoxCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NoxInstallGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = NoxInstallGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Updates Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Android 12+ unattended background updates enabled.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Action: Update All Button
        if (pendingUpdates.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NoxUpdateBlue)
                            .clickable { onUpdateAll() }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Update All (${pendingUpdates.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        val totalUpdateSize = pendingUpdates.sumOf { it.sizeBytes }
                        Text(
                            text = "${(totalUpdateSize / (1024 * 1024))} MB total",
                            color = Color(0xFFD4E3FC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Pending Updates Section
        if (pendingUpdates.isNotEmpty()) {
            item {
                SectionHeader(title = "Updates Available (${pendingUpdates.size})")
            }

            items(pendingUpdates) { app ->
                val state = downloadStates[app.packageName] ?: DownloadState.Idle
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    AppCardRow(
                        app = app,
                        downloadState = state,
                        onClick = { onAppClick(app) },
                        onActionClick = { onUpdateApp(app) }
                    )
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = NoxInstallGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All Apps are Up to Date",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No updates pending right now.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Up to date installed apps
        if (upToDateApps.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Recently Updated & Installed (${upToDateApps.size})")
            }

            items(upToDateApps) { app ->
                val state = downloadStates[app.packageName] ?: DownloadState.Idle
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    AppCardRow(
                        app = app,
                        downloadState = state,
                        onClick = { onAppClick(app) },
                        onActionClick = { onAppClick(app) }
                    )
                }
            }
        }
    }
}

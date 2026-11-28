package com.nox.store.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.ui.components.AppIcon
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxUpdateBlue

@Composable
fun AppDetailScreen(
    app: AppItem,
    downloadState: DownloadState,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var selectedScreenshotUrl by remember { mutableStateOf<String?>(null) }
    var supportExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out ${app.name} on Nox Store!\n${app.downloadUrl}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = colorScheme.onSurface
                    )
                }
            }
        }

        // Header: Icon, Title, Developer, Category
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    iconUrl = app.iconUrl,
                    appName = app.name,
                    sizeDp = 76,
                    packageName = app.packageName
                )

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = app.developer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.secondary
                        )
                        if (app.isDevVerified || app.isSelfApp) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified Developer",
                                tint = if (app.isSelfApp) Color(0xFFFFB300) else colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (app.isSelfApp) Color(0xFFFFB300).copy(alpha = 0.2f)
                                        else colorScheme.secondary.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (app.isSelfApp) "Store Founder" else app.devBadgeLevel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (app.isSelfApp) Color(0xFFFFB300) else colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colorScheme.surfaceVariant)
                                .border(1.dp, colorScheme.outline, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = app.category,
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        if (app.isSelfApp) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SELF-UPDATE ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real Stats Pillars: Stars, Download Size, Real Downloads
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.formattedRating,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (app.realStars > 0) Color(0xFFFFB300) else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (app.realStars > 0) "${app.realStars} GitHub Stars" else "Community App",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(colorScheme.outline)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = app.formattedSize,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Download Size",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(colorScheme.outline)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = app.formattedDownloads,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Total Downloads",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary Action / Download Progress Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surfaceVariant)
                                .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading ${downloadState.progressPercent}%",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = onCancelDownload,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cancel",
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = colorScheme.secondary,
                                trackColor = colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = downloadState.formattedProgress,
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = downloadState.formattedSpeed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.secondary
                                )
                            }
                        }
                    }

                    is DownloadState.Connecting -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Connecting to CDN...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }

                    is DownloadState.Installing -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Installing package in background...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (downloadState is DownloadState.Failed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.error.copy(alpha = 0.14f))
                                        .border(1.dp, colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = downloadState.error,
                                        color = colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            val (btnText, btnColor) = when {
                                app.isSelfApp && app.hasUpdate -> "Update Nox Store to v${app.latestVersionName}" to NoxUpdateBlue
                                app.hasUpdate -> "Update to v${app.latestVersionName}" to NoxUpdateBlue
                                app.isInstalled -> "Open Application" to colorScheme.primary
                                downloadState is DownloadState.Downloaded -> "Install Downloaded APK" to NoxInstallGreen
                                else -> "Install" to NoxInstallGreen
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .background(btnColor)
                                    .clickable { onActionClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = btnText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Play Store Style Screenshots Gallery
        if (app.screenshots.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Screenshots",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(app.screenshots) { url ->
                        Box(
                            modifier = Modifier
                                .width(175.dp)
                                .height(310.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surfaceVariant)
                                .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                                .clickable { selectedScreenshotUrl = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Screenshot",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // What's New / Changelog
        if (app.changelog.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "What's new",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "v${app.latestVersionName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    app.changelog.forEach { logItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = logItem,
                                fontSize = 13.sp,
                                color = colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // About this app
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "About this app",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = app.description.ifBlank { app.summary },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // Play Store Style App Support / Developer Contact Section
        item {
            val effectiveWebsite = app.developerWebsite.ifBlank {
                if (!app.githubRepo.isNullOrBlank()) "https://github.com/${app.githubRepo}" else ""
            }
            val hasSupportInfo = app.developerEmail.isNotBlank() ||
                effectiveWebsite.isNotBlank() ||
                app.developerLocation.isNotBlank() ||
                app.developerBio.isNotBlank()

            if (hasSupportInfo) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { supportExpanded = !supportExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "App support & Developer",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            )
                            Text(
                                text = app.developer,
                                fontSize = 11.sp,
                                color = colorScheme.secondary
                            )
                        }
                        Icon(
                            imageVector = if (supportExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = supportExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (app.developerBio.isNotBlank()) {
                                Text(
                                    text = app.developerBio,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }

                            if (effectiveWebsite.isNotBlank()) {
                                DeveloperSupportRow(
                                    icon = Icons.Rounded.Language,
                                    label = "Website",
                                    value = effectiveWebsite,
                                    onClick = {
                                        val url = if (effectiveWebsite.startsWith("http")) effectiveWebsite else "https://$effectiveWebsite"
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {}
                                    }
                                )
                            }

                            if (app.developerEmail.isNotBlank()) {
                                DeveloperSupportRow(
                                    icon = Icons.Rounded.Email,
                                    label = "Support email",
                                    value = app.developerEmail,
                                    onClick = {
                                        try {
                                            val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${app.developerEmail}"))
                                            context.startActivity(mailIntent)
                                        } catch (_: Exception) {}
                                    }
                                )
                            }

                            if (app.developerLocation.isNotBlank()) {
                                DeveloperSupportRow(
                                    icon = Icons.Rounded.LocationOn,
                                    label = "Address",
                                    value = app.developerLocation,
                                    onClick = null
                                )
                            }
                        }
                    }
                }
            }
        }

        // App Specifications & Technical Details
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "App Specifications",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                SpecRow(label = "Package Name", value = app.packageName)
                SpecRow(label = "Version Name", value = app.latestVersionName)
                SpecRow(label = "Version Code", value = "${app.latestVersionCode}")
                SpecRow(
                    label = "Installed Version",
                    value = if (app.isInstalled) "v${app.installedVersionName} (${app.installedVersionCode})" else "Not Installed"
                )
                SpecRow(label = "Minimum Android", value = "Android 8.0 (API ${app.minSdk})")
                SpecRow(label = "Target Android", value = "Android 15 (API ${app.targetSdk})")
                SpecRow(label = "Silent Updates", value = "Supported (Android 12–15)")
                if (app.githubRepo != null) {
                    SpecRow(label = "Source Repository", value = app.githubRepo)
                }
            }
        }
    }

    // Full-screen Screenshot Viewer Dialog
    if (selectedScreenshotUrl != null) {
        Dialog(
            onDismissRequest = { selectedScreenshotUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { selectedScreenshotUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedScreenshotUrl,
                    contentDescription = "Screenshot Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { selectedScreenshotUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperSupportRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface
        )
    }
}

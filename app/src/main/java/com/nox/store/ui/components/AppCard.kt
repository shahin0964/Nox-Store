package com.nox.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.ui.theme.NoxAccentCyan
import com.nox.store.ui.theme.NoxCard
import com.nox.store.ui.theme.NoxCardBorder
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxPrimary
import com.nox.store.ui.theme.NoxUpdateBlue
import com.nox.store.ui.theme.TextMuted

@Composable
fun AppIcon(
    iconUrl: String,
    appName: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 54,
    packageName: String? = null
) {
    val context = LocalContext.current
    val installedDrawable = remember(packageName) {
        if (!packageName.isNullOrBlank()) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val initial = appName.firstOrNull()?.uppercase() ?: "N"
    val gradientColors = when ((appName.hashCode() % 4 + 4) % 4) {
        0 -> listOf(Color(0xFF7C5CFC), Color(0xFF5B36F5))
        1 -> listOf(Color(0xFF00C853), Color(0xFF009688))
        2 -> listOf(Color(0xFFFF4081), Color(0xFFC2185B))
        else -> listOf(Color(0xFF00E5FF), Color(0xFF0097A7))
    }

    val modelToLoad: Any? = installedDrawable ?: iconUrl.takeIf { it.isNotBlank() && !it.endsWith(".xml") }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.28).dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, NoxCardBorder.copy(alpha = 0.5f), RoundedCornerShape((sizeDp * 0.28).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (modelToLoad != null) {
            SubcomposeAsyncImage(
                model = modelToLoad,
                contentDescription = appName,
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape((sizeDp * 0.28).dp)),
                contentScale = ContentScale.Crop,
                error = {
                    Box(
                        modifier = Modifier.size(sizeDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = (sizeDp * 0.44).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (sizeDp * 0.44).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AppCardRow(
    app: AppItem,
    downloadState: DownloadState,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NoxCard)
            .border(1.dp, NoxCardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(iconUrl = app.iconUrl, appName = app.name, sizeDp = 52, packageName = app.packageName)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (app.isSelfApp) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NoxPrimary.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STORE",
                            color = NoxAccentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.summary.ifBlank { app.developer },
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Real stats row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Real Stars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = if (app.realStars > 0) Color(0xFFFFB300) else TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = app.formattedRating,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(text = "•", fontSize = 10.sp, color = TextMuted)

                // Real Downloads
                Text(
                    text = "${app.formattedDownloads} dl",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Text(text = "•", fontSize = 10.sp, color = TextMuted)

                // Size
                Text(
                    text = app.formattedSize,
                    fontSize = 11.sp,
                    color = TextMuted
                )

                if (app.hasUpdate) {
                    Text(text = "•", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = "v${app.latestVersionName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoxAccentCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        AppActionButton(
            app = app,
            downloadState = downloadState,
            onActionClick = onActionClick
        )
    }
}

@Composable
fun AppActionButton(
    app: AppItem,
    downloadState: DownloadState,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (downloadState) {
        is DownloadState.Downloading -> {
            Box(
                modifier = modifier
                    .size(42.dp)
                    .clickable { onActionClick() },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier.size(34.dp),
                    color = NoxAccentCyan,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "${downloadState.progressPercent}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        is DownloadState.Connecting -> {
            Box(
                modifier = modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = NoxPrimary,
                    strokeWidth = 2.dp
                )
            }
        }

        is DownloadState.Installing -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NoxPrimary.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Installing...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NoxPrimary
                )
            }
        }

        else -> {
            val (buttonText, bgColor, textColor) = when {
                app.hasUpdate -> Triple("Update", NoxUpdateBlue, Color.White)
                app.isInstalled -> Triple("Open", NoxCardBorder, MaterialTheme.colorScheme.onSurface)
                else -> Triple("Install", NoxInstallGreen, Color.White)
            }

            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onActionClick() }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun AppCardGridItem(
    app: AppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(iconUrl = app.iconUrl, appName = app.name, sizeDp = 60, packageName = app.packageName)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (app.realStars > 0) Color(0xFFFFB300) else TextMuted,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = app.formattedRating,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

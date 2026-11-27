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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
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
import coil.compose.AsyncImage
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import com.nox.store.ui.theme.NoxAccentCyan
import com.nox.store.ui.theme.NoxCard
import com.nox.store.ui.theme.NoxCardBorder
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxPrimary
import com.nox.store.ui.theme.NoxUpdateBlue
import com.nox.store.ui.theme.TextMuted
import com.nox.store.ui.theme.TextWhite

@Composable
fun AppIcon(
    iconUrl: String,
    appName: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 56
) {
    val initial = appName.firstOrNull()?.uppercase() ?: "N"
    val gradientColors = when ((appName.hashCode() % 4 + 4) % 4) {
        0 -> listOf(Color(0xFF7C5CFC), Color(0xFF6366F1))
        1 -> listOf(Color(0xFF00C853), Color(0xFF00B0FF))
        2 -> listOf(Color(0xFFFF4081), Color(0xFFFF6E40))
        else -> listOf(Color(0xFF00E5FF), Color(0xFF7C5CFC))
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.25).dp))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (iconUrl.isNotBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = appName,
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape((sizeDp * 0.25).dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (sizeDp * 0.42).sp,
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
            .clip(RoundedCornerShape(16.dp))
            .background(NoxCard)
            .border(1.dp, NoxCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(iconUrl = app.iconUrl, appName = app.name, sizeDp = 54)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.summary.ifBlank { app.developer },
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${app.rating}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "•",
                    fontSize = 10.sp,
                    color = TextMuted
                )

                // Size
                Text(
                    text = app.formattedSize,
                    fontSize = 11.sp,
                    color = TextMuted
                )

                if (app.hasUpdate) {
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "v${app.latestVersionName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NoxAccentCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

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
                    modifier = Modifier.size(36.dp),
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
                    modifier = Modifier.size(24.dp),
                    color = NoxPrimary,
                    strokeWidth = 2.5.dp
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
                    fontWeight = FontWeight.SemiBold,
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
            .width(100.dp)
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(iconUrl = app.iconUrl, appName = app.name, sizeDp = 64)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
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
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "${app.rating}",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

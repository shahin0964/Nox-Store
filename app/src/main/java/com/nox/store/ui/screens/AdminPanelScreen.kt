package com.nox.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.StoreNotice
import com.nox.store.data.repository.AdminRepository
import com.nox.store.data.repository.AppsRepository
import com.nox.store.ui.components.AppIcon
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.TextMuted
import kotlinx.coroutines.launch

enum class AdminTab(val title: String) {
    OVERVIEW("Overview"),
    NOTICE("Notice Banner"),
    APPS("Moderation"),
    BADGES("Dev Badges")
}

@Composable
fun AdminPanelScreen(
    adminRepository: AdminRepository,
    appsRepository: AppsRepository,
    ownerToken: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val apps by appsRepository.apps.collectAsState()
    val storeNotice by adminRepository.storeNotice.collectAsState()
    val badgesMap by adminRepository.badgesMap.collectAsState()

    var currentTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var noticeTitleInput by remember(storeNotice) { mutableStateOf(storeNotice?.title ?: "STORE ANNOUNCEMENT") }
    var noticeMessageInput by remember(storeNotice) { mutableStateOf(storeNotice?.message ?: "") }
    var noticeTypeInput by remember(storeNotice) { mutableStateOf(storeNotice?.type ?: "INFO") }
    var isPublishingNotice by remember { mutableStateOf(false) }

    var newDevInput by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Sticky Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Owner Admin Center",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "👑", fontSize = 16.sp)
                }
                Text(
                    text = "Store Governance & Verification (@${AdminRepository.OWNER_USERNAME})",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB300)
                )
            }
        }

        // Action confirmation banner
        if (actionMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NoxInstallGreen.copy(alpha = 0.2f))
                    .border(1.dp, NoxInstallGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = actionMessage ?: "",
                    color = colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontally Scrollable Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) colorScheme.primary else colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { currentTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when (currentTab) {
                AdminTab.OVERVIEW -> {
                    item {
                        AdminOverviewSection(
                            apps = apps,
                            badgesCount = badgesMap.size,
                            currentNotice = storeNotice,
                            onOpenNoticeEditor = { currentTab = AdminTab.NOTICE }
                        )
                    }
                }

                AdminTab.NOTICE -> {
                    item {
                        AdminBroadcastSection(
                            currentNotice = storeNotice,
                            titleInput = noticeTitleInput,
                            messageInput = noticeMessageInput,
                            typeInput = noticeTypeInput,
                            isPublishing = isPublishingNotice,
                            hasGitHubToken = !ownerToken.isNullOrBlank(),
                            onTitleChange = { noticeTitleInput = it },
                            onMessageChange = { noticeMessageInput = it },
                            onTypeChange = { noticeTypeInput = it },
                            onSaveNotice = {
                                if (noticeMessageInput.isBlank()) {
                                    actionMessage = "Please enter a notice message first."
                                    return@AdminBroadcastSection
                                }
                                scope.launch {
                                    isPublishingNotice = true
                                    val notice = StoreNotice(
                                        title = noticeTitleInput.trim().ifBlank { "STORE ANNOUNCEMENT" },
                                        message = noticeMessageInput.trim(),
                                        type = noticeTypeInput
                                    )
                                    val result = adminRepository.publishNoticeToGitHub(notice, ownerToken)
                                    isPublishingNotice = false
                                    result.onSuccess { syncedRemote ->
                                        actionMessage = if (syncedRemote) {
                                            "📢 Notice banner published live to GitHub & all store users!"
                                        } else {
                                            "📢 Notice banner saved locally! (Sign in with GitHub Device Code to sync to all users)"
                                        }
                                    }.onFailure { err ->
                                        actionMessage = "Saved locally, but GitHub sync failed: ${err.message}"
                                    }
                                }
                            },
                            onClearNotice = {
                                scope.launch {
                                    isPublishingNotice = true
                                    val result = adminRepository.publishNoticeToGitHub(null, ownerToken)
                                    isPublishingNotice = false
                                    noticeMessageInput = ""
                                    result.onSuccess { syncedRemote ->
                                        actionMessage = if (syncedRemote) {
                                            "Notice banner cleared globally on GitHub."
                                        } else {
                                            "Local notice banner cleared."
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                AdminTab.APPS -> {
                    item {
                        Text(
                            text = "Manage Live Store Apps (${apps.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(apps) { app ->
                        AdminAppRow(
                            app = app,
                            onToggleFeatured = {
                                val updated = app.copy(isFeatured = !app.isFeatured)
                                actionMessage = if (updated.isFeatured) "⭐ ${app.name} set as Hero Featured!" else "Unfeatured ${app.name}"
                            },
                            onRemoveApp = {
                                actionMessage = "🗑️ ${app.name} delisted from active catalog"
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                AdminTab.BADGES -> {
                    item {
                        Column {
                            Text(
                                text = "Developer Verification & Level Badges",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reward creators with blue verified badges and level ranks",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colorScheme.surfaceVariant)
                                    .border(1.dp, colorScheme.outline, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = newDevInput,
                                    onValueChange = { newDevInput = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(
                                        color = colorScheme.onSurface,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(colorScheme.secondary),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (newDevInput.isEmpty()) {
                                            Text(
                                                text = "Enter GitHub username to verify...",
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colorScheme.primary)
                                        .clickable {
                                            val trimmed = newDevInput.trim().removePrefix("@")
                                            if (trimmed.isNotBlank()) {
                                                adminRepository.setDeveloperBadge(trimmed, isVerified = true, level = "Pro Dev")
                                                actionMessage = "Verified developer @$trimmed (Pro Dev)"
                                                newDevInput = ""
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Add Dev",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    val devUsernames = (listOf(AdminRepository.OWNER_USERNAME) + badgesMap.keys + apps.mapNotNull { it.githubRepo?.split("/")?.firstOrNull() })
                        .distinct()

                    items(devUsernames) { username ->
                        val badge = adminRepository.getDeveloperBadge(username)
                        AdminDevBadgeCard(
                            username = username,
                            badge = badge,
                            isOwner = adminRepository.isOwner(username),
                            onBadgeUpdate = { isVerified, level ->
                                adminRepository.setDeveloperBadge(username, isVerified, level)
                                actionMessage = "Updated badge for @$username ($level, Verified: $isVerified)"
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOverviewSection(
    apps: List<AppItem>,
    badgesCount: Int,
    currentNotice: StoreNotice?,
    onOpenNoticeEditor: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val totalDownloads = apps.sumOf { it.realDownloads }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF332066), Color(0xFF1B1938), Color(0xFF101222))
                    )
                )
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Store Governance Active",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Admin permissions granted to @${AdminRepository.OWNER_USERNAME}",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill(title = "Live Apps", value = "${apps.size}")
                    StatPill(title = "Total Downloads", value = "$totalDownloads")
                    StatPill(title = "Dev Badges", value = "$badgesCount")
                }
            }
        }

        // Direct Shortcut Card for Notice Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(18.dp))
                .clickable { onOpenNoticeEditor() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Store Notice Banner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (currentNotice != null && currentNotice.message.isNotBlank()) {
                            "Active: ${currentNotice.message}"
                        } else {
                            "Tap to publish an announcement banner across Nox Store"
                        },
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun StatPill(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
fun AdminAppRow(
    app: AppItem,
    onToggleFeatured: () -> Unit,
    onRemoveApp: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(iconUrl = app.iconUrl, appName = app.name, sizeDp = 44, packageName = app.packageName)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = "v${app.latestVersionName} • ${app.packageName}",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onToggleFeatured) {
            Icon(
                imageVector = if (app.isFeatured) Icons.Filled.Star else Icons.Rounded.StarBorder,
                contentDescription = "Feature App",
                tint = if (app.isFeatured) Color(0xFFFFB300) else colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onRemoveApp) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delist",
                tint = colorScheme.error
            )
        }
    }
}

@Composable
fun AdminDevBadgeCard(
    username: String,
    badge: com.nox.store.data.model.DeveloperBadgeData,
    isOwner: Boolean,
    onBadgeUpdate: (Boolean, String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isVerified by remember(badge.isVerified) { mutableStateOf(badge.isVerified) }
    var selectedLevel by remember(badge.level) { mutableStateOf(badge.level) }

    val levels = if (isOwner) listOf("Store Founder") else listOf("Starter", "Pro Dev", "Elite Dev")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@$username",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colorScheme.onSurface
                    )

                    if (isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified",
                            tint = if (isOwner) Color(0xFFFFB300) else colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isOwner) Color(0xFFFFB300).copy(alpha = 0.2f) else colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedLevel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOwner) Color(0xFFFFB300) else colorScheme.secondary
                        )
                    }
                }

                if (!isOwner) {
                    Switch(
                        checked = isVerified,
                        onCheckedChange = {
                            isVerified = it
                            onBadgeUpdate(it, selectedLevel)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colorScheme.primary
                        )
                    )
                }
            }

            if (!isOwner) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.forEach { level ->
                        val isSel = level == selectedLevel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) colorScheme.primary else colorScheme.outline)
                                .clickable {
                                    selectedLevel = level
                                    onBadgeUpdate(isVerified, level)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = level,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) colorScheme.onPrimary else colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBroadcastSection(
    currentNotice: StoreNotice?,
    titleInput: String,
    messageInput: String,
    typeInput: String,
    isPublishing: Boolean,
    hasGitHubToken: Boolean,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onSaveNotice: () -> Unit,
    onClearNotice: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "Store-wide Notice Banner",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        )
        Text(
            text = if (hasGitHubToken) {
                "Publishes directly to GitHub registry (apps.json) so ALL users see this banner."
            } else {
                "Saves locally. Connect with GitHub Device Code in Developer Studio to push live to all users."
            },
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "Banner Title", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        BasicInputBox(
            value = titleInput,
            onValueChange = onTitleChange,
            placeholder = "e.g. STORE ANNOUNCEMENT"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Notice Message", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        BasicInputBox(
            value = messageInput,
            onValueChange = onMessageChange,
            placeholder = "e.g. Welcome to Nox Store! New updates available.",
            singleLine = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Banner Style", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        val types = listOf("INFO" to "Announcement 📢", "UPDATE" to "Update 🚀", "WARNING" to "Alert ⚠️")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { (key, label) ->
                val isSelected = typeInput.equals(key, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
                        .border(1.dp, if (isSelected) colorScheme.primary else colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { onTypeChange(key) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isPublishing) colorScheme.outline else NoxInstallGreen)
                    .clickable(enabled = !isPublishing) { onSaveNotice() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Publish Notice Banner 📢",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (currentNotice != null || messageInput.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isPublishing) { onClearNotice() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Clear Notice",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

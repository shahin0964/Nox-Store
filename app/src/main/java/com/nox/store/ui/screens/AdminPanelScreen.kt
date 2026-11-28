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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Verified
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
import com.nox.store.data.repository.AdminRepository
import com.nox.store.data.repository.AppsRepository
import com.nox.store.ui.components.AppIcon
import com.nox.store.ui.theme.NoxAccentCyan
import com.nox.store.ui.theme.NoxCard
import com.nox.store.ui.theme.NoxCardBorder
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxPrimary
import com.nox.store.ui.theme.TextMuted

enum class AdminTab(val title: String) {
    OVERVIEW("Overview"),
    APPS("Moderation"),
    BADGES("Dev Badges"),
    NOTICE("Broadcast")
}

@Composable
fun AdminPanelScreen(
    adminRepository: AdminRepository,
    appsRepository: AppsRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apps by appsRepository.apps.collectAsState()
    val storeNotice by adminRepository.storeNotice.collectAsState()
    val badgesMap by adminRepository.badgesMap.collectAsState()

    var currentTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var noticeInput by remember(storeNotice) { mutableStateOf(storeNotice ?: "") }
    var newDevInput by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Owner Admin Center",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                    .padding(10.dp)
            ) {
                Text(
                    text = actionMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Navigation Tabs for Admin Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NoxPrimary else NoxCard)
                        .clickable { currentTab = tab }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextMuted
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
                            hasNotice = !storeNotice.isNullOrBlank()
                        )
                    }
                }

                AdminTab.APPS -> {
                    item {
                        Text(
                            text = "Manage Live Store Apps (${apps.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reward creators with blue verified badges and level ranks",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Add developer manually input
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NoxCard)
                                    .border(1.dp, NoxCardBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = newDevInput,
                                    onValueChange = { newDevInput = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(NoxAccentCyan),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (newDevInput.isEmpty()) {
                                            Text(
                                                text = "Enter GitHub username to verify...",
                                                color = TextMuted,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NoxPrimary)
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

                    // Extract all unique developer usernames from catalog + badges map + owner
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

                AdminTab.NOTICE -> {
                    item {
                        AdminBroadcastSection(
                            currentNotice = storeNotice,
                            noticeInput = noticeInput,
                            onNoticeChange = { noticeInput = it },
                            onSaveNotice = {
                                adminRepository.setStoreNotice(noticeInput)
                                actionMessage = "Broadcast announcement published to all store users!"
                            },
                            onClearNotice = {
                                adminRepository.setStoreNotice(null)
                                noticeInput = ""
                                actionMessage = "Broadcast announcement cleared."
                            }
                        )
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
    hasNotice: Boolean
) {
    val totalDownloads = apps.sumOf { it.realDownloads }
    val featuredCount = apps.count { it.isFeatured }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NoxCard)
            .border(1.dp, NoxCardBorder, RoundedCornerShape(16.dp))
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "v${app.latestVersionName} • ${app.packageName}",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        IconButton(onClick = onToggleFeatured) {
            Icon(
                imageVector = if (app.isFeatured) Icons.Filled.Star else Icons.Rounded.StarBorder,
                contentDescription = "Feature App",
                tint = if (app.isFeatured) Color(0xFFFFB300) else TextMuted
            )
        }

        IconButton(onClick = onRemoveApp) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delist",
                tint = MaterialTheme.colorScheme.error
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
    var isVerified by remember(badge.isVerified) { mutableStateOf(badge.isVerified) }
    var selectedLevel by remember(badge.level) { mutableStateOf(badge.level) }

    val levels = if (isOwner) listOf("Store Founder") else listOf("Starter", "Pro Dev", "Elite Dev")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NoxCard)
            .border(1.dp, NoxCardBorder, RoundedCornerShape(16.dp))
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified",
                            tint = if (isOwner) Color(0xFFFFB300) else NoxAccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isOwner) Color(0xFFFFB300).copy(alpha = 0.2f) else NoxPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedLevel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOwner) Color(0xFFFFB300) else NoxAccentCyan
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
                            checkedTrackColor = NoxPrimary
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
                                .background(if (isSel) NoxPrimary else NoxCardBorder)
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
                                color = Color.White
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
    currentNotice: String?,
    noticeInput: String,
    onNoticeChange: (String) -> Unit,
    onSaveNotice: () -> Unit,
    onClearNotice: () -> Unit
) {
    Column {
        Text(
            text = "Store-wide Broadcast Announcement",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = "Displays a high-visibility announcement banner on all users' Home screens",
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        BasicInputBox(
            value = noticeInput,
            onValueChange = onNoticeChange,
            placeholder = "e.g. Welcome to Nox Store! New AI tools published."
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NoxInstallGreen)
                    .clickable { onSaveNotice() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Publish Notice 📢",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (!currentNotice.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NoxCardBorder)
                        .clickable { onClearNotice() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Clear Notice",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

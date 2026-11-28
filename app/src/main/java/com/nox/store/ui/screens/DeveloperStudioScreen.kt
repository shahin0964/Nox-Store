package com.nox.store.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DeveloperProfile
import com.nox.store.data.model.GitHubRepoItem
import com.nox.store.data.model.RepoAppPreview
import com.nox.store.data.repository.AppsRepository
import com.nox.store.data.repository.DeveloperRepository
import com.nox.store.ui.theme.NoxAccentCyan
import com.nox.store.ui.theme.NoxCard
import com.nox.store.ui.theme.NoxCardBorder
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxPrimary
import com.nox.store.ui.theme.NoxUpdateBlue
import com.nox.store.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperStudioScreen(
    developerRepository: DeveloperRepository,
    appsRepository: AppsRepository,
    onBackClick: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val developerProfile by developerRepository.developerProfile.collectAsState()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var usernameInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectError by remember { mutableStateOf<String?>(null) }

    var deviceCodeResponse by remember { mutableStateOf<com.nox.store.data.model.DeviceCodeResponse?>(null) }
    var isStartingDeviceFlow by remember { mutableStateOf(false) }
    var isPollingDeviceFlow by remember { mutableStateOf(false) }
    var showClientIdDialog by remember { mutableStateOf(false) }
    var clientIdInput by remember { mutableStateOf(developerRepository.getClientId()) }
    var deviceFlowError by remember { mutableStateOf<String?>(null) }
    var codeCopiedToast by remember { mutableStateOf(false) }

    fun triggerDeviceSignIn() {
        val clientId = developerRepository.getClientId().trim()
        if (clientId.isBlank()) {
            showClientIdDialog = true
            return
        }

        scope.launch {
            isStartingDeviceFlow = true
            deviceFlowError = null
            connectError = null
            val res = developerRepository.requestDeviceCode(clientId)
            isStartingDeviceFlow = false

            res.onSuccess { codeResp ->
                deviceCodeResponse = codeResp
                codeCopiedToast = false
                isPollingDeviceFlow = true
                scope.launch {
                    val pollRes = developerRepository.pollForToken(
                        clientId = clientId,
                        deviceCode = codeResp.deviceCode,
                        intervalSeconds = codeResp.interval
                    )
                    isPollingDeviceFlow = false
                    deviceCodeResponse = null
                    pollRes.onFailure { err ->
                        deviceFlowError = err.message ?: "Authorization failed"
                    }
                }
            }.onFailure { err ->
                val msg = err.message ?: "Failed requesting login code"
                deviceFlowError = msg
                if (msg.contains("client_id", ignoreCase = true) || msg.contains("not found", ignoreCase = true)) {
                    showClientIdDialog = true
                }
            }
        }
    }

    var reposList by remember { mutableStateOf<List<GitHubRepoItem>>(emptyList()) }
    var isLoadingRepos by remember { mutableStateOf(false) }

    var selectedRepoPreview by remember { mutableStateOf<RepoAppPreview?>(null) }
    var isInspectingRepo by remember { mutableStateOf(false) }
    var showPublishSheet by remember { mutableStateOf(false) }
    var publishSuccessMsg by remember { mutableStateOf<String?>(null) }
    var showDeveloperProfileSheet by remember { mutableStateOf(false) }

    // Fetch repositories when developer is connected
    LaunchedEffect(developerProfile) {
        if (developerProfile != null) {
            isLoadingRepos = true
            reposList = developerRepository.fetchUserRepositories()
            isLoadingRepos = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Top App Bar
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
                Text(
                    text = "Developer Studio",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Open App Marketplace & Registry",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (publishSuccessMsg != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NoxInstallGreen.copy(alpha = 0.2f))
                            .border(1.dp, NoxInstallGreen, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = NoxInstallGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = publishSuccessMsg ?: "",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            val dev = developerProfile
            if (dev == null) {
                // Not Connected (Guest) Screen
                item {
                    DeveloperOnboardingCard(
                        usernameInput = usernameInput,
                        onUsernameChange = { usernameInput = it },
                        isConnecting = isConnecting,
                        errorMessage = connectError,
                        onConnectClick = {
                            scope.launch {
                                isConnecting = true
                                connectError = null
                                val result = developerRepository.connectGitHub(usernameInput)
                                result.onFailure {
                                    connectError = it.localizedMessage ?: "Failed to connect"
                                }
                                isConnecting = false
                            }
                        },
                        isStartingDeviceFlow = isStartingDeviceFlow,
                        deviceFlowError = deviceFlowError,
                        onDeviceSignInClick = { triggerDeviceSignIn() },
                        onConfigureClientIdClick = { showClientIdDialog = true }
                    )
                }
            } else {
                // Connected Developer Profile Header
                item {
                    DeveloperProfileCard(
                        profile = dev,
                        onEditProfile = { showDeveloperProfileSheet = true },
                        onSignOut = { developerRepository.signOut() }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (dev.isOwner) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B277A), Color(0xFF1E1742), Color(0xFF131028))
                                    )
                                )
                                .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(18.dp))
                                .clickable { onOpenAdminPanel() }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "👑", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Owner Admin Command Center",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Manage all apps, verify developers & broadcast notices",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFFB300)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFFB300))
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "Open",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // Section: Repositories List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your GitHub Repositories (${reposList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoadingRepos = true
                                    reposList = developerRepository.fetchUserRepositories()
                                    isLoadingRepos = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = NoxAccentCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoadingRepos) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NoxPrimary)
                        }
                    }
                } else if (reposList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No repositories found for @${dev.username}",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(reposList) { repo ->
                        GitHubRepoCard(
                            repo = repo,
                            isInspecting = isInspectingRepo && selectedRepoPreview?.repoFullName == repo.fullName,
                            onInspectClick = {
                                scope.launch {
                                    isInspectingRepo = true
                                    selectedRepoPreview = developerRepository.inspectRepoForApp(repo.fullName)
                                    isInspectingRepo = false
                                    showPublishSheet = true
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    // Modal Sheet for Publishing the App
    if (showPublishSheet && selectedRepoPreview != null) {
        val colorScheme = MaterialTheme.colorScheme
        val preview = selectedRepoPreview!!
        val publisherName = developerProfile?.studioName?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: developerProfile?.displayName?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: developerProfile?.username ?: "Nox Developer"
        var appTitle by remember(preview.appName) { mutableStateOf(preview.appName) }
        var appPkg by remember(preview.packageName) { mutableStateOf(preview.packageName) }
        var appSummary by remember(preview.summary) { mutableStateOf(preview.summary) }
        var selectedCategory by remember(preview.category) { mutableStateOf(preview.category) }
        var isPublishing by remember { mutableStateOf(false) }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showPublishSheet = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Publish App to Nox Store",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // APK Inspection Status Card
                if (preview.hasValidApk) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NoxInstallGreen.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = NoxInstallGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ready to Publish: v${preview.latestTag}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "APK Size: ${(preview.apkSize / (1024 * 1024))} MB",
                                    fontSize = 11.sp,
                                    color = colorScheme.secondary
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.error.copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = preview.errorMessage ?: "No release APK found",
                                fontSize = 12.sp,
                                color = colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Publisher Profile Identity Summary (linked to Developer Profile)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.background)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { showDeveloperProfileSheet = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Publisher: $publisherName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        val infoParts = listOfNotNull(
                            developerProfile?.email?.takeIf { it.isNotBlank() },
                            developerProfile?.website?.takeIf { it.isNotBlank() },
                            developerProfile?.location?.takeIf { it.isNotBlank() }
                        )
                        Text(
                            text = if (infoParts.isNotEmpty()) infoParts.joinToString(" • ") else "Tap to complete Developer Info (email, website, bio)",
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profile",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.secondary
                    )
                }

                // Screenshots detected from README.md
                if (preview.screenshots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Screenshots from README.md (${preview.screenshots.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(preview.screenshots) { ssUrl ->
                            AsyncImage(
                                model = ssUrl,
                                contentDescription = "Screenshot",
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App Title Field
                Text(text = "App Display Name", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(value = appTitle, onValueChange = { appTitle = it })

                Spacer(modifier = Modifier.height(10.dp))

                // Package Name Field
                Text(text = "Package Name", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(value = appPkg, onValueChange = { appPkg = it })

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Field
                Text(text = "One-line Summary", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(value = appSummary, onValueChange = { appSummary = it })

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                Text(text = "Category", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                val categories = listOf("Tools", "Communication", "Productivity", "Personalization", "Development", "Games")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) colorScheme.primary else colorScheme.outline)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (preview.hasValidApk && !isPublishing) NoxInstallGreen else colorScheme.outline)
                        .clickable(enabled = preview.hasValidApk && !isPublishing) {
                            scope.launch {
                                isPublishing = true
                                val dev = developerProfile
                                val newApp = AppItem(
                                    id = appPkg.replace(".", "_"),
                                    packageName = appPkg,
                                    name = appTitle,
                                    developer = publisherName,
                                    developerEmail = dev?.email ?: "",
                                    developerWebsite = dev?.website ?: "",
                                    developerLocation = dev?.location ?: "",
                                    developerBio = dev?.bio ?: "",
                                    category = selectedCategory,
                                    summary = appSummary,
                                    description = appSummary,
                                    iconUrl = "https://raw.githubusercontent.com/${preview.repoFullName}/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                                    screenshots = preview.screenshots,
                                    latestVersionCode = 1,
                                    latestVersionName = preview.latestTag,
                                    downloadUrl = preview.apkUrl,
                                    sizeBytes = preview.apkSize,
                                    githubRepo = preview.repoFullName,
                                    changelog = listOf("Published via Nox Developer Studio"),
                                    isFeatured = false
                                )

                                appsRepository.publishApp(newApp, dev?.token)
                                isPublishing = false
                                showPublishSheet = false
                                publishSuccessMsg = "🎉 ${newApp.name} successfully published to Nox Store!"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Publish to Nox Store Registry 🚀",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // GitHub Device Code Authorization Modal Bottom Sheet
    if (deviceCodeResponse != null) {
        val codeResp = deviceCodeResponse!!
        ModalBottomSheet(
            onDismissRequest = {
                deviceCodeResponse = null
                isPollingDeviceFlow = false
            },
            containerColor = NoxCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔑 GitHub Device Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter this one-time code on GitHub to authorize Nox Store:",
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(18.dp))

                // The 8-character user code box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF16182B))
                        .border(1.5.dp, NoxAccentCyan, RoundedCornerShape(16.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(codeResp.userCode))
                            codeCopiedToast = true
                        }
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = codeResp.userCode,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (codeCopiedToast) "✓ Code copied to clipboard!" else "Tap to copy code",
                    fontSize = 11.sp,
                    color = if (codeCopiedToast) NoxInstallGreen else TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Copy & Open GitHub button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NoxPrimary, NoxAccentCyan)
                            )
                        )
                        .clickable {
                            clipboardManager.setText(AnnotatedString(codeResp.userCode))
                            codeCopiedToast = true
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(codeResp.verificationUri))
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInBrowser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy & Open GitHub (github.com/login/device)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Polling spinner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NoxAccentCyan
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Waiting for you to authorize in browser...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // GitHub OAuth App Client ID Dialog
    if (showClientIdDialog) {
        AlertDialog(
            onDismissRequest = { showClientIdDialog = false },
            title = {
                Text(
                    text = "GitHub OAuth Client ID",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Device Code লগইনের জন্য আপনার GitHub OAuth App-এর Client ID দিন:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BasicInputBox(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it },
                        placeholder = "e.g. Ov23liXXXXXXXXXX"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create at github.com/settings/developers (check 'Enable Device Flow')",
                        fontSize = 10.sp,
                        color = NoxAccentCyan,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/developers"))
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = clientIdInput.trim()
                        if (trimmed.isNotBlank()) {
                            developerRepository.setClientId(trimmed)
                            showClientIdDialog = false
                            triggerDeviceSignIn()
                        }
                    }
                ) {
                    Text("Save & Continue", color = NoxAccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClientIdDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = NoxCard
        )
    }

    // Complete Play Store-style Developer Profile & Publisher Info Sheet
    if (showDeveloperProfileSheet && developerProfile != null) {
        val colorScheme = MaterialTheme.colorScheme
        val currentProfile = developerProfile!!
        var tempStudioName by remember(currentProfile.studioName) {
            mutableStateOf(currentProfile.studioName.ifBlank { currentProfile.displayName })
        }
        var tempBio by remember(currentProfile.bio) { mutableStateOf(currentProfile.bio) }
        var tempEmail by remember(currentProfile.email) { mutableStateOf(currentProfile.email) }
        var tempWebsite by remember(currentProfile.website) { mutableStateOf(currentProfile.website) }
        var tempLocation by remember(currentProfile.location) { mutableStateOf(currentProfile.location) }

        val devSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDeveloperProfileSheet = false },
            sheetState = devSheetState,
            containerColor = colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Developer Profile & Info",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Just like Google Play Console — this information is shown on every app you publish to Nox Store.",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Developer / Company / Studio Name", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(
                    value = tempStudioName,
                    onValueChange = { tempStudioName = it },
                    placeholder = "e.g. Nox Studios / Metro Telecom"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "About Developer / Bio", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(
                    value = tempBio,
                    onValueChange = { tempBio = it },
                    placeholder = "Short intro about your studio or apps...",
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Support Email", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    placeholder = "e.g. support@noxstudios.dev"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Website / Portfolio URL", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(
                    value = tempWebsite,
                    onValueChange = { tempWebsite = it },
                    placeholder = "e.g. https://github.com/shahin0964"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Location / Address", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                BasicInputBox(
                    value = tempLocation,
                    onValueChange = { tempLocation = it },
                    placeholder = "e.g. Dhaka, Bangladesh"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colorScheme.primary)
                        .clickable {
                            developerRepository.saveDeveloperInfo(
                                studioName = tempStudioName,
                                bio = tempBio,
                                email = tempEmail,
                                website = tempWebsite,
                                location = tempLocation
                            )
                            showDeveloperProfileSheet = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save Developer Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DeveloperOnboardingCard(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    isConnecting: Boolean,
    errorMessage: String?,
    onConnectClick: () -> Unit,
    isStartingDeviceFlow: Boolean = false,
    deviceFlowError: String? = null,
    onDeviceSignInClick: () -> Unit = {},
    onConfigureClientIdClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF231C4D), Color(0xFF131525), Color(0xFF0F111E))
                )
            )
            .border(1.dp, NoxPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NoxPrimary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = NoxAccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Join as a Developer",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Publish apps directly from your GitHub repos",
                        color = NoxAccentCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "• 100% Free & Open Ecosystem (Option A Registry)\n• Auto-syncs releases, APKs, downloads & stars\n• Background silent updates for all your users",
                color = Color(0xFFD0D3E5),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action: Sign in with GitHub (Device Code)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NoxPrimary, NoxAccentCyan)
                        )
                    )
                    .clickable(enabled = !isStartingDeviceFlow) { onDeviceSignInClick() }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isStartingDeviceFlow) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sign in with GitHub (Device Code)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "No token needed • Authenticate via code in browser",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (deviceFlowError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = deviceFlowError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider: or connect with username
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(NoxCardBorder)
                )
                Text(
                    text = "  or connect with username  ",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(NoxCardBorder)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            BasicInputBox(
                value = usernameInput,
                onValueChange = onUsernameChange,
                placeholder = "e.g. shahin0964 or ghp_token"
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NoxCard)
                    .border(1.dp, NoxCardBorder, RoundedCornerShape(22.dp))
                    .clickable(enabled = !isConnecting) { onConnectClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Connect Profile Only",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Configure Client ID link at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onConfigureClientIdClick() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Config",
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Configure OAuth Client ID",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DeveloperProfileCard(
    profile: DeveloperProfile,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (profile.avatarUrl.isNotBlank() && !profile.avatarUrl.equals("null", ignoreCase = true)) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.username,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.username.firstOrNull()?.uppercase() ?: "D",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val cleanDisplay = profile.studioName.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: profile.displayName.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: profile.username

                Text(
                    text = cleanDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${profile.username}",
                        fontSize = 12.sp,
                        color = colorScheme.secondary
                    )
                    if (profile.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified",
                            tint = if (profile.isOwner) Color(0xFFFFB300) else colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (profile.isOwner) Color(0xFFFFB300).copy(alpha = 0.2f)
                                else colorScheme.primary.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = profile.badgeLevel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profile.isOwner) Color(0xFFFFB300) else colorScheme.secondary
                        )
                    }
                }

                if (profile.bio.isNotBlank() && !profile.bio.equals("null", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.bio,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.outline)
                    .clickable { onSignOut() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Sign out",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full Developer Profile Info Bar (Play Store Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.background)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { onEditProfile() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Developer Profile & Support Info",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                val summaryItems = listOfNotNull(
                    profile.email.takeIf { it.isNotBlank() },
                    profile.website.takeIf { it.isNotBlank() },
                    profile.location.takeIf { it.isNotBlank() }
                )
                Text(
                    text = if (summaryItems.isNotEmpty()) {
                        summaryItems.joinToString(" • ")
                    } else {
                        "Add Studio Name, Bio, Email, Website & Address"
                    },
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Info",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.secondary
            )
        }
    }
}

@Composable
fun GitHubRepoCard(
    repo: GitHubRepoItem,
    isInspecting: Boolean,
    onInspectClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = repo.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = colorScheme.onSurface
            )

            if (repo.description.isNotBlank() && !repo.description.equals("null", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = repo.description,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${repo.stars}",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Branch: ${repo.defaultBranch}",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(NoxUpdateBlue)
                .clickable(enabled = !isInspecting) { onInspectClick() }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isInspecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Inspect",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BasicInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.background)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(text = placeholder, color = colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = colorScheme.onSurface,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

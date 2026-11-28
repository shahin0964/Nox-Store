package com.nox.store.ui.screens

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nox.store.data.model.AppThemeMode
import com.nox.store.data.model.StoreSettings
import com.nox.store.data.repository.InstallerManager
import com.nox.store.ui.theme.NoxInstallGreen
import com.nox.store.ui.theme.NoxWarning

@Composable
fun SettingsScreen(
    settings: StoreSettings,
    onSettingsChanged: (StoreSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorScheme = MaterialTheme.colorScheme

    var canInstall by remember { mutableStateOf(InstallerManager.canInstallPackages(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canInstall = InstallerManager.canInstallPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                text = "Store Settings",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Theme, dynamic colors & background updates",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Section: Appearance & Theme
            item {
                SectionTitle(title = "Appearance & Dynamic Theme")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Theme Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Choose System, Pure White, Dark, or AMOLED Black",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2x2 Theme Options Grid
                    val modes = AppThemeMode.entries
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (rowIndex in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (colIndex in 0 until 2) {
                                    val mode = modes[rowIndex * 2 + colIndex]
                                    val isSelected = settings.themeMode == mode
                                    val swatchColor = when (mode) {
                                        AppThemeMode.SYSTEM -> colorScheme.primary
                                        AppThemeMode.LIGHT -> Color(0xFFFFFFFF)
                                        AppThemeMode.DARK -> Color(0xFF141622)
                                        AppThemeMode.AMOLED -> Color(0xFF000000)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) colorScheme.primary.copy(alpha = 0.16f)
                                                else colorScheme.background
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) colorScheme.primary else colorScheme.outline,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                onSettingsChanged(settings.copy(themeMode = mode))
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(1.dp, colorScheme.outline, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = mode.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingSwitchRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Material You Dynamic Colors",
                            subtitle = "Adapt accent palette from your phone wallpaper",
                            checked = settings.dynamicColors,
                            onCheckedChange = { onSettingsChanged(settings.copy(dynamicColors = it)) }
                        )
                    }
                }
            }

            // Section: Background Updates
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionTitle(title = "Background Auto-Updates")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingSwitchRow(
                        icon = Icons.Rounded.Sync,
                        title = "Automatic Background Update",
                        subtitle = "Check & download updates automatically",
                        checked = settings.autoUpdateEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(autoUpdateEnabled = it)) }
                    )

                    SettingSwitchRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Play Store Silent Install",
                        subtitle = "Install owned updates silently without dialogs (Android 12+)",
                        checked = settings.unattendedMode,
                        onCheckedChange = { onSettingsChanged(settings.copy(unattendedMode = it)) }
                    )

                    SettingSwitchRow(
                        icon = Icons.Rounded.Wifi,
                        title = "Only Update Over Wi-Fi",
                        subtitle = "Prevent downloading APK updates using mobile data",
                        checked = settings.wifiOnly,
                        onCheckedChange = { onSettingsChanged(settings.copy(wifiOnly = it)) }
                    )

                    SettingSwitchRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Show Update Notifications",
                        subtitle = "Notify when background updates are finished",
                        checked = settings.notifyOnUpdate,
                        onCheckedChange = { onSettingsChanged(settings.copy(notifyOnUpdate = it)) }
                    )
                }
            }

            // Section: Install Permission Status
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionTitle(title = "Install Permissions")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable { InstallerManager.openInstallPermissionSettings(context) }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (canInstall) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (canInstall) NoxInstallGreen else NoxWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (canInstall) "Install Unknown Apps: Allowed" else "Install Unknown Apps: Action Required",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (canInstall) {
                                    "Nox Store is authorized to install & update apps. Tap to manage."
                                } else {
                                    "Tap here to enable 'Allow from this source' so APKs can install!"
                                },
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tip: Once an app is installed or updated once via Nox Store on Android 14/15, future updates apply silently in the background. Disable 'Scan apps with Play Protect' in Play Store settings if Play Protect blocks custom APKs.",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Section: About
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionTitle(title = "About Nox Store")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nox Store",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Package: com.nox.store\nTarget SDK: Android 15 (API 35)\nCrafted for autonomous package distribution and silent background updates.",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colorScheme.primary,
                uncheckedThumbColor = colorScheme.onSurfaceVariant,
                uncheckedTrackColor = colorScheme.outline
            )
        )
    }
}

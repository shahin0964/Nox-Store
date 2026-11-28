package com.nox.store.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nox.store.data.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = NoxPrimary,
    onPrimary = TextWhite,
    primaryContainer = NoxPrimaryVariant,
    onPrimaryContainer = TextWhite,
    secondary = NoxAccentCyan,
    onSecondary = NoxBackground,
    background = NoxBackground,
    onBackground = TextWhite,
    surface = NoxSurface,
    onSurface = TextWhite,
    surfaceVariant = NoxCard,
    onSurfaceVariant = TextMuted,
    outline = NoxCardBorder,
    error = NoxError
)

private val AmoledColorScheme = darkColorScheme(
    primary = NoxPrimary,
    onPrimary = TextWhite,
    primaryContainer = NoxPrimaryVariant,
    onPrimaryContainer = TextWhite,
    secondary = NoxAccentCyan,
    onSecondary = AmoledBackground,
    background = AmoledBackground,
    onBackground = TextWhite,
    surface = AmoledSurface,
    onSurface = TextWhite,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = TextMuted,
    outline = AmoledCardBorder,
    error = NoxError
)

private val LightColorScheme = lightColorScheme(
    primary = NoxPrimary,
    onPrimary = TextWhite,
    primaryContainer = Color(0xFFEDE8FF),
    onPrimaryContainer = Color(0xFF28117A),
    secondary = Color(0xFF00838F),
    onSecondary = TextWhite,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = Color(0xFFD32F2F)
)

@Composable
fun NoxStoreTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) {
                val dynamicDark = dynamicDarkColorScheme(context)
                if (themeMode == AppThemeMode.AMOLED) {
                    dynamicDark.copy(
                        background = AmoledBackground,
                        surface = AmoledSurface,
                        surfaceVariant = AmoledCard,
                        surfaceContainer = AmoledCard,
                        outline = AmoledCardBorder,
                        outlineVariant = AmoledCardBorder
                    )
                } else {
                    dynamicDark
                }
            } else {
                dynamicLightColorScheme(context).copy(
                    background = LightBackground,
                    surface = LightSurface
                )
            }
        }
        themeMode == AppThemeMode.AMOLED -> AmoledColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

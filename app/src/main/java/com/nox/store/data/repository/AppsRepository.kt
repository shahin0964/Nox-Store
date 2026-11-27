package com.nox.store.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.nox.store.data.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class AppsRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadInitialCatalog()
        refreshInstalledStatus()
    }

    private fun loadInitialCatalog() {
        _apps.value = defaultAppsList.map { app ->
            detectInstalledVersion(app)
        }
    }

    fun refreshInstalledStatus() {
        val currentList = _apps.value.ifEmpty { defaultAppsList }
        _apps.value = currentList.map { app ->
            detectInstalledVersion(app)
        }
    }

    fun fetchRemoteCatalog(repoUrl: String) {
        scope.launch {
            _isRefreshing.value = true
            try {
                val request = Request.Builder().url(repoUrl).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val parsed = parseAppsJson(bodyString)
                    if (parsed.isNotEmpty()) {
                        _apps.value = parsed.map { detectInstalledVersion(it) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch remote catalog, keeping cached: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun detectInstalledVersion(app: AppItem): AppItem {
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(app.packageName, 0)
            }

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

            app.copy(
                installedVersionCode = vCode,
                installedVersionName = packageInfo.versionName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            app.copy(
                installedVersionCode = null,
                installedVersionName = null
            )
        }
    }

    private fun parseAppsJson(jsonString: String): List<AppItem> {
        val list = mutableListOf<AppItem>()
        try {
            val root = JSONObject(jsonString)
            val appsArray = root.optJSONArray("apps") ?: JSONArray()
            for (i in 0 until appsArray.length()) {
                val obj = appsArray.getJSONObject(i)
                val changelogList = mutableListOf<String>()
                val clArray = obj.optJSONArray("changelog")
                if (clArray != null) {
                    for (j in 0 until clArray.length()) {
                        changelogList.add(clArray.getString(j))
                    }
                }

                val screenshotsList = mutableListOf<String>()
                val scArray = obj.optJSONArray("screenshots")
                if (scArray != null) {
                    for (k in 0 until scArray.length()) {
                        screenshotsList.add(scArray.getString(k))
                    }
                }

                list.add(
                    AppItem(
                        id = obj.optString("id", obj.optString("packageName")),
                        packageName = obj.getString("packageName"),
                        name = obj.getString("name"),
                        developer = obj.optString("developer", "Nox Studios"),
                        category = obj.optString("category", "Tools"),
                        summary = obj.optString("summary", ""),
                        description = obj.optString("description", ""),
                        iconUrl = obj.optString("iconUrl", ""),
                        bannerUrl = obj.optString("bannerUrl", ""),
                        screenshots = screenshotsList,
                        latestVersionCode = obj.getInt("latestVersionCode"),
                        latestVersionName = obj.optString("latestVersionName", "1.0.0"),
                        downloadUrl = obj.getString("downloadUrl"),
                        sizeBytes = obj.optLong("sizeBytes", 15_000_000L),
                        rating = obj.optDouble("rating", 4.8).toFloat(),
                        reviewsCount = obj.optString("reviewsCount", "1.5K"),
                        downloadsCount = obj.optString("downloadsCount", "10K+"),
                        changelog = changelogList,
                        minSdk = obj.optInt("minSdk", 26),
                        targetSdk = obj.optInt("targetSdk", 35),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        releaseDate = obj.optString("releaseDate", "Nov 2026")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error", e)
        }
        return list
    }

    companion object {
        private const val TAG = "AppsRepository"

        val defaultAppsList = listOf(
            AppItem(
                id = "smart_messages",
                packageName = "com.smartmessages.app",
                name = "Smart Messages",
                developer = "Nox Studios",
                category = "Communication",
                summary = "AI-powered SMS & instant messaging with private vault and spam protection",
                description = "Smart Messages is the next generation messaging experience for Android. Featuring on-device spam detection, scheduled texting, dual SIM management, high-speed backup & restore, and gorgeous AMOLED dark mode crafted with Jetpack Compose.",
                iconUrl = "https://raw.githubusercontent.com/shahin0964/Smart-Messages/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1577563908411-5077b6dc7624?w=600&q=80",
                    "https://images.unsplash.com/photo-1551650975-87deedd944c3?w=600&q=80"
                ),
                latestVersionCode = 12,
                latestVersionName = "1.0.12",
                downloadUrl = "https://github.com/shahin0964/Smart-Messages/releases/download/v1.0.12/SmartMessages-latest.apk",
                sizeBytes = 14_800_000L,
                rating = 4.9f,
                reviewsCount = "2.8K",
                downloadsCount = "50K+",
                changelog = listOf(
                    "Fixed input text contrast across dark theme",
                    "Added auto-categorization for OTPs & transactions",
                    "Improved dual-SIM switching response time"
                ),
                isFeatured = true
            ),
            AppItem(
                id = "smart_dialer",
                packageName = "com.smartdialer.app",
                name = "Smart Dialer",
                developer = "Nox Studios",
                category = "Communication",
                summary = "Ultra-fast T9 dialer with call recorder, caller insights and dark UI",
                description = "A blazingly fast, lightweight, and privacy-respecting dialer with smart T9 contact search, call logs analysis, call recording without prompt, and intuitive gestures.",
                iconUrl = "https://raw.githubusercontent.com/shahin0964/Smart-Dialer/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                bannerUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&q=80"
                ),
                latestVersionCode = 68,
                latestVersionName = "1.0.68",
                downloadUrl = "https://github.com/shahin0964/Smart-Dialer/releases/download/v1.0.68/SmartDialer-latest.apk",
                sizeBytes = 12_400_000L,
                rating = 4.8f,
                reviewsCount = "4.1K",
                downloadsCount = "100K+",
                changelog = listOf(
                    "Instant search algorithm optimization",
                    "Added quick WhatsApp shortcut beside contact numbers",
                    "Improved Bluetooth headset audio routing"
                ),
                isFeatured = true
            ),
            AppItem(
                id = "max_ai",
                packageName = "com.max.ai",
                name = "Max AI Assistant",
                developer = "Nox Studios",
                category = "Productivity",
                summary = "Interactive AI voice & vision assistant with glowing neural orb",
                description = "Max AI delivers next-generation conversational intelligence with a 3D glowing neural orb, real-time audio synthesis, visual object scanning, and memory persistence.",
                iconUrl = "https://raw.githubusercontent.com/shahin0964/Max-Ai/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                bannerUrl = "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&q=80"
                ),
                latestVersionCode = 139,
                latestVersionName = "2.4.0",
                downloadUrl = "https://github.com/shahin0964/Max-Ai/releases/download/v2.4.0/MaxAI-latest.apk",
                sizeBytes = 28_500_000L,
                rating = 4.9f,
                reviewsCount = "8.9K",
                downloadsCount = "250K+",
                changelog = listOf(
                    "Folded 3D ribbon M branding with glossy gradient",
                    "Aoede natural voice synthesis integration",
                    "Holographic memories portal"
                ),
                isFeatured = true
            ),
            AppItem(
                id = "nox_keyboard",
                packageName = "com.nox.keyboard",
                name = "Nox Keyboard",
                developer = "Nox Studios",
                category = "Personalization",
                summary = "Fluid bilingual typing keyboard with gesture glide and customizable themes",
                description = "Ergonomic keyboard engine tailored for lightning-fast Bangla and English typing. Built-in clipboard history, text expansions, voice typing, and deep OLED neon themes.",
                iconUrl = "https://raw.githubusercontent.com/shahin0964/Nox-Keyboard/main/assets/icon.png",
                bannerUrl = "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80",
                screenshots = emptyList(),
                latestVersionCode = 24,
                latestVersionName = "1.4.0",
                downloadUrl = "https://github.com/shahin0964/Nox-Keyboard/releases/download/v1.4.0/NoxKeyboard-latest.apk",
                sizeBytes = 18_200_000L,
                rating = 4.7f,
                reviewsCount = "1.9K",
                downloadsCount = "30K+",
                changelog = listOf(
                    "Smart Bangla phonetic dictionary update",
                    "Added emoji prediction bar",
                    "Reduced memory footprint by 22%"
                ),
                isFeatured = false
            ),
            AppItem(
                id = "compose_craft",
                packageName = "com.composecraft.app",
                name = "ComposeCraft Studio",
                developer = "Nox Dev",
                category = "Development",
                summary = "Live visual Jetpack Compose layout builder and code generator",
                description = "Build Android UI components visually and export clean, production-ready Kotlin Jetpack Compose code instantly right from your mobile device.",
                iconUrl = "https://raw.githubusercontent.com/shahin0964/ComposeCraft/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png",
                bannerUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80",
                screenshots = emptyList(),
                latestVersionCode = 8,
                latestVersionName = "1.2.0",
                downloadUrl = "https://github.com/shahin0964/ComposeCraft/releases/download/v1.2.0/ComposeCraft-latest.apk",
                sizeBytes = 22_000_000L,
                rating = 4.8f,
                reviewsCount = "720",
                downloadsCount = "8K+",
                changelog = listOf(
                    "Support for Compose BOM 2024.10",
                    "Added interactive animation timeline",
                    "Direct code copy to clipboard"
                ),
                isFeatured = false
            )
        )
    }
}

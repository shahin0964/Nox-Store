package com.nox.store.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.nox.store.data.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AppsRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val localPublishedFile by lazy {
        File(context.filesDir, "published_apps.json")
    }

    init {
        loadCatalog()
        refreshInstalledStatus()
        fetchRemoteCentralRegistry()
    }

    private fun loadCatalog() {
        val list = mutableListOf<AppItem>()
        // 1. Always include official Nox Store as base self-updating app
        list.add(detectInstalledVersion(baseNoxStoreApp))

        // 2. Load locally published apps by developers on this device
        list.addAll(loadLocalPublishedApps().map { detectInstalledVersion(it) })

        _apps.value = list
    }

    fun refreshInstalledStatus() {
        val currentList = _apps.value.ifEmpty { listOf(baseNoxStoreApp) }
        _apps.value = currentList.map { detectInstalledVersion(it) }
    }

    /**
     * Option A (উপায় ক): GitHub-Powered Central Registry
     * Fetches published community apps from the central Nox-Store repository apps.json
     */
    fun fetchRemoteCentralRegistry(registryUrl: String = DEFAULT_REGISTRY_URL) {
        scope.launch {
            _isRefreshing.value = true
            try {
                val request = Request.Builder()
                    .url(registryUrl)
                    .header("User-Agent", "NoxStore/1.0.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val parsed = parseAppsJson(bodyString)
                        if (parsed.isNotEmpty()) {
                            mergeAndSyncCatalog(parsed)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Central registry fetch note: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun mergeAndSyncCatalog(remoteApps: List<AppItem>) = withContext(Dispatchers.IO) {
        val mergedMap = mutableMapOf<String, AppItem>()

        // Always put Nox Store base app
        mergedMap[baseNoxStoreApp.packageName] = detectInstalledVersion(baseNoxStoreApp)

        // Add remote published apps
        for (remote in remoteApps) {
            mergedMap[remote.packageName] = detectInstalledVersion(remote)
        }

        // Add locally published apps (in case developer published locally)
        for (local in loadLocalPublishedApps()) {
            mergedMap[local.packageName] = detectInstalledVersion(local)
        }

        _apps.value = mergedMap.values.toList()

        // Sync live GitHub stars, downloads, and release data
        syncAllFromGitHub()
    }

    /**
     * Publish a new app to the store registry (Option A: GitHub-Powered Registry)
     */
    suspend fun publishApp(newApp: AppItem, devToken: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Save locally in published_apps.json
            val localList = loadLocalPublishedApps().toMutableList()
            localList.removeAll { it.packageName == newApp.packageName }
            localList.add(newApp)
            saveLocalPublishedApps(localList)

            // 2. Immediately update in-memory catalog
            val currentList = _apps.value.toMutableList()
            currentList.removeAll { it.packageName == newApp.packageName }
            currentList.add(detectInstalledVersion(newApp))
            _apps.value = currentList

            // 3. If token is provided, commit directly to GitHub-powered central registry (shahin0964/Nox-Store/apps.json)
            if (!devToken.isNullOrBlank()) {
                commitToGitHubRegistry(newApp, devToken)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing app", e)
            false
        }
    }

    private fun commitToGitHubRegistry(app: AppItem, token: String) {
        try {
            val getFileUrl = "https://api.github.com/repos/shahin0964/Nox-Store/contents/apps.json"
            val getReq = Request.Builder()
                .url(getFileUrl)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "NoxStore/1.0.0")
                .build()

            client.newCall(getReq).execute().use { response ->
                if (!response.isSuccessful) return
                val body = response.body?.string() ?: ""
                val fileJson = JSONObject(body)
                val sha = fileJson.getString("sha")
                val contentBase64 = fileJson.getString("content").replace("\n", "")
                val decodedJson = String(Base64.decode(contentBase64, Base64.DEFAULT))

                val registryObj = JSONObject(decodedJson)
                val appsArray = registryObj.optJSONArray("apps") ?: JSONArray()

                // Check if app already in registry array
                var exists = false
                val newAppJson = JSONObject().apply {
                    put("id", app.id)
                    put("packageName", app.packageName)
                    put("name", app.name)
                    put("developer", app.developer)
                    put("category", app.category)
                    put("summary", app.summary)
                    put("description", app.description)
                    put("iconUrl", app.iconUrl)
                    put("bannerUrl", app.bannerUrl)
                    put("screenshots", JSONArray(app.screenshots))
                    put("latestVersionCode", app.latestVersionCode)
                    put("latestVersionName", app.latestVersionName)
                    put("downloadUrl", app.downloadUrl)
                    put("sizeBytes", app.sizeBytes)
                    put("githubRepo", app.githubRepo)
                    put("changelog", JSONArray(app.changelog))
                    put("minSdk", app.minSdk)
                    put("targetSdk", app.targetSdk)
                    put("isFeatured", false)
                    put("isSelfApp", false)
                }

                for (i in 0 until appsArray.length()) {
                    val existing = appsArray.getJSONObject(i)
                    if (existing.optString("packageName") == app.packageName) {
                        appsArray.put(i, newAppJson)
                        exists = true
                        break
                    }
                }
                if (!exists) {
                    appsArray.put(newAppJson)
                }

                registryObj.put("apps", appsArray)
                registryObj.put("lastUpdated", System.currentTimeMillis().toString())

                val updatedContent = registryObj.toString(2)
                val encodedUpdated = Base64.encodeToString(updatedContent.toByteArray(), Base64.NO_WRAP)

                val putBody = JSONObject().apply {
                    put("message", "feat(store): publish ${app.name} (${app.packageName})")
                    put("content", encodedUpdated)
                    put("sha", sha)
                }

                val putReq = Request.Builder()
                    .url(getFileUrl)
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", "NoxStore/1.0.0")
                    .put(putBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(putReq).execute().close()
                Log.i(TAG, "Successfully committed ${app.name} to central GitHub registry!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GitHub Registry commit skipped or failed", e)
        }
    }

    private fun loadLocalPublishedApps(): List<AppItem> {
        if (!localPublishedFile.exists()) return emptyList()
        return try {
            val content = localPublishedFile.readText()
            parseAppsJson(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLocalPublishedApps(apps: List<AppItem>) {
        try {
            val root = JSONObject()
            val array = JSONArray()
            apps.forEach { app ->
                val obj = JSONObject().apply {
                    put("id", app.id)
                    put("packageName", app.packageName)
                    put("name", app.name)
                    put("developer", app.developer)
                    put("category", app.category)
                    put("summary", app.summary)
                    put("description", app.description)
                    put("iconUrl", app.iconUrl)
                    put("bannerUrl", app.bannerUrl)
                    put("screenshots", JSONArray(app.screenshots))
                    put("latestVersionCode", app.latestVersionCode)
                    put("latestVersionName", app.latestVersionName)
                    put("downloadUrl", app.downloadUrl)
                    put("sizeBytes", app.sizeBytes)
                    put("githubRepo", app.githubRepo)
                    put("changelog", JSONArray(app.changelog))
                    put("minSdk", app.minSdk)
                    put("targetSdk", app.targetSdk)
                    put("isFeatured", app.isFeatured)
                    put("isSelfApp", app.isSelfApp)
                }
                array.put(obj)
            }
            root.put("apps", array)
            localPublishedFile.writeText(root.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local published apps", e)
        }
    }

    /**
     * Direct GitHub Sync Engine
     * Fetches real APK releases, download counts, stars, descriptions, and changelogs from GitHub API
     */
    fun syncAllFromGitHub() {
        scope.launch {
            _isRefreshing.value = true
            try {
                val currentList = _apps.value.toMutableList()

                for (i in currentList.indices) {
                    val app = currentList[i]
                    val repo = app.githubRepo ?: continue
                    try {
                        val updatedApp = fetchRepoDetailsFromGitHub(repo, app)
                        currentList[i] = detectInstalledVersion(updatedApp)
                        _apps.value = currentList.toList()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error syncing $repo: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncing from GitHub", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchRepoDetailsFromGitHub(repo: String, baseApp: AppItem): AppItem = withContext(Dispatchers.IO) {
        var realStars = baseApp.realStars
        var description = baseApp.description
        var summary = baseApp.summary

        // 1. Fetch Repository Info (Stars, Repo Description)
        try {
            val repoReq = Request.Builder()
                .url("https://api.github.com/repos/$repo")
                .header("User-Agent", "NoxStore/1.0.0")
                .build()

            client.newCall(repoReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    realStars = json.optInt("stargazers_count", 0)
                    val desc = json.optString("description", "")
                    if (desc.isNotBlank()) {
                        description = desc
                        summary = desc
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fetch Latest Release Info
        var latestVersionCode = baseApp.latestVersionCode
        var latestVersionName = baseApp.latestVersionName
        var downloadUrl = baseApp.downloadUrl
        var sizeBytes = baseApp.sizeBytes
        var realDownloads = 0L
        val changelog = mutableListOf<String>()

        try {
            val releaseReq = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("User-Agent", "NoxStore/1.0.0")
                .build()

            client.newCall(releaseReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    val tagName = json.optString("tag_name", "").removePrefix("v")
                    if (tagName.isNotBlank()) {
                        latestVersionName = tagName
                        val parts = tagName.split(".")
                        val parsedCode = parts.lastOrNull()?.toIntOrNull()
                        if (parsedCode != null) {
                            latestVersionCode = parsedCode
                        }
                    }

                    val releaseBody = json.optString("body", "")
                    if (releaseBody.isNotBlank()) {
                        changelog.clear()
                        releaseBody.lines().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("*") || trimmed.startsWith("-")) {
                                changelog.add(trimmed.removePrefix("*").removePrefix("-").trim())
                            }
                        }
                    }

                    val assets = json.optJSONArray("assets") ?: JSONArray()
                    var apkFound = false
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val assetName = asset.optString("name", "")
                        val assetDownloads = asset.optLong("download_count", 0L)
                        realDownloads += assetDownloads

                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            if (!apkFound || assetName.contains("latest", ignoreCase = true)) {
                                downloadUrl = asset.getString("browser_download_url")
                                sizeBytes = asset.optLong("size", sizeBytes)
                                apkFound = true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return@withContext baseApp.copy(
            latestVersionCode = latestVersionCode,
            latestVersionName = latestVersionName,
            downloadUrl = downloadUrl,
            sizeBytes = sizeBytes,
            realStars = realStars,
            realDownloads = realDownloads,
            description = description,
            summary = summary,
            changelog = if (changelog.isNotEmpty()) changelog else baseApp.changelog
        )
    }

    private fun detectInstalledVersion(app: AppItem): AppItem {
        val pm = context.packageManager
        // 1. Try exact package name
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(app.packageName, 0)
            }

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

            return app.copy(
                installedVersionCode = vCode,
                installedVersionName = packageInfo.versionName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Not found by exact packageName, try fallback below
        } catch (e: Exception) {
            Log.e(TAG, "Error checking package info for ${app.packageName}", e)
        }

        // 2. Fallback: Search all installed packages by app label or package sub-string
        try {
            val installedPackages = pm.getInstalledPackages(0)
            val cleanAppName = app.name.replace(" ", "").replace("-", "").lowercase()

            for (pkg in installedPackages) {
                val pkgName = pkg.packageName.lowercase()
                val label = try {
                    pm.getApplicationLabel(pkg.applicationInfo).toString().trim().lowercase()
                } catch (e: Exception) { "" }

                val isMatch = (label.isNotBlank() && label == app.name.trim().lowercase()) ||
                        (label.isNotBlank() && label == cleanAppName) ||
                        (cleanAppName.length >= 4 && pkgName.contains(cleanAppName)) ||
                        (app.githubRepo.isNotBlank() && pkgName.contains(app.githubRepo.substringAfter("/").replace("-", "").replace("_", "").lowercase()))

                if (isMatch) {
                    val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkg.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode
                    }
                    Log.d(TAG, "Matched installed app '${app.name}' with actual package '${pkg.packageName}'")
                    return app.copy(
                        packageName = pkg.packageName,
                        installedVersionCode = vCode,
                        installedVersionName = pkg.versionName
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback package detection error", e)
        }

        return app.copy(
            installedVersionCode = null,
            installedVersionName = null
        )
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

                val devName = obj.optString("developer", "Nox Community Developer")
                val isSelf = obj.optBoolean("isSelfApp", false)
                val adminRepo = AdminRepository(context)
                val badge = adminRepo.getDeveloperBadge(devName)

                list.add(
                    AppItem(
                        id = obj.optString("id", obj.optString("packageName")),
                        packageName = obj.getString("packageName"),
                        name = obj.getString("name"),
                        developer = devName,
                        category = obj.optString("category", "Tools"),
                        summary = obj.optString("summary", ""),
                        description = obj.optString("description", ""),
                        iconUrl = obj.optString("iconUrl", ""),
                        bannerUrl = obj.optString("bannerUrl", ""),
                        screenshots = screenshotsList,
                        latestVersionCode = obj.optInt("latestVersionCode", 1),
                        latestVersionName = obj.optString("latestVersionName", "1.0.0"),
                        downloadUrl = obj.getString("downloadUrl"),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        realStars = obj.optInt("realStars", 0),
                        realDownloads = obj.optLong("realDownloads", 0L),
                        githubRepo = obj.optString("githubRepo", null),
                        changelog = changelogList,
                        minSdk = obj.optInt("minSdk", 26),
                        targetSdk = obj.optInt("targetSdk", 35),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        isSelfApp = isSelf,
                        isDevVerified = obj.optBoolean("isDevVerified", isSelf || badge.isVerified),
                        devBadgeLevel = obj.optString("devBadgeLevel", if (isSelf) "Store Founder" else badge.level),
                        releaseDate = obj.optString("releaseDate", "Recent")
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
        const val DEFAULT_REGISTRY_URL = "https://raw.githubusercontent.com/shahin0964/Nox-Store/main/apps.json"

        // The single official base self-updating app in Nox Store
        val baseNoxStoreApp = AppItem(
            id = "nox_store",
            packageName = "com.nox.store",
            name = "Nox Store",
            developer = "Nox Studios",
            category = "Tools",
            summary = "Autonomous Android App Store with background updates",
            description = "Nox Store is an open application marketplace featuring automated background updates and direct GitHub releases publishing.",
            iconUrl = "https://raw.githubusercontent.com/shahin0964/Nox-Store/main/app/src/main/res/drawable/ic_launcher_foreground.xml",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
            screenshots = emptyList(),
            latestVersionCode = 4,
            latestVersionName = "1.0.4",
            downloadUrl = "https://github.com/shahin0964/Nox-Store/releases/download/v1.0.4/NoxStore-latest.apk",
            sizeBytes = 12_127_122L,
            realStars = 0,
            realDownloads = 0L,
            githubRepo = "shahin0964/Nox-Store",
            changelog = listOf(
                "Autonomous self-updating store architecture",
                "Option A: GitHub-powered central registry",
                "Developer Studio with 1-click GitHub publishing"
            ),
            isFeatured = true,
            isSelfApp = true,
            isDevVerified = true,
            devBadgeLevel = "Store Founder"
        )
    }
}

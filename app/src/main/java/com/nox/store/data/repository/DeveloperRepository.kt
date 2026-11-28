package com.nox.store.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nox.store.data.model.DeveloperProfile
import com.nox.store.data.model.DeviceCodeResponse
import com.nox.store.data.model.GitHubRepoItem
import com.nox.store.data.model.RepoAppPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeveloperRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nox_store_developer", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _developerProfile = MutableStateFlow<DeveloperProfile?>(loadSavedProfile())
    val developerProfile: StateFlow<DeveloperProfile?> = _developerProfile.asStateFlow()

    private fun cleanOptString(obj: JSONObject, key: String, fallback: String = ""): String {
        if (!obj.has(key) || obj.isNull(key)) return fallback
        val v = obj.optString(key, fallback).trim()
        return if (v.equals("null", ignoreCase = true) || v.isBlank()) fallback else v
    }

    private fun cleanString(value: String?, fallback: String = ""): String {
        if (value == null) return fallback
        val trimmed = value.trim()
        return if (trimmed.equals("null", ignoreCase = true) || trimmed.isBlank()) fallback else trimmed
    }

    private fun loadSavedProfile(): DeveloperProfile? {
        val rawUsername = prefs.getString(KEY_USERNAME, null) ?: return null
        val username = cleanString(rawUsername)
        if (username.isBlank()) return null

        val adminRepo = AdminRepository(context)
        val isOwner = adminRepo.isOwner(username)
        val badge = adminRepo.getDeveloperBadge(username)
        val displayName = cleanString(prefs.getString(KEY_DISPLAY_NAME, username), username)
        val studioName = cleanString(prefs.getString(KEY_STUDIO_NAME, ""))

        return DeveloperProfile(
            username = username,
            displayName = if (displayName.isNotBlank()) displayName else username,
            studioName = studioName,
            avatarUrl = cleanString(prefs.getString(KEY_AVATAR_URL, "")),
            bio = cleanString(prefs.getString(KEY_BIO, "")),
            publicRepos = prefs.getInt(KEY_REPOS_COUNT, 0),
            token = prefs.getString(KEY_TOKEN, null),
            isVerified = badge.isVerified,
            badgeLevel = badge.level,
            isOwner = isOwner
        )
    }

    fun getStudioName(): String {
        return cleanString(prefs.getString(KEY_STUDIO_NAME, ""))
    }

    fun setStudioName(name: String) {
        val clean = name.trim()
        prefs.edit().putString(KEY_STUDIO_NAME, clean).apply()
        refreshProfile()
    }

    fun refreshProfile() {
        _developerProfile.value = loadSavedProfile()
    }

    suspend fun connectGitHub(identifier: String): Result<DeveloperProfile> = withContext(Dispatchers.IO) {
        val trimmed = identifier.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("GitHub username or token cannot be empty"))
        }

        val isToken = trimmed.startsWith("ghp_") || trimmed.startsWith("github_pat_") || trimmed.length >= 35

        val url = if (isToken) "https://api.github.com/user" else "https://api.github.com/users/$trimmed"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "NoxStore/1.0.0")

        if (isToken) {
            requestBuilder.header("Authorization", "Bearer $trimmed")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub user not found (HTTP ${response.code})"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)

            val username = json.getString("login")
            val rawName = cleanOptString(json, "name", "")
            val displayName = if (rawName.isNotBlank()) rawName else username
            val avatarUrl = cleanOptString(json, "avatar_url", "")
            val bio = cleanOptString(json, "bio", "")
            val publicRepos = json.optInt("public_repos", 0)

            val adminRepo = AdminRepository(context)
            val isOwner = adminRepo.isOwner(username)
            val badge = adminRepo.getDeveloperBadge(username)
            val studioName = getStudioName()

            val profile = DeveloperProfile(
                username = username,
                displayName = displayName,
                studioName = studioName,
                avatarUrl = avatarUrl,
                bio = bio,
                publicRepos = publicRepos,
                token = if (isToken) trimmed else null,
                isVerified = badge.isVerified,
                badgeLevel = badge.level,
                isOwner = isOwner
            )

            // Save in prefs
            prefs.edit()
                .putString(KEY_USERNAME, profile.username)
                .putString(KEY_DISPLAY_NAME, profile.displayName)
                .putString(KEY_AVATAR_URL, profile.avatarUrl)
                .putString(KEY_BIO, profile.bio)
                .putInt(KEY_REPOS_COUNT, profile.publicRepos)
                .putString(KEY_TOKEN, profile.token)
                .apply()

            _developerProfile.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed connecting to GitHub", e)
            Result.failure(e)
        }
    }

    suspend fun fetchUserRepositories(): List<GitHubRepoItem> = withContext(Dispatchers.IO) {
        val profile = _developerProfile.value ?: return@withContext emptyList()
        val isToken = !profile.token.isNullOrBlank()

        val url = if (isToken) {
            "https://api.github.com/user/repos?per_page=60&sort=updated"
        } else {
            "https://api.github.com/users/${profile.username}/repos?per_page=60&sort=updated"
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "NoxStore/1.0.0")

        if (isToken) {
            requestBuilder.header("Authorization", "Bearer ${profile.token}")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: ""
            val array = JSONArray(body)
            val list = mutableListOf<GitHubRepoItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GitHubRepoItem(
                        name = obj.getString("name"),
                        fullName = obj.getString("full_name"),
                        description = cleanOptString(obj, "description", ""),
                        stars = obj.optInt("stargazers_count", 0),
                        defaultBranch = cleanOptString(obj, "default_branch", "main"),
                        updatedAt = cleanOptString(obj, "updated_at", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch repositories", e)
            emptyList()
        }
    }

    private fun findLocalInstalledPackage(repoName: String, cleanAppName: String): String? {
        return try {
            val pm = context.packageManager
            val installed = pm.getInstalledPackages(0)
            val simplifiedRepo = repoName.replace("-", "").replace("_", "").lowercase()
            val simplifiedApp = cleanAppName.replace(" ", "").lowercase()

            for (pkg in installed) {
                val pkgName = pkg.packageName.lowercase()
                val label = try {
                    pm.getApplicationLabel(pkg.applicationInfo).toString().trim().lowercase()
                } catch (e: Exception) { "" }

                if (label == cleanAppName.lowercase() || (label.isNotBlank() && label == simplifiedApp) ||
                    pkgName.contains(simplifiedRepo) || pkgName.contains(simplifiedApp)) {
                    return pkg.packageName
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchPackageNameFromRepo(repoFullName: String, defaultBranch: String): String? = withContext(Dispatchers.IO) {
        val paths = listOf(
            "app/build.gradle.kts",
            "app/build.gradle",
            "build.gradle.kts",
            "build.gradle",
            "app/src/main/AndroidManifest.xml"
        )
        val branches = listOf(defaultBranch, "main", "master").distinct()

        for (branch in branches) {
            for (path in paths) {
                try {
                    val url = "https://raw.githubusercontent.com/$repoFullName/$branch/$path"
                    val req = Request.Builder().url(url).header("User-Agent", "NoxStore/1.0.0").build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) {
                        val content = res.body?.string() ?: ""
                        if (path.endsWith("AndroidManifest.xml")) {
                            val pkgRegex = Regex("""package\s*=\s*["']([^"']+)["']""")
                            val match = pkgRegex.find(content)
                            if (match != null && match.groupValues[1].isNotBlank()) {
                                return@withContext match.groupValues[1]
                            }
                        } else {
                            val appIdRegex = Regex("""applicationId\s*=?\s*["']([^"']+)["']""")
                            val appIdMatch = appIdRegex.find(content)
                            if (appIdMatch != null && appIdMatch.groupValues[1].isNotBlank()) {
                                return@withContext appIdMatch.groupValues[1]
                            }

                            val nsRegex = Regex("""namespace\s*=?\s*["']([^"']+)["']""")
                            val nsMatch = nsRegex.find(content)
                            if (nsMatch != null && nsMatch.groupValues[1].isNotBlank()) {
                                return@withContext nsMatch.groupValues[1]
                            }
                        }
                    }
                } catch (e: Exception) {
                    // try next
                }
            }
        }
        null
    }

    suspend fun inspectRepoForApp(repoFullName: String): RepoAppPreview = withContext(Dispatchers.IO) {
        val repoParts = repoFullName.split("/")
        val repoName = repoParts.lastOrNull() ?: repoFullName
        val cleanName = repoName.replace("-", " ").replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val detectedPkg = findLocalInstalledPackage(repoName, cleanName)
            ?: fetchPackageNameFromRepo(repoFullName, "main")
            ?: "com.${repoParts.firstOrNull()?.lowercase() ?: "app"}.${repoName.lowercase().replace("-", "").replace("_", "")}"

        val profile = _developerProfile.value
        val requestBuilder = Request.Builder()
            .url("https://api.github.com/repos/$repoFullName/releases/latest")
            .header("User-Agent", "NoxStore/1.0.0")

        if (!profile?.token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${profile?.token}")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext RepoAppPreview(
                    repoFullName = repoFullName,
                    appName = cleanName,
                    packageName = detectedPkg,
                    summary = "",
                    category = "Tools",
                    latestTag = "1.0.0",
                    apkUrl = "",
                    apkSize = 0L,
                    hasValidApk = false,
                    errorMessage = "No GitHub Release found! Please create a Release with a .apk file on GitHub."
                )
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "1.0.0").removePrefix("v")
            val releaseBody = json.optString("body", "")

            val assets = json.optJSONArray("assets") ?: JSONArray()
            var apkUrl = ""
            var apkSize = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkUrl.isBlank()) {
                return@withContext RepoAppPreview(
                    repoFullName = repoFullName,
                    appName = cleanName,
                    packageName = detectedPkg,
                    summary = releaseBody.lines().firstOrNull()?.removePrefix("#")?.trim() ?: "",
                    category = "Tools",
                    latestTag = tagName,
                    apkUrl = "",
                    apkSize = 0L,
                    hasValidApk = false,
                    errorMessage = "Latest release has no .apk asset. Make sure to attach your signed APK to the release."
                )
            }

            RepoAppPreview(
                repoFullName = repoFullName,
                appName = cleanName,
                packageName = detectedPkg,
                summary = releaseBody.lines().firstOrNull()?.removePrefix("#")?.trim() ?: "Published via Nox Developer Studio",
                category = "Tools",
                latestTag = tagName,
                apkUrl = apkUrl,
                apkSize = apkSize,
                hasValidApk = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            RepoAppPreview(
                repoFullName = repoFullName,
                appName = cleanName,
                packageName = detectedPkg,
                summary = "",
                category = "Tools",
                latestTag = "1.0.0",
                apkUrl = "",
                apkSize = 0L,
                hasValidApk = false,
                errorMessage = e.message ?: "Failed inspecting repository"
            )
        }
    }
    }

    fun getClientId(): String {
        return prefs.getString(KEY_CLIENT_ID, DEFAULT_CLIENT_ID) ?: DEFAULT_CLIENT_ID
    }

    fun setClientId(clientId: String) {
        prefs.edit().putString(KEY_CLIENT_ID, clientId.trim()).apply()
    }

    suspend fun requestDeviceCode(clientId: String = getClientId()): Result<DeviceCodeResponse> = withContext(Dispatchers.IO) {
        val cleanClientId = clientId.trim()
        if (cleanClientId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("GitHub Client ID is required"))
        }

        val formBody = FormBody.Builder()
            .add("client_id", cleanClientId)
            .add("scope", "repo,read:user")
            .build()

        val request = Request.Builder()
            .url("https://github.com/login/device/code")
            .header("Accept", "application/json")
            .header("User-Agent", "NoxStore/1.0.0")
            .post(formBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)

            if (!response.isSuccessful || json.has("error")) {
                val errorMsg = json.optString("error_description", json.optString("error", "Failed getting device code"))
                return@withContext Result.failure(Exception(errorMsg))
            }

            val deviceCode = json.getString("device_code")
            val userCode = json.getString("user_code")
            val verificationUri = json.optString("verification_uri", "https://github.com/login/device")
            val expiresIn = json.optInt("expires_in", 900)
            val interval = json.optInt("interval", 5)

            Result.success(
                DeviceCodeResponse(
                    deviceCode = deviceCode,
                    userCode = userCode,
                    verificationUri = verificationUri,
                    expiresIn = expiresIn,
                    interval = interval
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Device code request failed", e)
            Result.failure(e)
        }
    }

    suspend fun pollForToken(
        clientId: String = getClientId(),
        deviceCode: String,
        intervalSeconds: Int = 5,
        maxWaitSeconds: Int = 300
    ): Result<DeveloperProfile> = withContext(Dispatchers.IO) {
        val cleanClientId = clientId.trim()
        var interval = intervalSeconds.coerceAtLeast(5)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxWaitSeconds * 1000L) {
            delay(interval * 1000L)

            val formBody = FormBody.Builder()
                .add("client_id", cleanClientId)
                .add("device_code", deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()

            val request = Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .header("User-Agent", "NoxStore/1.0.0")
                .post(formBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)

                if (json.has("access_token")) {
                    val token = json.getString("access_token")
                    return@withContext connectGitHub(token)
                }

                val error = json.optString("error", "")
                when (error) {
                    "authorization_pending" -> {
                        // User hasn't finished authorizing yet, continue polling
                    }
                    "slow_down" -> {
                        interval = json.optInt("interval", interval + 5)
                    }
                    "expired_token" -> {
                        return@withContext Result.failure(Exception("Verification code expired. Please try again."))
                    }
                    "access_denied" -> {
                        return@withContext Result.failure(Exception("Authorization cancelled by user."))
                    }
                    else -> {
                        if (error.isNotBlank()) {
                            val desc = json.optString("error_description", error)
                            return@withContext Result.failure(Exception(desc))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error polling access token", e)
            }
        }

        Result.failure(Exception("Authorization timed out. Please try again."))
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _developerProfile.value = null
    }

    companion object {
        private const val TAG = "DeveloperRepository"
        const val DEFAULT_CLIENT_ID = "Ov23liT6ApYtO1ay4Mrk"
        private const val KEY_CLIENT_ID = "dev_oauth_client_id"
        private const val KEY_USERNAME = "dev_username"
        private const val KEY_DISPLAY_NAME = "dev_display_name"
        private const val KEY_STUDIO_NAME = "dev_studio_name"
        private const val KEY_AVATAR_URL = "dev_avatar_url"
        private const val KEY_BIO = "dev_bio"
        private const val KEY_REPOS_COUNT = "dev_repos_count"
        private const val KEY_TOKEN = "dev_token"
    }
}

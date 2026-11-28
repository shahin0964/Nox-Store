package com.nox.store.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nox.store.data.model.DeveloperProfile
import com.nox.store.data.model.GitHubRepoItem
import com.nox.store.data.model.RepoAppPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

    private fun loadSavedProfile(): DeveloperProfile? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val adminRepo = AdminRepository(context)
        val isOwner = adminRepo.isOwner(username)
        val badge = adminRepo.getDeveloperBadge(username)
        return DeveloperProfile(
            username = username,
            displayName = prefs.getString(KEY_DISPLAY_NAME, username) ?: username,
            avatarUrl = prefs.getString(KEY_AVATAR_URL, "") ?: "",
            bio = prefs.getString(KEY_BIO, "") ?: "",
            publicRepos = prefs.getInt(KEY_REPOS_COUNT, 0),
            token = prefs.getString(KEY_TOKEN, null),
            isVerified = badge.isVerified,
            badgeLevel = badge.level,
            isOwner = isOwner
        )
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
            val displayName = json.optString("name", username).ifBlank { username }
            val avatarUrl = json.optString("avatar_url", "")
            val bio = json.optString("bio", "")
            val publicRepos = json.optInt("public_repos", 0)

            val adminRepo = AdminRepository(context)
            val isOwner = adminRepo.isOwner(username)
            val badge = adminRepo.getDeveloperBadge(username)

            val profile = DeveloperProfile(
                username = username,
                displayName = displayName,
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
                        description = obj.optString("description", ""),
                        stars = obj.optInt("stargazers_count", 0),
                        defaultBranch = obj.optString("default_branch", "main"),
                        updatedAt = obj.optString("updated_at", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch repositories", e)
            emptyList()
        }
    }

    suspend fun inspectRepoForApp(repoFullName: String): RepoAppPreview = withContext(Dispatchers.IO) {
        val repoParts = repoFullName.split("/")
        val repoName = repoParts.lastOrNull() ?: repoFullName
        val cleanName = repoName.replace("-", " ").replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

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
                    packageName = "com.${repoParts.firstOrNull()?.lowercase() ?: "app"}.${repoName.lowercase().replace("-", "")}",
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
                    packageName = "com.${repoParts.firstOrNull()?.lowercase() ?: "app"}.${repoName.lowercase().replace("-", "")}",
                    summary = releaseBody.lines().firstOrNull() ?: "",
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
                packageName = "com.${repoParts.firstOrNull()?.lowercase() ?: "app"}.${repoName.lowercase().replace("-", "")}",
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
                packageName = "com.${repoParts.firstOrNull()?.lowercase() ?: "app"}.${repoName.lowercase().replace("-", "")}",
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

    fun signOut() {
        prefs.edit().clear().apply()
        _developerProfile.value = null
    }

    companion object {
        private const val TAG = "DeveloperRepository"
        private const val KEY_USERNAME = "dev_username"
        private const val KEY_DISPLAY_NAME = "dev_display_name"
        private const val KEY_AVATAR_URL = "dev_avatar_url"
        private const val KEY_BIO = "dev_bio"
        private const val KEY_REPOS_COUNT = "dev_repos_count"
        private const val KEY_TOKEN = "dev_token"
    }
}

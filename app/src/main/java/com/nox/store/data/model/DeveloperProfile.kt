package com.nox.store.data.model

data class DeveloperProfile(
    val username: String,
    val displayName: String = "",
    val studioName: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val publicRepos: Int = 0,
    val token: String? = null,
    val isVerified: Boolean = false,
    val badgeLevel: String = "Starter",
    val isOwner: Boolean = false
)

data class GitHubRepoItem(
    val name: String,
    val fullName: String,
    val description: String = "",
    val stars: Int = 0,
    val defaultBranch: String = "main",
    val updatedAt: String = ""
)

data class RepoAppPreview(
    val repoFullName: String,
    val appName: String,
    val packageName: String,
    val summary: String,
    val category: String = "Tools",
    val latestTag: String,
    val apkUrl: String,
    val apkSize: Long,
    val hasValidApk: Boolean,
    val errorMessage: String? = null
)

data class DeveloperBadgeData(
    val username: String,
    val isVerified: Boolean = false,
    val level: String = "Starter" // "Starter", "Pro Dev", "Elite Dev", "Store Founder"
)

data class DeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String = "https://github.com/login/device",
    val expiresIn: Int = 900,
    val interval: Int = 5
)

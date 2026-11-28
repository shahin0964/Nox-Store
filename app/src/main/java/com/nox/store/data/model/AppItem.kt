package com.nox.store.data.model

import java.text.DecimalFormat

data class AppItem(
    val id: String,
    val packageName: String,
    val name: String,
    val developer: String = "Nox Studios",
    val category: String = "Tools",
    val summary: String,
    val description: String,
    val iconUrl: String,
    val bannerUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val latestVersionCode: Int,
    val latestVersionName: String,
    val installedVersionCode: Int? = null,
    val installedVersionName: String? = null,
    val downloadUrl: String,
    val sizeBytes: Long = 0L,
    val realStars: Int = 0,
    val realDownloads: Long = 0L,
    val githubRepo: String? = null,
    val changelog: List<String> = emptyList(),
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val isFeatured: Boolean = false,
    val isSelfApp: Boolean = false,
    val isDevVerified: Boolean = false,
    val devBadgeLevel: String = "Starter",
    val releaseDate: String = "Recent"
) {
    val isInstalled: Boolean
        get() = installedVersionCode != null

    val hasUpdate: Boolean
        get() = installedVersionCode != null && latestVersionCode > (installedVersionCode ?: 0)

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "-- MB"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb < 1.0) {
                "${sizeBytes / 1024} KB"
            } else {
                "${DecimalFormat("#.#").format(mb)} MB"
            }
        }

    val formattedDownloads: String
        get() {
            return when {
                realDownloads <= 0 -> "New"
                realDownloads < 1000 -> "$realDownloads"
                realDownloads < 1_000_000 -> "${DecimalFormat("#.#").format(realDownloads / 1000.0)}K"
                else -> "${DecimalFormat("#.#").format(realDownloads / 1_000_000.0)}M"
            }
        }

    val formattedRating: String
        get() {
            return if (realStars > 0) "$realStars" else "New"
        }
}

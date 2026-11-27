package com.nox.store.data.model

import java.io.File
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
    val rating: Float = 4.8f,
    val reviewsCount: String = "1.2K",
    val downloadsCount: String = "10K+",
    val changelog: List<String> = emptyList(),
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val isFeatured: Boolean = false,
    val releaseDate: String = "Nov 2026"
) {
    val isInstalled: Boolean
        get() = installedVersionCode != null

    val hasUpdate: Boolean
        get() = installedVersionCode != null && latestVersionCode > (installedVersionCode ?: 0)

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "15 MB"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb < 1.0) {
                "${sizeBytes / 1024} KB"
            } else {
                "${DecimalFormat("#.#").format(mb)} MB"
            }
        }
}

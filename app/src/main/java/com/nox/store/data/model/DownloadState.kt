package com.nox.store.data.model

import java.io.File
import java.text.DecimalFormat

sealed class DownloadState {
    object Idle : DownloadState()
    object Connecting : DownloadState()
    
    data class Downloading(
        val progress: Float,
        val speedBytesPerSec: Long,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState() {
        val progressPercent: Int
            get() = (progress * 100).toInt().coerceIn(0, 100)

        val formattedSpeed: String
            get() {
                val mbps = speedBytesPerSec / (1024.0 * 1024.0)
                return if (mbps >= 1.0) {
                    "${DecimalFormat("#.#").format(mbps)} MB/s"
                } else {
                    val kbps = speedBytesPerSec / 1024.0
                    "${DecimalFormat("#").format(kbps)} KB/s"
                }
            }

        val formattedProgress: String
            get() {
                val downMb = downloadedBytes / (1024.0 * 1024.0)
                val totMb = totalBytes / (1024.0 * 1024.0)
                return "${DecimalFormat("#.#").format(downMb)} MB / ${DecimalFormat("#.#").format(totMb)} MB"
            }
    }

    data class Downloaded(val apkFile: File) : DownloadState()
    object Installing : DownloadState()
    object Installed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

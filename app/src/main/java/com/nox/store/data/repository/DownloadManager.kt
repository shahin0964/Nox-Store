package com.nox.store.data.repository

import android.content.Context
import android.util.Log
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = mutableMapOf<String, Job>()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val apkDir: File by lazy {
        File(context.cacheDir, "apks").apply {
            if (!exists()) mkdirs()
        }
    }

    fun startDownload(app: AppItem, autoInstall: Boolean = true, unattended: Boolean = true) {
        if (activeJobs[app.packageName]?.isActive == true) {
            Log.d(TAG, "Download already in progress for ${app.packageName}")
            return
        }

        val job = scope.launch {
            updateState(app.packageName, DownloadState.Connecting)
            val destinationFile = File(apkDir, "${app.packageName}_v${app.latestVersionCode}.apk")

            try {
                val request = Request.Builder()
                    .url(app.downloadUrl)
                    .header("User-Agent", "NoxStore/1.0.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val error = "HTTP ${response.code}: ${response.message}"
                    updateState(app.packageName, DownloadState.Failed(error))
                    return@launch
                }

                val body = response.body ?: run {
                    updateState(app.packageName, DownloadState.Failed("Empty response body"))
                    return@launch
                }

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else app.sizeBytes
                var downloadedBytes = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L
                var currentSpeed = 0L

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(32768)
                var read: Int

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read
                    bytesSinceLastUpdate += read

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastProgressUpdate
                    if (timeDiff >= 300) {
                        currentSpeed = ((bytesSinceLastUpdate * 1000L) / timeDiff)
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        updateState(
                            app.packageName,
                            DownloadState.Downloading(
                                progress = progress.coerceIn(0f, 1f),
                                speedBytesPerSec = currentSpeed,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        )
                        lastProgressUpdate = now
                        bytesSinceLastUpdate = 0L
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                updateState(app.packageName, DownloadState.Downloaded(destinationFile))

                if (autoInstall) {
                    updateState(app.packageName, DownloadState.Installing)
                    val success = InstallerManager.installApk(
                        context = context,
                        apkFile = destinationFile,
                        packageName = app.packageName,
                        unattended = unattended
                    )
                    if (!success) {
                        updateState(app.packageName, DownloadState.Failed("Installation failed to launch"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error for ${app.packageName}", e)
                updateState(app.packageName, DownloadState.Failed(e.localizedMessage ?: "Download failed"))
            } finally {
                activeJobs.remove(app.packageName)
            }
        }

        activeJobs[app.packageName] = job
    }

    fun cancelDownload(packageName: String) {
        activeJobs[packageName]?.cancel()
        activeJobs.remove(packageName)
        updateState(packageName, DownloadState.Idle)
    }

    fun setInstalled(packageName: String) {
        updateState(packageName, DownloadState.Installed)
    }

    fun setFailed(packageName: String, error: String) {
        updateState(packageName, DownloadState.Failed(error))
    }

    fun clearState(packageName: String) {
        updateState(packageName, DownloadState.Idle)
    }

    private fun updateState(packageName: String, state: DownloadState) {
        _downloadStates.update { current ->
            current.toMutableMap().apply {
                put(packageName, state)
            }
        }
    }

    companion object {
        private const val TAG = "DownloadManager"
    }
}

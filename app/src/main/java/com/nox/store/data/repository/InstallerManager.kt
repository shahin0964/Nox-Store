package com.nox.store.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

object InstallerManager {
    private const val TAG = "InstallerManager"

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed opening unknown sources settings", e)
            }
        }
    }

    fun installApk(
        context: Context,
        apkFile: File,
        packageName: String,
        unattended: Boolean = true
    ): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e(TAG, "Cannot install: APK file does not exist or is empty")
            return false
        }

        if (!canInstallPackages(context)) {
            Log.w(TAG, "Install unknown apps permission not granted; opening settings")
            openInstallPermissionSettings(context)
            return false
        }

        // Extract the exact package name from inside the downloaded APK to avoid SessionParams mismatch
        val archivePackageName = try {
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)?.packageName
        } catch (e: Exception) {
            Log.w(TAG, "Could not inspect APK archive info", e)
            null
        }
        val realPackageName = archivePackageName?.takeIf { it.isNotBlank() } ?: packageName
        Log.d(TAG, "Installing APK for $realPackageName (stateKey=$packageName, size=${apkFile.length()})")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                installViaPackageInstaller(
                    context = context,
                    apkFile = apkFile,
                    realPackageName = realPackageName,
                    stateKey = packageName,
                    unattended = unattended
                )
            } else {
                installViaSystemIntent(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "PackageInstaller failed, falling back to System Intent", e)
            installViaSystemIntent(context, apkFile)
        }
    }

    /**
     * Android 12+ / 14+ / 15 Native Store Installation & Unattended Updates
     * Configures PACKAGE_SOURCE_STORE, USER_ACTION_NOT_REQUIRED, and Update Ownership.
     */
    private fun installViaPackageInstaller(
        context: Context,
        apkFile: File,
        realPackageName: String,
        stateKey: String,
        unattended: Boolean
    ): Boolean {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(realPackageName)
            setSize(apkFile.length())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(
                    if (unattended) {
                        PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
                    } else {
                        PackageInstaller.SessionParams.USER_ACTION_REQUIRED
                    }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setRequestUpdateOwnership(true)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = FileInputStream(apkFile)
            outputStream = session.openWrite("package_$realPackageName", 0, apkFile.length())

            val buffer = ByteArray(65536)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            session.fsync(outputStream)
            outputStream.close()
            outputStream = null
            inputStream.close()
            inputStream = null

            val intent = Intent(context, InstallStatusReceiver::class.java).apply {
                action = InstallStatusReceiver.ACTION_INSTALL_STATUS
                putExtra(InstallStatusReceiver.EXTRA_PACKAGE_NAME, realPackageName)
                putExtra(InstallStatusReceiver.EXTRA_STATE_KEY, stateKey)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                pendingIntentFlags
            )

            session.commit(pendingIntent.intentSender)
            session.close()
            Log.i(TAG, "Committed PackageInstaller session $sessionId for $realPackageName")
            return true
        } catch (e: Exception) {
            session.abandon()
            Log.e(TAG, "Error in PackageInstaller session", e)
            throw e
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    /**
     * Standard Android Installation via System PackageInstaller Intent
     */
    private fun installViaSystemIntent(context: Context, apkFile: File): Boolean {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch intent installer", e)
            false
        }
    }
}

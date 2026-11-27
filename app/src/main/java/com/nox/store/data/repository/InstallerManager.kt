package com.nox.store.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

object InstallerManager {
    private const val TAG = "InstallerManager"

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

        return try {
            if (unattended && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                installViaPackageInstaller(context, apkFile, packageName)
            } else {
                installViaSystemIntent(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "PackageInstaller failed, falling back to System Intent", e)
            installViaSystemIntent(context, apkFile)
        }
    }

    /**
     * Android 12+ (API 31+) Unattended / Silent Background Installation
     * Uses PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
     */
    private fun installViaPackageInstaller(
        context: Context,
        apkFile: File,
        packageName: String
    ): Boolean {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setSize(apkFile.length())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Allows silent update without user confirmation dialog for apps installed by Nox Store
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ ownership protection
                setRequestUpdateOwnership(true)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = FileInputStream(apkFile)
            outputStream = session.openWrite("package_$packageName", 0, apkFile.length())

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
                putExtra(InstallStatusReceiver.EXTRA_PACKAGE_NAME, packageName)
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
            Log.i(TAG, "Committed PackageInstaller session $sessionId for $packageName")
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

package com.nox.store.data.repository

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.nox.store.NoxStoreApp
import com.nox.store.worker.NotificationHelper

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "app"
        val stateKey = intent.getStringExtra(EXTRA_STATE_KEY) ?: packageName

        Log.d(TAG, "Install status received for $packageName (key=$stateKey): status=$status, message=$message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (isAppInForeground(context)) {
                        try {
                            context.startActivity(confirmIntent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Foreground startActivity failed, falling back to notification", e)
                            NotificationHelper.showPendingInstallConfirmation(context, packageName, confirmIntent)
                        }
                    } else {
                        NotificationHelper.showPendingInstallConfirmation(context, packageName, confirmIntent)
                    }
                    // Reset installing spinner to Downloaded/Idle so user isn't stuck on "Installing..." if they cancel dialog
                    NoxStoreApp.instance.downloadManager.clearState(stateKey)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Package $packageName successfully installed/updated!")
                NotificationHelper.showInstallSuccess(context, packageName)
                NoxStoreApp.instance.appsRepository.refreshInstalledStatus()
                NoxStoreApp.instance.downloadManager.setInstalled(stateKey)
                if (stateKey != packageName) {
                    NoxStoreApp.instance.downloadManager.setInstalled(packageName)
                }
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.i(TAG, "Installation aborted/cancelled by user for $packageName")
                NoxStoreApp.instance.downloadManager.clearState(stateKey)
            }

            else -> {
                Log.e(TAG, "Installation failed for $packageName: $message (code: $status)")
                NotificationHelper.showInstallFailure(context, packageName, message ?: "Install failed (code $status)")
                NoxStoreApp.instance.downloadManager.setFailed(stateKey, message ?: "Install failed (code $status)")
            }
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val processes = am?.runningAppProcesses ?: return false
            processes.any {
                it.processName == context.packageName &&
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.nox.store.ACTION_INSTALL_STATUS"
        const val EXTRA_PACKAGE_NAME = "com.nox.store.EXTRA_PACKAGE_NAME"
        const val EXTRA_STATE_KEY = "com.nox.store.EXTRA_STATE_KEY"
        private const val TAG = "InstallStatusReceiver"
    }
}

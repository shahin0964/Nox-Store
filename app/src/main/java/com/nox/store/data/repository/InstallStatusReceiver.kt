package com.nox.store.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import com.nox.store.NoxStoreApp
import com.nox.store.worker.NotificationHelper

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "app"

        Log.d(TAG, "Install status received for $packageName: status=$status, message=$message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // User confirmation required (for first time install)
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Package $packageName successfully installed/updated!")
                NotificationHelper.showInstallSuccess(context, packageName)
                NoxStoreApp.instance.appsRepository.refreshInstalledStatus()
                NoxStoreApp.instance.downloadManager.setInstalled(packageName)
            }

            else -> {
                Log.e(TAG, "Installation failed for $packageName: $message (code: $status)")
                NotificationHelper.showInstallFailure(context, packageName, message ?: "Unknown error")
                NoxStoreApp.instance.downloadManager.setFailed(packageName, message ?: "Install failed (code $status)")
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.nox.store.ACTION_INSTALL_STATUS"
        const val EXTRA_PACKAGE_NAME = "com.nox.store.EXTRA_PACKAGE_NAME"
        private const val TAG = "InstallStatusReceiver"
    }
}

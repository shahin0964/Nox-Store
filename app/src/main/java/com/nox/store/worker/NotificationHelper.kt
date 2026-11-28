package com.nox.store.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nox.store.MainActivity
import com.nox.store.R

object NotificationHelper {
    const val CHANNEL_UPDATES = "nox_store_updates"
    const val CHANNEL_DOWNLOADS = "nox_store_downloads"
    private const val NOTIF_ID_UPDATES = 1001
    private const val NOTIF_ID_SUCCESS = 1002
    private const val NOTIF_ID_FAILURE = 1003

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val updateChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "Nox Store Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for available app updates and background installations"
            }

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Nox Store Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active APK download status and speeds"
            }

            manager.createNotificationChannel(updateChannel)
            manager.createNotificationChannel(downloadChannel)
        }
    }

    fun showUpdatesAvailable(context: Context, updateCount: Int, appNames: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "updates")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$updateCount Updates Available")
            .setContentText(appNames)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Updates available for: $appNames"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_UPDATES, notification)
        } catch (_: SecurityException) {
            // Permission not granted yet
        }
    }

    fun showInstallSuccess(context: Context, packageName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Updated")
            .setContentText("$packageName was successfully updated in background.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_SUCCESS, notification)
        } catch (_: SecurityException) {}
    }

    fun showInstallFailure(context: Context, packageName: String, error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Update Failed")
            .setContentText("Could not update $packageName: $error")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_FAILURE, notification)
        } catch (_: SecurityException) {}
    }

    fun showPendingInstallConfirmation(context: Context, packageName: String, confirmIntent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ready to Update")
            .setContentText("Tap to complete update & enable future silent updates for $packageName")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(packageName.hashCode(), notification)
        } catch (_: SecurityException) {}
    }
}

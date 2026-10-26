package de.kjltowinkler.msapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

actual object PlatformNotifier {

    private const val CHANNEL_ID = "merian_vplan_updates"
    private const val CHANNEL_NAME = "Vertretungsplan"
    private const val CHANNEL_DESC = "Benachrichtigungen über neue Änderungen, Vertretungen und Ausfälle"
    private var notificationId = 2001

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    actual fun sendNotification(title: String, summary: String, details: List<String>) {
        try {
            val context = AppContextProvider.context
            ensureChannel(context)

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = if (launchIntent != null) {
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                PendingIntent.getActivity(context, 0, launchIntent, flags)
            } else {
                null
            }

            val notifResId = context.resources.getIdentifier("ic_notification", "drawable", context.packageName)
            val smallIconRes = if (notifResId != 0) notifResId else (context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setColor(0xFF395A3C.toInt())
                .setContentTitle(title)
                .setContentText(summary)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            try {
                val appIconRes = context.applicationInfo.icon
                if (appIconRes != 0) {
                    val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, appIconRes)
                    if (largeIcon != null) {
                        builder.setLargeIcon(largeIcon)
                    }
                }
            } catch (_: Throwable) {
                // Großes Icon optional
            }

            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }

            if (details.isNotEmpty()) {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText("Vertretungsplan")
                    .bigText(details.joinToString("\n"))
                builder.setStyle(bigTextStyle)
            }

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId++, builder.build())
        } catch (e: Throwable) {
            println("PlatformNotifier.android: Fehler beim Senden: ${e.message}")
        }
    }
}

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

    private const val CHANNEL_ID = "merian_vplan_urgent_v2"
    private const val CHANNEL_NAME = "Vertretungsplan-Änderungen"
    private const val CHANNEL_DESC = "Sofortige Benachrichtigungen über Vertretungen, Raumänderungen und Ausfälle"

    private const val TASK_CHANNEL_ID = "merian_tasks_reminders_v1"
    private const val TASK_CHANNEL_NAME = "Aufgaben & Fristen"
    private const val TASK_CHANNEL_DESC = "Erinnerungen an fällige und überfällige Aufgaben"

    private var notificationId = 2001
    private var taskNotificationId = 3001

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                    setSound(defaultSoundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }

            if (notificationManager.getNotificationChannel(TASK_CHANNEL_ID) == null) {
                val taskChannel = NotificationChannel(
                    TASK_CHANNEL_ID,
                    TASK_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = TASK_CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    setSound(defaultSoundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(taskChannel)
            }
        }
    }

    actual fun sendNotification(title: String, summary: String, details: List<String>) {
        try {
            val context = AppContextProvider.context
            ensureChannels(context)

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
            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setColor(0xFF395A3C.toInt())
                .setContentTitle(title)
                .setContentText(summary)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 300, 150, 300))
                .setAutoCancel(true)

            try {
                val appIconRes = context.applicationInfo.icon
                if (appIconRes != 0) {
                    val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, appIconRes)
                    if (largeIcon != null) {
                        builder.setLargeIcon(largeIcon)
                    }
                }
            } catch (_: Throwable) {}

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

    actual fun sendTaskNotification(title: String, summary: String, details: List<String>) {
        try {
            val context = AppContextProvider.context
            ensureChannels(context)

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
            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val isOverdue = title.contains("überfällig", ignoreCase = true)
            val accentColor = if (isOverdue) 0xFFBA1A1A.toInt() else 0xFF1B65A4.toInt()

            val builder = NotificationCompat.Builder(context, TASK_CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setColor(accentColor)
                .setContentTitle(title)
                .setContentText(summary)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 250, 150, 250))
                .setAutoCancel(true)

            try {
                val appIconRes = context.applicationInfo.icon
                if (appIconRes != 0) {
                    val largeIcon = android.graphics.BitmapFactory.decodeResource(context.resources, appIconRes)
                    if (largeIcon != null) {
                        builder.setLargeIcon(largeIcon)
                    }
                }
            } catch (_: Throwable) {}

            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }

            if (details.isNotEmpty()) {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(if (isOverdue) "Dringende Aufgabe" else "Aufgaben-Erinnerung")
                    .bigText(details.joinToString("\n"))
                builder.setStyle(bigTextStyle)
            }

            val manager = NotificationManagerCompat.from(context)
            manager.notify(taskNotificationId++, builder.build())
        } catch (e: Throwable) {
            println("PlatformNotifier.android: Fehler beim Senden von Task-Notif: ${e.message}")
        }
    }
}

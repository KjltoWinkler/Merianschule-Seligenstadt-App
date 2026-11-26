package de.kjltowinkler.msapp.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import de.kjltowinkler.msapp.AppRoutes
import de.kjltowinkler.msapp.MainActivity
import de.kjltowinkler.msapp.R
import de.kjltowinkler.msapp.data.wear.PlatformWearPairing
import de.kjltowinkler.msapp.data.wear.WearPairingPaths
import de.kjltowinkler.msapp.util.AppContextProvider
import de.kjltowinkler.msapp.util.NavigationEvents

class MobileWearableListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        AppContextProvider.init(applicationContext)
        PlatformWearPairing.handleIncomingMessage(
            path = messageEvent.path,
            data = messageEvent.data,
            sourceNodeId = messageEvent.sourceNodeId
        )

        if (messageEvent.path == WearPairingPaths.REQUEST_PAIR) {
            NavigationEvents.emit(AppRoutes.PAIR_SMARTWATCH_PROCEED)

            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", AppRoutes.PAIR_SMARTWATCH_PROCEED)
            }

            runCatching {
                startActivity(launchIntent)
            }

            showPairingNotification(launchIntent)
        }
    }

    private fun showPairingNotification(launchIntent: Intent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "wear_pairing_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smartwatch-Kopplung",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            9001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Smartwatch verbinden")
            .setContentText("Tippe, um deine Smartwatch mit Notes+ zu koppeln.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            manager.notify(9001, notification)
        }
    }
}

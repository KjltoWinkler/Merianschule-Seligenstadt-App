package de.kjltowinkler.msapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver zum manuellen Testen des WorkManagers über adb.
 * Befehl: adb shell am broadcast -a de.kjltowinkler.msapp.TRIGGER_SYNC
 */
class TestSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val forceNotify = intent.getBooleanExtra("force_notify", true)
        println("TestSyncReceiver: Broadcast empfangen (force_notify=$forceNotify). Starte WorkManager...")
        SubstitutionWorkScheduler.triggerImmediateCheck(context, forceTestNotification = forceNotify)
    }
}

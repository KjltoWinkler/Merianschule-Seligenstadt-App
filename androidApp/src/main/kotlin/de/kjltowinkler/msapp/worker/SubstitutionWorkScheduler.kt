package de.kjltowinkler.msapp.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Plant und steuert die Hintergrundabfragen des Vertretungsplans über den Android WorkManager.
 */
object SubstitutionWorkScheduler {

    private const val PERIODIC_WORK_TAG = "msapp_substitution_periodic_sync"
    private const val ONE_TIME_WORK_TAG = "msapp_substitution_immediate_sync"

    /**
     * Registriert die periodische Hintergrund-Synchronisation (alle 15 Minuten bei Internetverbindung).
     * Durch [ExistingPeriodicWorkPolicy.KEEP] bleibt ein bereits geplanter Zyklus unverändert erhalten.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SubstitutionSyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex-Intervall: prüft innerhalb der letzten 5 Minuten des 15-Minuten-Fensters
        )
            .setConstraints(constraints)
            .addTag("substitution_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /**
     * Löst eine sofortige Hintergrundabfrage aus (z.B. für manuellen Test oder nach Login).
     */
    fun triggerImmediateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SubstitutionSyncWorker>()
            .setConstraints(constraints)
            .addTag("substitution_sync_immediate")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}

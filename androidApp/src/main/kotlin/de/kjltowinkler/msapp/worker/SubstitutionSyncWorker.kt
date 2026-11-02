package de.kjltowinkler.msapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.kjltowinkler.msapp.data.substitution.SubstitutionSyncManager
import de.kjltowinkler.msapp.util.AppContextProvider

/**
 * Hintergrund-Worker, der auch bei geschlossener App oder nach einem Neustart periodisch
 * den Vertretungsplan abruft, Änderungen für die Klasse filtert und bei Bedarf
 * eine laute Push-Benachrichtigung mit Ton und Vibration auslöst.
 */
class SubstitutionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AppContextProvider.init(applicationContext)
            val forceNotify = inputData.getBoolean("force_notify", false)

            val diff = SubstitutionSyncManager.checkForUpdates(
                forceRefresh = true,
                notifyIfChanged = true
            )

            if (forceNotify && !diff.hasChanges) {
                de.kjltowinkler.msapp.util.PlatformNotifier.sendNotification(
                    title = "WorkManager Test erfolgreich",
                    summary = "Hintergrund-Abfrage wurde fehlerfrei ausgeführt!",
                    details = listOf(
                        "Status: Hintergrund-Worker ist aktiv",
                        "Server-Verbindung: Erfolgreich geprüft",
                        "Hintergrunddienst läuft zuverlässig."
                    )
                )
            }

            // Aufgaben-Erinnerungen und Überfälligkeits-Check im Hintergrund
            de.kjltowinkler.msapp.data.task.TaskNotificationManager.checkAndNotify()

            // App-Updates im Hintergrund prüfen und Push-Benachrichtigung senden
            de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate(forceNotify = forceNotify)

            println("SubstitutionSyncWorker: Background-Check abgeschlossen (hasChanges=${diff.hasChanges}, neu=${diff.addedEntries.size}, geändert=${diff.modifiedEntries.size})")
            Result.success()
        } catch (e: Throwable) {
            println("SubstitutionSyncWorker: Fehler beim Hintergrund-Check: ${e.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

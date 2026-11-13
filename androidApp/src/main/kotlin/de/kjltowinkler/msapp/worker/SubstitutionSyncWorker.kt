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

            // 1. Vertretungsplan im Hintergrund prüfen
            try {
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
                println("SubstitutionSyncWorker: VPlan-Check abgeschlossen (hasChanges=${diff.hasChanges})")
            } catch (e: Throwable) {
                println("SubstitutionSyncWorker: Fehler beim Vertretungsplan-Check: ${e.message}")
            }

            // 2. Aufgaben-Erinnerungen und Überfälligkeits-Check im Hintergrund
            try {
                de.kjltowinkler.msapp.data.task.TaskNotificationManager.checkAndNotify()
            } catch (e: Throwable) {
                println("SubstitutionSyncWorker: Fehler beim Aufgaben-Check: ${e.message}")
            }

            // 3. App-Updates im Hintergrund prüfen und Push-Benachrichtigung senden
            try {
                de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate(forceNotify = forceNotify)
            } catch (e: Throwable) {
                println("SubstitutionSyncWorker: Fehler beim App-Update-Check: ${e.message}")
            }

            // 4. Neuer Stundenplan im Hintergrund prüfen und Push-Benachrichtigung senden
            try {
                de.kjltowinkler.msapp.data.timetable.TimetableSyncManager.checkForUpdates(
                    forceRefresh = true,
                    notifyIfChanged = true
                )
            } catch (e: Throwable) {
                println("SubstitutionSyncWorker: Fehler beim Stundenplan-Check: ${e.message}")
            }

            Result.success()
        } catch (e: Throwable) {
            println("SubstitutionSyncWorker: Schwerer Fehler im Hintergrund-Worker: ${e.message}")
            Result.success()
        }
    }
}

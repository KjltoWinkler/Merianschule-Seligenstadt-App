package de.kjltowinkler.msapp.data.substitution

import de.kjltowinkler.msapp.util.PlatformNotifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Verwaltet die automatische und manuelle Synchronisation des Vertretungsplans
 * sowie die Erkennung und Benachrichtigung bei Änderungen.
 */
object SubstitutionSyncManager {

    private var syncJob: Job? = null
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Zuletzt bekannter Stand der Einträge zur Erkennung von Unterschieden.
     */
    var lastKnownSnapshot: List<SubstitutionEntry>? = null
        private set

    private val _changeEvents = MutableSharedFlow<SubstitutionDiffResult>(extraBufferCapacity = 1)
    val changeEvents: SharedFlow<SubstitutionDiffResult> = _changeEvents.asSharedFlow()

    /**
     * Initialisiert den bekannten Snapshot mit den aktuellen Startdaten,
     * ohne eine Benachrichtigung zu versenden.
     */
    fun initializeBaseline(entries: List<SubstitutionEntry>) {
        if (lastKnownSnapshot == null && entries.isNotEmpty()) {
            lastKnownSnapshot = entries
        }
    }

    /**
     * Prüft auf Änderungen im Vertretungsplan (über Netzwerk / Schulportal).
     * Sendet automatisch eine System-Push-Benachrichtigung, falls eine reale Änderung vorliegt.
     */
    suspend fun checkForUpdates(
        forceRefresh: Boolean = true,
        notifyIfChanged: Boolean = true
    ): SubstitutionDiffResult {
        // 1. Hole frische Daten über das Repository
        val newEntries = SubstitutionRepository.loadSubstitutions(forceRefresh = forceRefresh)

        val previous = lastKnownSnapshot
        if (previous == null) {
            // Erster Abruf nach App-Start -> als Baseline merken, keine Benachrichtigung
            lastKnownSnapshot = newEntries
            return SubstitutionDiffResult(hasChanges = false)
        }

        // 2. Differenz ermitteln
        val diff = SubstitutionChangeDetector.detectChanges(previous, newEntries)

        if (diff.hasChanges) {
            // Snapshot aktualisieren
            lastKnownSnapshot = newEntries

            // Event für UI (z.B. SnackBar) auslösen
            _changeEvents.tryEmit(diff)

            // System-Push-Benachrichtigung absenden
            if (notifyIfChanged) {
                PlatformNotifier.sendNotification(
                    title = diff.title,
                    summary = diff.summary,
                    details = diff.detailLines
                )
            }
        } else {
            // Keine inhaltlichen Änderungen, aber Snapshot aktualisieren
            lastKnownSnapshot = newEntries
        }

        return diff
    }

    /**
     * Startet die automatische periodische Abfrage im Hintergrund, solange die App aktiv ist.
     * @param intervalMinutes Intervall in Minuten (Standard: 15 Minuten)
     */
    fun startPeriodicSync(intervalMinutes: Long = 15) {
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            while (isActive) {
                delay(intervalMinutes * 60 * 1000L)
                try {
                    checkForUpdates(forceRefresh = true, notifyIfChanged = true)
                } catch (e: Throwable) {
                    println("SubstitutionSyncManager: Fehler beim periodischen Sync: ${e.message}")
                }
            }
        }
    }

    /**
     * Stoppt die periodische Abfrage.
     */
    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }
}

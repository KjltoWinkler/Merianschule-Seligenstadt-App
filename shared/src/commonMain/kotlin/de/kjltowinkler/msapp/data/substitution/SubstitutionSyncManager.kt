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

    private const val KEY_LAST_SEEN_IDS = "key_last_seen_live_entry_ids"

    /**
     * Prüft auf Änderungen im Vertretungsplan (über Netzwerk / Schulportal).
     * Sendet automatisch eine System-Push-Benachrichtigung, falls eine reale Änderung vorliegt.
     * Vergangene Tage, bereits beendete Stunden von heute sowie fremde Klassen werden ignoriert.
     */
    suspend fun checkForUpdates(
        forceRefresh: Boolean = true,
        notifyIfChanged: Boolean = true
    ): SubstitutionDiffResult {
        // 1. Hole frische Daten über das Repository
        val newEntries = SubstitutionRepository.loadSubstitutions(forceRefresh = forceRefresh)

        // 2. WICHTIG: Wenn die Daten nur aus dem Offline-Mock-Fallback stammen, NIEMALS benachrichtigen!
        if (!SubstitutionRepository.isLiveDataSource) {
            return SubstitutionDiffResult(hasChanges = false)
        }

        val previous = lastKnownSnapshot
        if (previous == null) {
            // Erster Abruf nach App-Start -> als Baseline merken, keine Benachrichtigung
            lastKnownSnapshot = newEntries
            val currentIds = newEntries.map { it.id }.joinToString(",")
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString(KEY_LAST_SEEN_IDS, currentIds)
            return SubstitutionDiffResult(hasChanges = false)
        }

        val userClass = de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.className?.ifBlank { null } ?: "09AR"

        // 3. Differenz ermitteln (nur für diesen Schüler relevante und noch bevorstehende Stunden)
        val diff = SubstitutionChangeDetector.detectChanges(
            previousEntries = previous,
            newEntries = newEntries,
            userClass = userClass,
            filterActiveAndRelevantOnly = true
        )

        if (diff.hasChanges) {
            // Snapshot aktualisieren
            lastKnownSnapshot = newEntries
            val currentIds = newEntries.map { it.id }.joinToString(",")
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString(KEY_LAST_SEEN_IDS, currentIds)

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
     * @param intervalMinutes Intervall in Minuten (Standard: 5 Minuten)
     * @param initialDelayMs Verzögerung vor dem ersten Check (Standard: 5 Sekunden)
     */
    fun startPeriodicSync(intervalMinutes: Long = 5, initialDelayMs: Long = 5000L) {
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            if (initialDelayMs > 0) {
                delay(initialDelayMs)
                try {
                    checkForUpdates(forceRefresh = true, notifyIfChanged = true)
                } catch (e: Throwable) {
                    println("SubstitutionSyncManager: Fehler beim initialen Sync: ${e.message}")
                }
            }
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

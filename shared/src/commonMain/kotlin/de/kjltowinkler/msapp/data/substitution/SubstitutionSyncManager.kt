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

    var isBaselineInitialized: Boolean = false
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
        isBaselineInitialized = true
    }

    private const val KEY_LAST_SEEN_IDS = "key_last_seen_live_entry_ids"
    private const val KEY_LAST_SNAPSHOT_JSON = "key_last_snapshot_json"

    private fun saveSnapshot(entries: List<SubstitutionEntry>) {
        lastKnownSnapshot = entries
        val currentIds = entries.map { it.id }.joinToString(",")
        de.kjltowinkler.msapp.data.storage.PlatformStorage.setString(KEY_LAST_SEEN_IDS, currentIds)
        try {
            val json = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(SubstitutionEntry.serializer()),
                entries
            )
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString(KEY_LAST_SNAPSHOT_JSON, json)
        } catch (_: Throwable) {}
    }

    /**
     * Registriert den WebSocket-Listener für sofortige Vertretungsplan-Aktualisierungen vom Server.
     */
    fun initializeRealtimeListener() {
        de.kjltowinkler.msapp.data.backend.BackendApiClient.setSubstitutionUpdateListener {
            handleRemoteSubstitutionUpdate()
        }
        de.kjltowinkler.msapp.data.backend.BackendApiClient.startRealtimeConnection()
    }

    /**
     * Wird direkt aufgerufen, sobald der Server über WebSocket ein substitutions_updated Event sendet.
     */
    suspend fun handleRemoteSubstitutionUpdate() {
        try {
            println("SubstitutionSyncManager: Remote-Signal 'substitutions_updated' empfangen -> sofortiger Check!")
            checkForUpdates(
                forceRefresh = true,
                notifyIfChanged = true,
                allowEmptyBaseline = true,
                fromRemoteTrigger = true
            )
        } catch (e: Throwable) {
            println("SubstitutionSyncManager: Fehler beim Verarbeiten des Remote-Updates: ${e.message}")
        }
    }

    /**
     * Prüft auf Änderungen im Vertretungsplan (über Netzwerk / Schulportal).
     * Sendet automatisch eine System-Push-Benachrichtigung, falls eine reale Änderung vorliegt.
     * Vergangene Tage, bereits beendete Stunden von heute sowie fremde Klassen werden für
     * schülerspezifische Alarme gefiltert.
     */
    suspend fun checkForUpdates(
        forceRefresh: Boolean = true,
        notifyIfChanged: Boolean = true,
        allowEmptyBaseline: Boolean = false,
        fromRemoteTrigger: Boolean = false
    ): SubstitutionDiffResult {
        // 1. Hole frische Daten über das Repository
        val newEntries = SubstitutionRepository.loadSubstitutions(forceRefresh = forceRefresh)

        // 2. WICHTIG: Wenn die Daten nur aus dem Offline-Mock-Fallback stammen, NIEMALS benachrichtigen!
        if (!SubstitutionRepository.isLiveDataSource) {
            return SubstitutionDiffResult(hasChanges = false)
        }

        var previous = lastKnownSnapshot
        if (previous == null) {
            val savedJson = de.kjltowinkler.msapp.data.storage.PlatformStorage.getString(KEY_LAST_SNAPSHOT_JSON)
            if (!savedJson.isNullOrBlank()) {
                try {
                    previous = kotlinx.serialization.json.Json.decodeFromString<List<SubstitutionEntry>>(savedJson)
                    lastKnownSnapshot = previous
                } catch (_: Throwable) {}
            }
        }

        val hasSavedBaseline = isBaselineInitialized || previous != null || de.kjltowinkler.msapp.data.storage.PlatformStorage.getString(KEY_LAST_SNAPSHOT_JSON) != null
        isBaselineInitialized = true

        if (previous == null) {
            if (!fromRemoteTrigger && !hasSavedBaseline) {
                // Erster Abruf nach allererstem App-Start ohne gespeicherten Snapshot -> als Baseline merken, keine Benachrichtigung
                saveSnapshot(newEntries)
                return SubstitutionDiffResult(hasChanges = false)
            }
            previous = emptyList()
        }

        val userClass = de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.className?.ifBlank { null }
            ?: de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("user_class_name")?.ifBlank { null }
            ?: "09AR"

        val canAllowEmpty = allowEmptyBaseline || fromRemoteTrigger || hasSavedBaseline

        // 3a. Differenz für Schüler-spezifische Benachrichtigung
        val diffUser = SubstitutionChangeDetector.detectChanges(
            previousEntries = previous,
            newEntries = newEntries,
            userClass = userClass,
            filterActiveAndRelevantOnly = true,
            allowEmptyBaseline = canAllowEmpty
        )

        // 3b. Differenz für alle Klassen (für globale UI-Aktualisierung und Benachrichtigung)
        val diffGlobal = SubstitutionChangeDetector.detectChanges(
            previousEntries = previous,
            newEntries = newEntries,
            userClass = userClass,
            filterActiveAndRelevantOnly = false,
            allowEmptyBaseline = canAllowEmpty
        )

        val finalDiff = if (diffUser.hasChanges) diffUser else diffGlobal

        if (diffGlobal.hasChanges || diffUser.hasChanges) {
            // Snapshot aktualisieren
            saveSnapshot(newEntries)

            // Event für UI (z.B. SnackBar und sofortiges Neu-Rendern) auslösen
            _changeEvents.tryEmit(finalDiff)

            // System-Push-Benachrichtigung absenden
            if (notifyIfChanged) {
                if (diffUser.hasChanges) {
                    val targetEntryId = diffUser.addedEntries.firstOrNull()?.id
                        ?: diffUser.modifiedEntries.firstOrNull()?.updated?.id
                    PlatformNotifier.sendNotification(
                        title = diffUser.title,
                        summary = diffUser.summary,
                        details = diffUser.detailLines,
                        entryId = targetEntryId
                    )
                } else if (diffGlobal.hasChanges) {
                    val count = diffGlobal.addedEntries.size + diffGlobal.modifiedEntries.size
                    val title = "Vertretungsplan aktualisiert"
                    val summary = if (count == 1) "1 neue Vertretungsplan-Änderung verfügbar" else "$count neue Vertretungsplan-Änderungen verfügbar"
                    val details = diffGlobal.detailLines.take(5)
                    val targetEntryId = diffGlobal.addedEntries.firstOrNull()?.id
                        ?: diffGlobal.modifiedEntries.firstOrNull()?.updated?.id
                    PlatformNotifier.sendNotification(
                        title = title,
                        summary = summary,
                        details = details,
                        entryId = targetEntryId
                    )
                }
            }
        } else {
            // Keine inhaltlichen Änderungen, aber Snapshot aktualisieren
            saveSnapshot(newEntries)
        }

        return finalDiff
    }

    /**
     * Startet die automatische periodische Abfrage im Hintergrund, solange die App aktiv ist,
     * und initialisiert den Echtzeit-WebSocket-Listener.
     * @param intervalMinutes Intervall in Minuten (Standard: 5 Minuten)
     * @param initialDelayMs Verzögerung vor dem ersten Check (Standard: 5 Sekunden)
     */
    fun startPeriodicSync(intervalMinutes: Long = 5, initialDelayMs: Long = 5000L) {
        initializeRealtimeListener()
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            if (initialDelayMs > 0) {
                delay(initialDelayMs)
                try {
                    checkForUpdates(forceRefresh = true, notifyIfChanged = true)
                } catch (e: Throwable) {
                    println("SubstitutionSyncManager: Fehler beim initialen Sync: ${e.message}")
                }
                try {
                    de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate()
                } catch (e: Throwable) {
                    println("SubstitutionSyncManager: Fehler beim App-Update-Check: ${e.message}")
                }
            }
            while (isActive) {
                delay(intervalMinutes * 60 * 1000L)
                try {
                    checkForUpdates(forceRefresh = true, notifyIfChanged = true)
                } catch (e: Throwable) {
                    println("SubstitutionSyncManager: Fehler beim periodischen Sync: ${e.message}")
                }
                try {
                    de.kjltowinkler.msapp.data.update.AppUpdateManager.checkAndNotifyUpdate()
                } catch (e: Throwable) {
                    println("SubstitutionSyncManager: Fehler beim App-Update-Check: ${e.message}")
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

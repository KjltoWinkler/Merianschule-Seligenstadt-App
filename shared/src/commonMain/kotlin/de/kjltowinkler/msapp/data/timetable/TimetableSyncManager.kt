package de.kjltowinkler.msapp.data.timetable

import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
import de.kjltowinkler.msapp.util.PlatformNotifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Verwaltet die automatische und manuelle Synchronisation des Stundenplans,
 * die Erkennung von Änderungen sowie Push-Benachrichtigungen an Schüler.
 */
object TimetableSyncManager {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val KEY_LAST_TIMETABLE_UPDATE_PREFIX = "timetable_last_updated_"
    private const val KEY_LAST_TIMETABLE_HASH_PREFIX = "timetable_last_hash_"

    private val _timetableUpdatedEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val timetableUpdatedEvents: SharedFlow<String> = _timetableUpdatedEvents.asSharedFlow()

    /**
     * Initialisiert den WebSocket-Listener für sofortige Stundenplan-Aktualisierungen vom Server.
     */
    fun initializeRealtimeListener() {
        BackendApiClient.setTimetableUpdateListener { updatedClassName ->
            handleRemoteTimetableUpdate(updatedClassName)
        }
    }

    /**
     * Wird direkt ausgeführt, sobald der Server über WebSocket ein timetable_updated Event sendet.
     */
    fun handleRemoteTimetableUpdate(updatedClassName: String?) {
        syncScope.launch {
            try {
                println("TimetableSyncManager: Remote-Signal 'timetable_updated' für Klasse '$updatedClassName' empfangen -> sofortiger Check!")
                checkForUpdates(
                    forceRefresh = true,
                    notifyIfChanged = true,
                    targetClass = updatedClassName
                )
            } catch (e: Throwable) {
                println("TimetableSyncManager: Fehler beim Verarbeiten des Remote-Updates: ${e.message}")
            }
        }
    }

    /**
     * Prüft auf neue oder aktualisierte Stundenpläne vom Server.
     * Sendet eine Push-Benachrichtigung, wenn ein neuer Stundenplan vorliegt.
     * @return true wenn ein Update vorlag und angewendet wurde, sonst false.
     */
    suspend fun checkForUpdates(
        forceRefresh: Boolean = true,
        notifyIfChanged: Boolean = true,
        targetClass: String? = null
    ): Boolean = withContext(Dispatchers.Default) {
        val userClass = TimetableRepository.getEffectiveUserClass(targetClass ?: "09AR")

        // Falls das Update für eine andere Klasse bestimmt war (z. B. 08AR != 09AR), ignorieren
        if (targetClass != null && !isClassMatching(targetClass, userClass)) {
            return@withContext false
        }

        val cleanClassKey = userClass.lowercase().replace(" ", "")
        val lastUpdateKey = "$KEY_LAST_TIMETABLE_UPDATE_PREFIX$cleanClassKey"
        val lastHashKey = "$KEY_LAST_TIMETABLE_HASH_PREFIX$cleanClassKey"

        val previousTimestamp = PlatformStorage.getString(lastUpdateKey)?.toLongOrNull() ?: 0L
        val previousHash = PlatformStorage.getString(lastHashKey) ?: ""

        val fetchRes = BackendApiClient.fetchTimetable(userClass)
        var dto = fetchRes.getOrNull()

        // Fallback für führende Nullen (z. B. 09AR <-> 9AR)
        if (dto == null || dto.days.isEmpty()) {
            val altClass = when {
                userClass.startsWith("0") && userClass.length > 1 && userClass[1].isDigit() -> userClass.substring(1)
                userClass.isNotEmpty() && userClass[0].isDigit() -> "0$userClass"
                else -> userClass
            }
            if (altClass != userClass) {
                val altRes = BackendApiClient.fetchTimetable(altClass)
                if (altRes.isSuccess && altRes.getOrNull()?.days?.isNotEmpty() == true) {
                    dto = altRes.getOrNull()
                }
            }
        }

        if (dto == null || dto.days.isEmpty()) {
            return@withContext false
        }

        // Berechne einen inhaltlichen Fingerabdruck der Stunden
        val currentHash = computeTimetableHash(dto)
        val isNewerTimestamp = dto.lastUpdated > 0L && dto.lastUpdated > previousTimestamp
        val isContentChanged = previousHash.isNotBlank() && currentHash != previousHash

        // Aktualisiere das lokale Repository und den Cache
        TimetableRepository.applyTimetableDto(dto)
        try {
            val jsonStr = kotlinx.serialization.json.Json.encodeToString(ClassTimetableDto.serializer(), dto)
            val classKey = "cached_timetable_dto_$cleanClassKey"
            PlatformStorage.setString(classKey, jsonStr)
            PlatformStorage.setString("cached_timetable_dto", jsonStr)
        } catch (_: Throwable) {}

        PlatformStorage.setString(lastUpdateKey, dto.lastUpdated.toString())
        PlatformStorage.setString(lastHashKey, currentHash)

        _timetableUpdatedEvents.tryEmit(userClass)

        // Benachrichtigen, wenn ein Update vorliegt:
        // Entweder neuer Zeitstempel oder geänderter Inhalt
        val shouldNotify = notifyIfChanged && (previousTimestamp > 0L || previousHash.isNotBlank()) && (isNewerTimestamp || isContentChanged)

        if (shouldNotify) {
            val validPeriod = if (dto.validFor.isNotBlank()) "Gültig ab: ${dto.validFor}" else "Aktuelles Schuljahr"
            PlatformNotifier.sendTimetableNotification(
                title = "Neuer Stundenplan verfügbar! 📅",
                summary = "Der Stundenplan für Klasse $userClass wurde aktualisiert.",
                details = listOf(
                    "Klasse: $userClass",
                    validPeriod,
                    "Tippe hier, um deinen neuen Stundenplan zu öffnen."
                )
            )
        }

        return@withContext (isNewerTimestamp || isContentChanged)
    }

    private fun isClassMatching(classA: String, classB: String): Boolean {
        val a = classA.trim().lowercase().replace(" ", "")
        val b = classB.trim().lowercase().replace(" ", "")
        if (a == b) return true
        val altA = if (a.startsWith("0")) a.substring(1) else "0$a"
        return altA == b
    }

    private fun computeTimetableHash(dto: ClassTimetableDto): String {
        return dto.days.entries.sortedBy { it.key }.joinToString(";") { (day, lessons) ->
            "$day:" + lessons.joinToString(",") { "${it.periodNumber}-${it.subject}-${it.room}-${it.teacherCode}" }
        }
    }
}

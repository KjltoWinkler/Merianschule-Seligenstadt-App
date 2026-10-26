package de.kjltowinkler.msapp.server.service

import de.kjltowinkler.msapp.data.schulportal.SchulportalClient
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SchulportalSyncStatus(
    val hasCredentials: Boolean,
    val schoolId: String,
    val username: String,
    val autoSyncEnabled: Boolean,
    val syncIntervalMinutes: Int,
    val lastSyncTimestamp: Long,
    val lastSyncText: String,
    val lastStatusMessage: String,
    val entriesCount: Int
)

@Serializable
data class SetSchulportalCredentialsRequest(
    val schoolId: String = "4322",
    val username: String,
    val password: String,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15
)

object SchulportalSyncService {

    var schoolId: String = System.getenv("SCHULPORTAL_SCHOOL_ID") ?: "4322"
        private set
    var username: String = System.getenv("SCHULPORTAL_USER") ?: ""
        private set
    private var password: String = System.getenv("SCHULPORTAL_PASSWORD") ?: ""

    var autoSyncEnabled: Boolean = (System.getenv("SCHULPORTAL_AUTO_SYNC") ?: "true").toBoolean()
    var syncIntervalMinutes: Int = System.getenv("SCHULPORTAL_SYNC_INTERVAL_MINUTES")?.toIntOrNull() ?: 15

    var lastSyncTimestamp: Long = 0L
        private set
    var lastSyncText: String = ""
        private set
    var lastStatusMessage: String = "Noch keine Synchronisation durchgeführt."
        private set

    val hasCredentials: Boolean
        get() = username.isNotBlank() && password.isNotBlank()

    suspend fun init() {
        // Load credentials from database if available, fallback to env vars
        DatabaseFactory.query { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT key, value FROM server_metadata WHERE key LIKE 'schulportal_%';").use { rs ->
                    while (rs.next()) {
                        val key = rs.getString("key")
                        val value = rs.getString("value")
                        when (key) {
                            "schulportal_school_id" -> if (value.isNotBlank()) schoolId = value
                            "schulportal_username" -> if (value.isNotBlank()) username = value
                            "schulportal_password" -> if (value.isNotBlank()) password = value
                            "schulportal_auto_sync" -> autoSyncEnabled = value.toBoolean()
                            "schulportal_interval" -> syncIntervalMinutes = value.toIntOrNull() ?: 15
                            "schulportal_last_sync_time" -> lastSyncTimestamp = value.toLongOrNull() ?: 0L
                            "schulportal_last_sync_text" -> lastSyncText = value
                            "schulportal_last_status" -> lastStatusMessage = value
                        }
                    }
                }
            }
        }

        if (hasCredentials) {
            println("Schulportal-Zugangsdaten für $schoolId ($username) geladen. Auto-Sync: $autoSyncEnabled ($syncIntervalMinutes Min.)")
        } else {
            println("Keine Schulportal-Zugangsdaten hinterlegt. Warte auf Konfiguration über /api/admin/schulportal/credentials")
        }
    }

    suspend fun saveCredentials(req: SetSchulportalCredentialsRequest) {
        schoolId = req.schoolId.trim()
        username = req.username.trim()
        password = req.password.trim()
        autoSyncEnabled = req.autoSyncEnabled
        syncIntervalMinutes = req.syncIntervalMinutes.coerceIn(2, 120)

        val now = System.currentTimeMillis()
        DatabaseFactory.query { conn ->
            val sql = "INSERT INTO server_metadata (key, value, updated_at) VALUES (?, ?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at;"
            conn.prepareStatement(sql).use { stmt ->
                fun saveMeta(key: String, value: String) {
                    stmt.setString(1, key)
                    stmt.setString(2, value)
                    stmt.setLong(3, now)
                    stmt.executeUpdate()
                }

                saveMeta("schulportal_school_id", schoolId)
                saveMeta("schulportal_username", username)
                saveMeta("schulportal_password", password)
                saveMeta("schulportal_auto_sync", autoSyncEnabled.toString())
                saveMeta("schulportal_interval", syncIntervalMinutes.toString())
            }
        }
    }

    suspend fun syncNow(force: Boolean = false): Result<Int> {
        if (!hasCredentials) {
            val msg = "Keine Schulportal-Zugangsdaten konfiguriert."
            lastStatusMessage = msg
            return Result.failure(IllegalStateException(msg))
        }

        try {
            println("Starte automatische Synchronisation mit Schulportal Hessen ($schoolId, Benutzer: $username)...")
            val loginResult = SchulportalClient.login(schoolId, username, password)
            if (loginResult.isFailure) {
                val err = loginResult.exceptionOrNull()?.message ?: "Unbekannter Login-Fehler"
                lastStatusMessage = "Schulportal-Login fehlgeschlagen: $err"
                println(lastStatusMessage)
                return Result.failure(Exception(lastStatusMessage))
            }

            val fetchResult = SchulportalClient.fetchSubstitutionPlan()
            if (fetchResult.isFailure) {
                val err = fetchResult.exceptionOrNull()?.message ?: "Unbekannter Abruf-Fehler"
                lastStatusMessage = "Abruf des Vertretungsplans fehlgeschlagen: $err"
                println(lastStatusMessage)
                return Result.failure(Exception(lastStatusMessage))
            }

            val remoteEntries = fetchResult.getOrNull() ?: emptyList()
            if (remoteEntries.isEmpty()) {
                lastStatusMessage = "Schulportal meldet 0 Vertretungs-Einträge."
                lastSyncTimestamp = System.currentTimeMillis()
                return Result.success(0)
            }

            // Check if there are changes compared to current entries
            val currentEntries = SubstitutionServerRepository.getAll()
            val hasChanges = currentEntries.size != remoteEntries.size || 
                             currentEntries.map { it.id }.toSet() != remoteEntries.map { it.id }.toSet()

            val updatedTime = SchulportalClient.lastUpdated ?: "Letzte Aktualisierung: ${java.time.LocalDate.now()}"
            lastSyncText = updatedTime
            lastSyncTimestamp = System.currentTimeMillis()

            if (hasChanges || force || currentEntries.isEmpty()) {
                val savedCount = SubstitutionServerRepository.replaceEntries(remoteEntries)
                lastStatusMessage = "Erfolgreich synchronisiert: $savedCount Einträge übernommen ($updatedTime)."
                println(">>> $lastStatusMessage")

                // Save metadata
                DatabaseFactory.query { conn ->
                    val sql = "INSERT INTO server_metadata (key, value, updated_at) VALUES (?, ?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at;"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, "substitutions_last_updated_text")
                        stmt.setString(2, updatedTime)
                        stmt.setLong(3, lastSyncTimestamp)
                        stmt.executeUpdate()
                    }
                }
                return Result.success(savedCount)
            } else {
                lastStatusMessage = "Keine Änderungen im Schulportal vorhanden. ($updatedTime)"
                println(lastStatusMessage)
                return Result.success(remoteEntries.size)
            }
        } catch (e: Exception) {
            lastStatusMessage = "Fehler bei der Schulportal-Synchronisation: ${e.message}"
            println(lastStatusMessage)
            return Result.failure(e)
        }
    }

    fun startBackgroundScheduler(scope: CoroutineScope) {
        scope.launch {
            // Initial sync on startup if credentials exist
            if (hasCredentials && autoSyncEnabled) {
                delay(3000) // Small grace period after server start
                syncNow()
            }

            while (isActive) {
                val intervalMs = (syncIntervalMinutes * 60 * 1000L).coerceAtLeast(60_000L)
                delay(intervalMs)

                if (hasCredentials && autoSyncEnabled) {
                    try {
                        syncNow()
                    } catch (e: Exception) {
                        println("Hintergrund-Synchronisation fehlgeschlagen: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun getStatus(): SchulportalSyncStatus {
        val count = SubstitutionServerRepository.getCount()
        return SchulportalSyncStatus(
            hasCredentials = hasCredentials,
            schoolId = schoolId,
            username = username,
            autoSyncEnabled = autoSyncEnabled,
            syncIntervalMinutes = syncIntervalMinutes,
            lastSyncTimestamp = lastSyncTimestamp,
            lastSyncText = lastSyncText,
            lastStatusMessage = lastStatusMessage,
            entriesCount = count
        )
    }
}

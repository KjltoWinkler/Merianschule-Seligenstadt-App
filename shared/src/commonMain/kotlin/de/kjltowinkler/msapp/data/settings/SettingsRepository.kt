package de.kjltowinkler.msapp.data.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.schoolway.SchoolWayConfig
import de.kjltowinkler.msapp.data.schoolway.SchoolWayRepository
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import de.kjltowinkler.msapp.data.task.TaskNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository zur Verwaltung aller App-Einstellungen.
 * Speichert alle Werte lokal im [PlatformStorage] (übersteht App-Neustarts und Offline-Phasen)
 * und synchronisiert sie automatisch in Echtzeit über das Backend.
 */
object SettingsRepository {

    private const val KEY_SETTINGS = "user_app_settings"
    private val json = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(Dispatchers.Default)

    var settings by mutableStateOf(loadPersistedSettings())
        private set

    init {
        // Initialisiere abhängige Manager mit den geladenen Einstellungen
        applySettingsToManagers(settings)
    }

    private fun loadPersistedSettings(): UserSettings {
        val cached = PlatformStorage.getString(KEY_SETTINGS)
        if (!cached.isNullOrBlank()) {
            try {
                return json.decodeFromString<UserSettings>(cached)
            } catch (_: Throwable) {}
        }
        return UserSettings()
    }

    private fun persistSettings(newSettings: UserSettings) {
        try {
            val jsonStr = json.encodeToString(newSettings)
            PlatformStorage.setString(KEY_SETTINGS, jsonStr)
        } catch (_: Throwable) {}
    }

    private fun applySettingsToManagers(s: UserSettings) {
        TaskNotificationManager.isRemindersEnabled = s.taskReminderEnabled
        TaskNotificationManager.reminderLeadTime = s.reminderLeadTime
        SchoolWayRepository.currentConfig = s.schoolWay
    }

    fun updateSettings(newSettings: UserSettings, pushToBackend: Boolean = true) {
        settings = newSettings
        persistSettings(newSettings)
        applySettingsToManagers(newSettings)

        if (pushToBackend && BackendApiClient.isLoggedIn) {
            repoScope.launch {
                try {
                    BackendApiClient.saveUserSettings(newSettings)
                } catch (_: Throwable) {}
            }
        }
    }

    fun updateDarkTheme(enabled: Boolean) {
        if (settings.isDarkTheme == enabled) return
        updateSettings(settings.copy(isDarkTheme = enabled))
    }

    fun updateDynamicColor(enabled: Boolean) {
        if (settings.isDynamicColor == enabled) return
        updateSettings(settings.copy(isDynamicColor = enabled))
    }

    fun updatePredictiveBack(enabled: Boolean) {
        if (settings.isPredictiveBackEnabled == enabled) return
        updateSettings(settings.copy(isPredictiveBackEnabled = enabled))
    }

    fun updateDelayWarnings(enabled: Boolean) {
        if (settings.delayWarningsEnabled == enabled) return
        updateSettings(settings.copy(delayWarningsEnabled = enabled))
    }

    fun updateRelaxedDismissal(enabled: Boolean) {
        if (settings.relaxedDismissalEnabled == enabled) return
        updateSettings(settings.copy(relaxedDismissalEnabled = enabled))
    }

    fun updateVplanUpdates(enabled: Boolean) {
        if (settings.vplanUpdatesEnabled == enabled) return
        updateSettings(settings.copy(vplanUpdatesEnabled = enabled))
    }

    fun updateTaskReminder(enabled: Boolean) {
        if (settings.taskReminderEnabled == enabled) return
        updateSettings(settings.copy(taskReminderEnabled = enabled))
    }

    fun updateReminderLeadTime(leadTime: String) {
        if (settings.reminderLeadTime == leadTime) return
        updateSettings(settings.copy(reminderLeadTime = leadTime))
    }

    fun updateSchoolWay(config: SchoolWayConfig) {
        if (settings.schoolWay == config) return
        updateSettings(settings.copy(schoolWay = config))
    }

    /**
     * Lädt die Einstellungen vom Backend und aktualisiert den lokalen Speicher,
     * falls das Backend neuere Daten hat.
     */
    suspend fun syncWithServer(): Result<UserSettings> = runCatching {
        if (!BackendApiClient.isLoggedIn) return@runCatching settings
        val remoteSettings = BackendApiClient.fetchUserSettings().getOrThrow()
        updateSettings(remoteSettings, pushToBackend = false)
        remoteSettings
    }

    /**
     * Startet den Echtzeit-Empfang von Einstellungsänderungen anderer Geräte.
     */
    fun startRealtimeSync() {
        BackendApiClient.startRealtimeSettingsUpdates {
            syncWithServer()
        }
    }
}

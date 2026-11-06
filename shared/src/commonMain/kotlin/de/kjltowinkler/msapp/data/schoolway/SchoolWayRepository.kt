package de.kjltowinkler.msapp.data.schoolway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Datenmodell für die Konfiguration "Mein Schulweg".
 * Entspricht den Figma-Entwürfen `settings#meinschulweg.png` und `settings#einstiegsheet.png`.
 */
@Serializable
data class SchoolWayConfig(
    val morningStartStop: String = "Mainhausen-Zellhausen Wiesenstraße",
    val morningEndStop: String = "Seligenstadt Würzburger Straße (Kleingärten)",
    val afternoonEndStop: String = "Mainhausen-Zellhausen Wiesenstraße",
    val walkingMinutes: Int = 5,
    val busLine: String = "OF-85"
)

/**
 * Repository zur Verwaltung der Schulweg-Einstellungen und Haltestellen-Empfehlungen.
 */
object SchoolWayRepository {

    private const val KEY_SCHOOL_WAY_CONFIG = "cached_school_way_config"
    private const val KEY_SCHOOL_WAY_RECOMMENDATIONS = "cached_school_way_recommendations"
    private val json = Json { ignoreUnknownKeys = true }

    val defaultRecommendations: List<String> = listOf(
        "Mainhausen-Zellhausen Wiesenstraße",
        "Seligenstadt-Würzburger Straße (Kleingärten)",
        "Seligenstadt-Schwimmbad",
        "Seligenstadt Merianschule",
        "Seligenstadt Bahnhof",
        "Mainhausen-Mainflingen Bürgerhaus",
        "Mainhausen-Zellhausen Rathaus"
    )

    var currentConfig by mutableStateOf(loadPersistedConfig())
    var customRecommendations by mutableStateOf(loadPersistedRecommendations())

    private fun loadPersistedConfig(): SchoolWayConfig {
        val cached = PlatformStorage.getString(KEY_SCHOOL_WAY_CONFIG)
        if (!cached.isNullOrBlank()) {
            try {
                return json.decodeFromString<SchoolWayConfig>(cached)
            } catch (_: Throwable) {}
        }
        return SchoolWayConfig()
    }

    private fun persistConfig() {
        try {
            val jsonStr = json.encodeToString(currentConfig)
            PlatformStorage.setString(KEY_SCHOOL_WAY_CONFIG, jsonStr)
        } catch (_: Throwable) {}
    }

    private fun loadPersistedRecommendations(): List<String> {
        val cached = PlatformStorage.getString(KEY_SCHOOL_WAY_RECOMMENDATIONS)
        if (!cached.isNullOrBlank()) {
            try {
                return json.decodeFromString<List<String>>(cached)
            } catch (_: Throwable) {}
        }
        return defaultRecommendations
    }

    private fun persistRecommendations() {
        try {
            val jsonStr = json.encodeToString(customRecommendations)
            PlatformStorage.setString(KEY_SCHOOL_WAY_RECOMMENDATIONS, jsonStr)
        } catch (_: Throwable) {}
    }

    fun updateMorningStops(startStop: String, endStop: String) {
        currentConfig = currentConfig.copy(
            morningStartStop = startStop,
            morningEndStop = endStop
        )
        persistConfig()
    }

    fun updateAfternoonStop(endStop: String) {
        currentConfig = currentConfig.copy(
            afternoonEndStop = endStop
        )
        persistConfig()
    }

    fun updateWalkingMinutes(minutes: Int) {
        currentConfig = currentConfig.copy(
            walkingMinutes = minutes.coerceIn(1, 15)
        )
        persistConfig()
    }

    fun resetToDefaults() {
        currentConfig = SchoolWayConfig()
        customRecommendations = defaultRecommendations
        persistConfig()
        persistRecommendations()
    }

    fun clearRecommendations() {
        customRecommendations = emptyList()
        persistRecommendations()
    }
}

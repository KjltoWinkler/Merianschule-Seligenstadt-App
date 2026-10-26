package de.kjltowinkler.msapp.data.schoolway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Datenmodell für die Konfiguration "Mein Schulweg".
 * Entspricht den Figma-Entwürfen `settings#meinschulweg.png` und `settings#einstiegsheet.png`.
 */
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

    var currentConfig by mutableStateOf(SchoolWayConfig())

    val defaultRecommendations: List<String> = listOf(
        "Mainhausen-Zellhausen Wiesenstraße",
        "Seligenstadt-Würzburger Straße (Kleingärten)",
        "Seligenstadt-Schwimmbad",
        "Seligenstadt Merianschule",
        "Seligenstadt Bahnhof",
        "Mainhausen-Mainflingen Bürgerhaus",
        "Mainhausen-Zellhausen Rathaus"
    )

    var customRecommendations by mutableStateOf(defaultRecommendations)

    fun updateMorningStops(startStop: String, endStop: String) {
        currentConfig = currentConfig.copy(
            morningStartStop = startStop,
            morningEndStop = endStop
        )
    }

    fun updateAfternoonStop(endStop: String) {
        currentConfig = currentConfig.copy(
            afternoonEndStop = endStop
        )
    }

    fun updateWalkingMinutes(minutes: Int) {
        currentConfig = currentConfig.copy(
            walkingMinutes = minutes.coerceIn(1, 15)
        )
    }

    fun resetToDefaults() {
        currentConfig = SchoolWayConfig()
        customRecommendations = defaultRecommendations
    }

    fun clearRecommendations() {
        customRecommendations = emptyList()
    }
}

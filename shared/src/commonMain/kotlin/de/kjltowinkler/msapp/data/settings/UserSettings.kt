package de.kjltowinkler.msapp.data.settings

import de.kjltowinkler.msapp.data.schoolway.SchoolWayConfig
import kotlinx.serialization.Serializable

/**
 * Zentrales Datenmodell für alle Benutzereinstellungen der Merianschule App.
 * Wird lokal im PlatformStorage gespeichert und bei bestehender Anmeldung
 * in Echtzeit über das Backend zwischen allen Geräten des Nutzers synchronisiert.
 */
@Serializable
data class UserSettings(
    val isDarkTheme: Boolean = true,
    val isDynamicColor: Boolean = true,
    val isPredictiveBackEnabled: Boolean = true,
    val delayWarningsEnabled: Boolean = true,
    val relaxedDismissalEnabled: Boolean = true,
    val vplanUpdatesEnabled: Boolean = true,
    val taskReminderEnabled: Boolean = true,
    val reminderLeadTime: String = "1 Tag",
    val schoolWay: SchoolWayConfig = SchoolWayConfig()
)

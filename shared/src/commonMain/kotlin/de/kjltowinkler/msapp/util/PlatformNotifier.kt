package de.kjltowinkler.msapp.util

/**
 * Erlaubt das plattformübergreifende Versenden von Push-Benachrichtigungen
 * an das Betriebssystem (Android NotificationManager / Desktop SystemTray).
 */
expect object PlatformNotifier {
    /**
     * Sendet eine Benachrichtigung an den Nutzer.
     * @param title Kurzer prägnanter Titel (z. B. "Vertretungsplan: Unterrichtsentfall")
     * @param summary Kurze Zusammenfassung für die Einzeilenvorschau (z. B. "3. Std Deutsch (Af) entfällt")
     * @param details Detaillierte Liste der Änderungen (für die aufklappbare Mehrzeilen-Ansicht)
     */
    fun sendNotification(title: String, summary: String, details: List<String> = emptyList())
}

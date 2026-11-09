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
     * @param entryId Optionale ID des geänderten Vertretungsplan-Eintrags zum gezielten Anspringen und Hervorheben.
     */
    fun sendNotification(
        title: String,
        summary: String,
        details: List<String> = emptyList(),
        entryId: String? = null
    )

    /**
     * Sendet eine Benachrichtigung zu Aufgaben (Erinnerungen an fällige / überfällige Fristen).
     * @param taskId Optionale ID der betroffenen Aufgabe zum gezielten Anspringen und Hervorheben.
     */
    fun sendTaskNotification(
        title: String,
        summary: String,
        details: List<String> = emptyList(),
        taskId: String? = null
    )

    /**
     * Sendet eine Push-Benachrichtigung, wenn ein neues App-Update verfügbar ist.
     * @return true wenn die Benachrichtigung erfolgreich an das System übergeben wurde, sonst false.
     */
    fun sendUpdateNotification(title: String, summary: String, details: List<String> = emptyList()): Boolean
}


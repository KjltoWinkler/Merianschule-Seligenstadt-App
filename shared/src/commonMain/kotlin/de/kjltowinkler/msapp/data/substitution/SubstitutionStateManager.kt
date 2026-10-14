package de.kjltowinkler.msapp.data.substitution

/**
 * Verwaltet den Lesezustand und die Favoriten/Markierungen von Vertretungsplan-Einträgen.
 * 
 * Funktionsweise:
 * - Ein Eintrag gilt als "neu" (grüner Indikator), solange er vom Nutzer noch nicht angeklickt / gesehen wurde.
 * - Sobald der Nutzer auf einen Eintrag klickt, wird er als gelesen markiert und der grüne Punkt verschwindet.
 * - Markierungen (Sterne) können ebenfalls lokal umgeschaltet werden.
 */
object SubstitutionStateManager {

    // Set der IDs von gelesenen Einträgen
    private val readEntryIds = mutableSetOf<String>()

    // Set der IDs von markierten Einträgen
    private val markedEntryIds = mutableSetOf<String>()

    // Initialisiert bereits markierte Einträge aus dem Backend/JSON
    fun initMarked(entries: List<SubstitutionEntry>) {
        entries.forEach { entry ->
            if (entry.isMarked) {
                markedEntryIds.add(entry.id)
            }
        }
    }

    /**
     * Prüft, ob ein Eintrag neu ist.
     * Ein Eintrag ist neu, wenn er noch nicht als gelesen markiert wurde.
     */
    fun isNew(entryId: String): Boolean {
        return !readEntryIds.contains(entryId)
    }

    /**
     * Markiert einen Eintrag als gelesen (isNew = false).
     */
    fun markAsRead(entryId: String) {
        readEntryIds.add(entryId)
    }

    /**
     * Prüft, ob ein Eintrag markiert (favorisiert) ist.
     */
    fun isMarked(entryId: String): Boolean {
        return markedEntryIds.contains(entryId)
    }

    /**
     * Schaltet den Markierungszustand um.
     */
    fun toggleMarked(entryId: String): Boolean {
        return if (markedEntryIds.contains(entryId)) {
            markedEntryIds.remove(entryId)
            false
        } else {
            markedEntryIds.add(entryId)
            true
        }
    }

    /**
     * Setzt den Markierungszustand explizit auf einen bestimmten Wert.
     */
    fun setMarked(entryId: String, marked: Boolean) {
        if (marked) {
            markedEntryIds.add(entryId)
        } else {
            markedEntryIds.remove(entryId)
        }
    }
}

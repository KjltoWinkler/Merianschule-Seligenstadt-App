package de.kjltowinkler.msapp.data.substitution

/**
 * Repräsentiert das Ergebnis einer Vertretungsplan-Differenzanalyse.
 */
data class SubstitutionDiffResult(
    val hasChanges: Boolean,
    val addedEntries: List<SubstitutionEntry> = emptyList(),
    val removedEntries: List<SubstitutionEntry> = emptyList(),
    val modifiedEntries: List<SubstitutionModification> = emptyList(),
    val title: String = "",
    val summary: String = "",
    val detailLines: List<String> = emptyList()
)

data class SubstitutionModification(
    val previous: SubstitutionEntry,
    val updated: SubstitutionEntry,
    val description: String
)

/**
 * Zuverlässiger Differenz-Detektor für Vertretungsplan-Einträge.
 * Erkennt neue Entfälle, Vertretungen, Raumänderungen sowie zurückgenommene Vertretungen.
 */
object SubstitutionChangeDetector {

    /**
     * Erzeugt einen eindeutigen Schlüssel für einen Eintrag zur Zuordnung.
     */
    private fun entryKey(entry: SubstitutionEntry): String {
        return "${entry.date.trim()}|${entry.period.trim().lowercase()}|${entry.className.trim().lowercase()}|${entry.subject.trim().lowercase()}"
    }

    /**
     * Vergleicht zwei Listen von Einträgen und ermittelt zuverlässig alle Änderungen.
     */
    fun detectChanges(
        previousEntries: List<SubstitutionEntry>,
        newEntries: List<SubstitutionEntry>
    ): SubstitutionDiffResult {
        if (previousEntries.isEmpty()) {
            // Wenn keine vorherigen Einträge vorlagen (z.B. erster Start), keine Benachrichtigung auslösen
            return SubstitutionDiffResult(hasChanges = false)
        }

        val prevMap = mutableMapOf<String, MutableList<SubstitutionEntry>>()
        for (entry in previousEntries) {
            val key = entryKey(entry)
            prevMap.getOrPut(key) { mutableListOf() }.add(entry)
        }

        val newMap = mutableMapOf<String, MutableList<SubstitutionEntry>>()
        for (entry in newEntries) {
            val key = entryKey(entry)
            newMap.getOrPut(key) { mutableListOf() }.add(entry)
        }

        val added = mutableListOf<SubstitutionEntry>()
        val modified = mutableListOf<SubstitutionModification>()
        val removed = mutableListOf<SubstitutionEntry>()

        // Ermittle alle Tage, die im neuen Plan vorhanden sind
        val newDates = newEntries.map { it.date }.toSet()

        // 1. Suche nach neuen oder geänderten Einträgen
        for (entry in newEntries) {
            val key = entryKey(entry)
            val matchedList = prevMap[key]
            if (matchedList == null || matchedList.isEmpty()) {
                // Neuer Eintrag
                added.add(entry)
            } else {
                // Ein oder mehrere Treffer mit gleichem Basisschlüssel: Suche nach inhaltlicher Übereinstimmung
                val exactMatchIndex = matchedList.indexOfFirst { prev ->
                    prev.type == entry.type &&
                    prev.details == entry.details &&
                    prev.roomInfo == entry.roomInfo
                }
                if (exactMatchIndex >= 0) {
                    // Identisch -> aus prevMap entfernen, damit Duplikate korrekt gematcht werden
                    matchedList.removeAt(exactMatchIndex)
                } else {
                    // Geändert (z.B. Raum oder Vertretungslehrer)
                    val prev = matchedList.removeAt(0)
                    val diffDesc = buildModificationDescription(prev, entry)
                    modified.add(SubstitutionModification(prev, entry, diffDesc))
                }
            }
        }

        // 2. Suche nach Einträgen, die im neuen Plan für die gleichen Tage nicht mehr existieren
        for ((_, remainingList) in prevMap) {
            for (prevEntry in remainingList) {
                // Nur als "entfernt" werten, wenn der Tag im neuen Plan grundsätzlich noch abgedeckt ist
                if (prevEntry.date in newDates) {
                    removed.add(prevEntry)
                }
            }
        }

        val totalChanges = added.size + modified.size + removed.size
        if (totalChanges == 0) {
            return SubstitutionDiffResult(hasChanges = false)
        }

        // Baue informative Texte und Detailzeilen
        val detailLines = mutableListOf<String>()

        // Neue Ausfälle und Vertretungen
        for (entry in added) {
            val dateLabel = formatShortDate(entry.date)
            val isEntfall = entry.type.equals("Entfall", ignoreCase = true)
            val line = if (isEntfall) {
                "• $dateLabel ${entry.period} ${entry.subject}: Entfall (${entry.details})"
            } else {
                "• $dateLabel ${entry.period} ${entry.subject}: ${entry.type} (${entry.details})"
            }
            detailLines.add(line)
        }

        // Geänderte Vertretungen
        for (mod in modified) {
            val dateLabel = formatShortDate(mod.updated.date)
            detailLines.add("• $dateLabel ${mod.updated.period} ${mod.updated.subject}: ${mod.description}")
        }

        // Aufgehobene Vertretungen (findet wieder regulär statt)
        for (rem in removed) {
            val dateLabel = formatShortDate(rem.date)
            detailLines.add("• $dateLabel ${rem.period} ${rem.subject}: Findet wieder regulär statt")
        }

        val (title, summary) = buildNotificationSummary(added, modified, removed)

        return SubstitutionDiffResult(
            hasChanges = true,
            addedEntries = added,
            removedEntries = removed,
            modifiedEntries = modified,
            title = title,
            summary = summary,
            detailLines = detailLines
        )
    }

    private fun buildModificationDescription(prev: SubstitutionEntry, updated: SubstitutionEntry): String {
        val parts = mutableListOf<String>()
        if (prev.roomInfo != updated.roomInfo && updated.roomInfo.isNotBlank()) {
            parts.add(updated.roomInfo)
        }
        if (prev.details != updated.details && updated.details.isNotBlank()) {
            parts.add(updated.details)
        }
        if (prev.type != updated.type) {
            parts.add("Jetzt ${updated.type}")
        }
        return if (parts.isNotEmpty()) parts.joinToString(" • ") else "Eintrag aktualisiert"
    }

    private fun buildNotificationSummary(
        added: List<SubstitutionEntry>,
        modified: List<SubstitutionModification>,
        removed: List<SubstitutionEntry>
    ): Pair<String, String> {
        val totalCount = added.size + modified.size + removed.size

        if (totalCount == 1) {
            if (added.isNotEmpty()) {
                val entry = added.first()
                val dateLabel = formatShortDate(entry.date)
                val isEntfall = entry.type.equals("Entfall", ignoreCase = true)
                val title = if (isEntfall) "Vertretungsplan: Unterrichtsentfall" else "Vertretungsplan: Neue Vertretung"
                val summary = if (isEntfall) {
                    "$dateLabel • ${entry.period} ${entry.subject} entfällt (${entry.details})"
                } else {
                    "$dateLabel • ${entry.period} ${entry.subject}: ${entry.type} (${entry.details})"
                }
                return Pair(title, summary)
            } else if (modified.isNotEmpty()) {
                val mod = modified.first()
                val dateLabel = formatShortDate(mod.updated.date)
                val title = "Vertretungsplan: Änderung"
                val summary = "$dateLabel • ${mod.updated.period} ${mod.updated.subject}: ${mod.description}"
                return Pair(title, summary)
            } else {
                val rem = removed.first()
                val dateLabel = formatShortDate(rem.date)
                val title = "Vertretungsplan: Änderung"
                val summary = "$dateLabel • ${rem.period} ${rem.subject} findet wieder regulär statt"
                return Pair(title, summary)
            }
        }

        // Mehrere Änderungen
        val title = "Vertretungsplan aktualisiert ($totalCount Änderungen)"
        val firstHighlight = when {
            added.isNotEmpty() -> {
                val e = added.first()
                val verb = if (e.type.equals("Entfall", ignoreCase = true)) "entfällt" else e.type
                "${e.period} ${e.subject} $verb"
            }
            modified.isNotEmpty() -> {
                val m = modified.first()
                "${m.updated.period} ${m.updated.subject} geändert"
            }
            else -> {
                val r = removed.first()
                "${r.period} ${r.subject} findet statt"
            }
        }
        val summary = "$totalCount Änderungen: $firstHighlight und weitere"
        return Pair(title, summary)
    }

    private fun formatShortDate(isoDate: String): String {
        val parts = isoDate.split("-")
        return if (parts.size == 3) {
            "${parts[2]}.${parts[1]}."
        } else {
            isoDate
        }
    }
}

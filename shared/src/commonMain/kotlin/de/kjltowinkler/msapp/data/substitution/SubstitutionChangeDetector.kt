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
     * Prüft, ob ein Vertretungsplan-Eintrag für HEUTE (noch bevorstehende Stunde)
     * oder für die ZUKUNFT (Morgen, Folgetage) aktiv ist.
     * Einträge aus der Vergangenheit (z.B. vergangene Tage oder bereits beendete Stunden von heute)
     * geben false zurück.
     */
    fun isEntryActiveOrUpcoming(
        entry: SubstitutionEntry,
        today: de.kjltowinkler.msapp.data.task.SimpleDate = de.kjltowinkler.msapp.data.task.getTodayDate(),
        currentTime: de.kjltowinkler.msapp.data.task.SimpleTime = de.kjltowinkler.msapp.data.task.getCurrentTime()
    ): Boolean {
        val entryDate = entry.date.trim()
        if (entryDate.isBlank()) return false
        val todayIso = today.toIsoString()

        // 1. Datum liegt vor heute (z.B. gestern oder letzter Donnerstag) -> vergangen!
        if (entryDate < todayIso) return false

        // 2. Datum liegt in der Zukunft (z.B. morgen oder nächste Woche) -> bevorstehend!
        if (entryDate > todayIso) return true

        // 3. Datum ist HEUTE: Prüfe, ob die Schulstunde laut Stundenraster bereits beendet ist
        val numbers = Regex("""\d+""").findAll(entry.period).mapNotNull { it.value.toIntOrNull() }.toList()
        val latestPeriod = numbers.maxOrNull() ?: 1
        val (endHour, endMinute) = getPeriodEndTime(latestPeriod)

        val currentMinutes = currentTime.hour * 60 + currentTime.minute
        val endMinutes = endHour * 60 + endMinute

        // Stunde ist noch nicht vorüber
        return currentMinutes < endMinutes
    }

    fun getPeriodEndTime(periodNumber: Int): Pair<Int, Int> = when (periodNumber) {
        1 -> 8 to 40
        2 -> 9 to 30
        3 -> 10 to 30
        4 -> 11 to 20
        5 -> 12 to 20
        6 -> 13 to 10
        7 -> 14 to 0
        8 -> 14 to 45
        9 -> 15 to 35
        10 -> 16 to 25
        else -> 17 to 0
    }

    /**
     * Vergleicht zwei Listen von Einträgen und ermittelt zuverlässig alle Änderungen.
     * @param filterActiveAndRelevantOnly Wenn true, werden vergangene Tage/Stunden und fremde Klassen ignoriert.
     */
    fun detectChanges(
        previousEntries: List<SubstitutionEntry>,
        newEntries: List<SubstitutionEntry>,
        userClass: String = "09AR",
        filterActiveAndRelevantOnly: Boolean = false,
        today: de.kjltowinkler.msapp.data.task.SimpleDate = de.kjltowinkler.msapp.data.task.getTodayDate(),
        currentTime: de.kjltowinkler.msapp.data.task.SimpleTime = de.kjltowinkler.msapp.data.task.getCurrentTime()
    ): SubstitutionDiffResult {
        if (previousEntries.isEmpty()) {
            // Wenn keine vorherigen Einträge vorlagen (z.B. erster Start), keine Benachrichtigung auslösen
            return SubstitutionDiffResult(hasChanges = false)
        }

        val effectivePrev = if (filterActiveAndRelevantOnly) {
            previousEntries.filter {
                isEntryActiveOrUpcoming(it, today, currentTime) &&
                de.kjltowinkler.msapp.data.courses.CourseRepository.isEntryRelevant(it, userClass)
            }
        } else previousEntries

        val effectiveNew = if (filterActiveAndRelevantOnly) {
            newEntries.filter {
                isEntryActiveOrUpcoming(it, today, currentTime) &&
                de.kjltowinkler.msapp.data.courses.CourseRepository.isEntryRelevant(it, userClass)
            }
        } else newEntries

        val prevMap = mutableMapOf<String, MutableList<SubstitutionEntry>>()
        for (entry in effectivePrev) {
            val key = entryKey(entry)
            prevMap.getOrPut(key) { mutableListOf() }.add(entry)
        }

        val added = mutableListOf<SubstitutionEntry>()
        val modified = mutableListOf<SubstitutionModification>()
        val removed = mutableListOf<SubstitutionEntry>()

        // Ermittle alle Tage, die im neuen Plan vorhanden sind
        val newDates = effectiveNew.map { it.date }.toSet()

        // 1. Suche nach neuen oder geänderten Einträgen
        for (entry in effectiveNew) {
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
            val dateLabel = formatShortDate(entry.date, today)
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
            val dateLabel = formatShortDate(mod.updated.date, today)
            detailLines.add("• $dateLabel ${mod.updated.period} ${mod.updated.subject}: ${mod.description}")
        }

        // Aufgehobene Vertretungen (findet wieder regulär statt)
        for (rem in removed) {
            val dateLabel = formatShortDate(rem.date, today)
            detailLines.add("• $dateLabel ${rem.period} ${rem.subject}: Findet wieder regulär statt")
        }

        val (title, summary) = buildNotificationSummary(added, modified, removed, today)

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
        removed: List<SubstitutionEntry>,
        today: de.kjltowinkler.msapp.data.task.SimpleDate = de.kjltowinkler.msapp.data.task.getTodayDate()
    ): Pair<String, String> {
        val totalCount = added.size + modified.size + removed.size

        if (totalCount == 1) {
            if (added.isNotEmpty()) {
                val entry = added.first()
                val dateLabel = formatShortDate(entry.date, today)
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
                val dateLabel = formatShortDate(mod.updated.date, today)
                val title = "Vertretungsplan: Änderung"
                val summary = "$dateLabel • ${mod.updated.period} ${mod.updated.subject}: ${mod.description}"
                return Pair(title, summary)
            } else {
                val rem = removed.first()
                val dateLabel = formatShortDate(rem.date, today)
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

    fun formatShortDate(
        isoDate: String,
        today: de.kjltowinkler.msapp.data.task.SimpleDate = de.kjltowinkler.msapp.data.task.getTodayDate()
    ): String {
        val todayIso = today.toIsoString()
        val tomorrowIso = today.plusDays(1).toIsoString()
        if (isoDate == todayIso) return "Heute"
        if (isoDate == tomorrowIso) return "Morgen"

        val parts = isoDate.split("-")
        return if (parts.size == 3) {
            "${parts[2]}.${parts[1]}."
        } else {
            isoDate
        }
    }
}

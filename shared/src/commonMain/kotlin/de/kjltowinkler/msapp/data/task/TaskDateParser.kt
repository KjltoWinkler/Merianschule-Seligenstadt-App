package de.kjltowinkler.msapp.data.task

/**
 * Hilfsfunktionen zum präzisen Parsen von Datums- und Zeitangaben einer Aufgabe
 * zur exakten Berechnung von Fälligkeit, Überfälligkeit und Sortierung.
 */
object TaskDateParser {

    private val MONTH_MAP = mapOf(
        "jan" to 1, "jan." to 1, "januar" to 1,
        "feb" to 2, "feb." to 2, "februar" to 2,
        "mär" to 3, "mär." to 3, "märz" to 3, "mar" to 3, "maerz" to 3,
        "apr" to 4, "apr." to 4, "april" to 4,
        "mai" to 5,
        "jun" to 6, "juni" to 6, "june" to 6,
        "jul" to 7, "juli" to 7, "july" to 7,
        "aug" to 8, "aug." to 8, "august" to 8,
        "sep" to 9, "sep." to 9, "sept" to 9, "sept." to 9, "september" to 9,
        "okt" to 10, "okt." to 10, "oktober" to 10, "oct" to 10, "october" to 10,
        "nov" to 11, "nov." to 11, "november" to 11,
        "dez" to 12, "dez." to 12, "dezember" to 12, "dec" to 12, "december" to 12
    )

    /**
     * Parst dueDate, dueTime und deadlineDate einer Aufgabe in einen Unix-Timestamp (in ms).
     * Gibt null zurück, wenn keine Zeit/Frist angegeben ist.
     */
    fun parseDueDateTime(dueDate: String, dueTime: String, deadlineDate: String): Long? {
        val dateCandidate = dueDate.ifBlank { deadlineDate }.trim()
        val timeCandidate = dueTime.ifBlank {
            val match = Regex("""(\d{1,2}):(\d{2})""").find(deadlineDate)
            match?.value ?: ""
        }.trim()

        if (dateCandidate.isBlank() && timeCandidate.isBlank()) {
            return null
        }

        val today = getTodayDate()
        var year: Int? = null
        var month: Int? = null
        var day: Int? = null

        // 1. Prüfe ISO Format (yyyy-MM-dd)
        val isoMatch = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""").find(dateCandidate)
        if (isoMatch != null) {
            year = isoMatch.groupValues[1].toIntOrNull()
            month = isoMatch.groupValues[2].toIntOrNull()
            day = isoMatch.groupValues[3].toIntOrNull()
        }

        // 2. Prüfe numerisches deutsches Format (d.M.yyyy oder dd.MM.yyyy)
        if (year == null || month == null || day == null) {
            val numMatch = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""").find(dateCandidate)
            if (numMatch != null) {
                day = numMatch.groupValues[1].toIntOrNull()
                month = numMatch.groupValues[2].toIntOrNull()
                year = numMatch.groupValues[3].toIntOrNull()
            }
        }

        // 3. Prüfe Format mit Monatsnamen (z. B. "2. Nov. 2026" oder "Mo., 2. Nov.")
        if (year == null || month == null || day == null) {
            val dayMatch = Regex("""(\d{1,2})\.\s*([A-Za-zäÄöÖüÜ]+)""").find(dateCandidate)
            if (dayMatch != null) {
                val d = dayMatch.groupValues[1].toIntOrNull()
                val mName = dayMatch.groupValues[2].lowercase().trimEnd('.')
                val m = MONTH_MAP[mName]
                if (d != null && m != null) {
                    day = d
                    month = m
                    val yearMatch = Regex("""\b(20\d{2})\b""").find(dateCandidate)
                    year = yearMatch?.groupValues?.get(1)?.toIntOrNull() ?: today.year
                }
            }
        }

        // 4. Nur Zeit angegeben (z. B. "14:00 Uhr") -> Heute als Datum annehmen
        if (day == null && timeCandidate.isNotBlank()) {
            day = today.dayOfMonth
            month = today.monthNumber
            year = today.year
        }

        if (year == null || month == null || day == null) {
            return null
        }

        // Uhrzeit bestimmen
        var hour = 23
        var minute = 59
        if (timeCandidate.isNotBlank()) {
            val timeMatch = Regex("""(\d{1,2}):(\d{2})""").find(timeCandidate)
            if (timeMatch != null) {
                val h = timeMatch.groupValues[1].toIntOrNull()
                val m = timeMatch.groupValues[2].toIntOrNull()
                if (h != null && m != null && h in 0..23 && m in 0..59) {
                    hour = h
                    minute = m
                }
            }
        }

        return parseDateTimeToEpochMillis(year, month, day, hour, minute)
    }
}

/**
 * Gibt den Fälligkeits-Timestamp in Millisekunden zurück oder null, falls keine Frist definiert ist.
 */
fun TaskItem.getDueEpochMillis(): Long? {
    return TaskDateParser.parseDueDateTime(dueDate, dueTime, deadlineDate)
}

/**
 * Prüft, ob eine Aufgabe überfällig ist.
 * Erledigte Aufgaben sind niemals überfällig.
 */
fun TaskItem.isOverdue(nowMillis: Long = getCurrentEpochMillis()): Boolean {
    if (isCompleted) return false
    val due = getDueEpochMillis() ?: return false
    return nowMillis > due
}

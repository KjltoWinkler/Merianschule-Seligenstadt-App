package de.kjltowinkler.msapp.data.task

data class SimpleDate(
    val year: Int,
    val monthNumber: Int,
    val dayOfMonth: Int
) {
    // dayOfWeek: 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    val dayOfWeek: Int
        get() {
            var y = year
            var m = monthNumber
            if (m < 3) {
                m += 12
                y -= 1
            }
            val k = y % 100
            val j = y / 100
            val h = (dayOfMonth + (13 * (m + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
            // Zeller's congruence: 0 = Saturday, 1 = Sunday, 2 = Monday, ...
            return ((h + 6) % 7) // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
        }

    fun formattedGerman(): String = "$dayOfMonth.$monthNumber.$year"

    fun formattedCompletedGerman(): String {
        val weekdays = listOf("So.", "Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.")
        val months = listOf("", "Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez.")
        val w = weekdays.getOrElse(dayOfWeek) { "" }
        val m = months.getOrElse(monthNumber) { "" }
        return "$w, $dayOfMonth. $m"
    }

    fun toIsoString(): String = "$year-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"

    fun plusDays(days: Int): SimpleDate {
        var d = dayOfMonth + days
        var m = monthNumber
        var y = year
        while (true) {
            val daysInMonth = when (m) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
                else -> 31
            }
            if (d <= daysInMonth) break
            d -= daysInMonth
            m++
            if (m > 12) {
                m = 1
                y++
            }
        }
        return SimpleDate(y, m, d)
    }

    fun minusDays(days: Int): SimpleDate {
        if (days <= 0) return plusDays(-days)
        var d = dayOfMonth - days
        var m = monthNumber
        var y = year
        while (d < 1) {
            m--
            if (m < 1) {
                m = 12
                y--
            }
            val daysInPrevMonth = when (m) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
                else -> 31
            }
            d += daysInPrevMonth
        }
        return SimpleDate(y, m, d)
    }

    /**
     * Liefert Montag und Sonntag der aktiven Schulwoche.
     * Montag bis Freitag: Aktuelle Woche (Mo..So).
     * Am Wochenende (Sa/So): Orientiert sich an der kommenden Woche (Mo..So).
     */
    fun getWeekRange(): Pair<SimpleDate, SimpleDate> {
        val effective = when (dayOfWeek) {
            6 -> plusDays(2) // Samstag -> kommender Montag
            0 -> plusDays(1) // Sonntag -> kommender Montag
            else -> this
        }
        val daysSinceMonday = effective.dayOfWeek - 1
        val monday = effective.minusDays(daysSinceMonday)
        val sunday = monday.plusDays(6)
        return monday to sunday
    }

    /**
     * Prüft, ob das gegebene ISO-Datum (YYYY-MM-DD) zur aktuellen Schulwoche gehört.
     * An Schultagen (Mo-Fr) werden nur Einträge der aktuellen Kalenderwoche akzeptiert.
     * Am Wochenende (Sa/So) werden sowohl die gerade endende als auch die kommende Woche akzeptiert,
     * aber niemals ältere Wochen (z. B. letzte Woche).
     */
    fun isDateInCurrentWeek(dateIso: String): Boolean {
        val trimmed = dateIso.trim()
        if (trimmed.length < 10) return false
        val (monday, sunday) = getWeekRange()
        val mondayIso = monday.toIsoString()
        val sundayIso = sunday.toIsoString()

        if (trimmed in mondayIso..sundayIso) return true

        // Am Wochenende (Sa/So) darf zusätzlich auch noch der Freitag der gerade beendeten Woche eingesehen werden
        if (dayOfWeek in listOf(0, 6)) {
            val daysSinceMonday = if (dayOfWeek == 0) 6 else 5
            val lastMonday = minusDays(daysSinceMonday)
            val lastSunday = lastMonday.plusDays(6)
            if (trimmed in lastMonday.toIsoString()..lastSunday.toIsoString()) {
                return true
            }
        }
        return false
    }
}

data class SimpleTime(
    val hour: Int,
    val minute: Int
)

expect fun getTodayDate(): SimpleDate
expect fun getCurrentTime(): SimpleTime
expect fun getCurrentEpochMillis(): Long
expect fun parseDateTimeToEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long


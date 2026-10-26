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
}

expect fun getTodayDate(): SimpleDate
expect fun getCurrentEpochMillis(): Long

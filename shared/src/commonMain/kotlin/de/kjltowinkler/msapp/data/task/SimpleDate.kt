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
}

expect fun getTodayDate(): SimpleDate
expect fun getCurrentEpochMillis(): Long

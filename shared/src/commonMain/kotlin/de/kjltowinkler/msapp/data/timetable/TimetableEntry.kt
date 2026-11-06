package de.kjltowinkler.msapp.data.timetable

import kotlinx.serialization.Serializable

/**
 * Repräsentiert eine Unterrichtsstunde im Stundenplan.
 */
@Serializable
data class TimetableLesson(
    val id: String,
    val periodNumber: Int,
    val timeRange: String,
    val subject: String,
    val room: String,
    val teacherCode: String,
    val teacherName: String = "",
    val teacherEmail: String = "",
    val teacherOfficeHours: String = "",
    val breakAfterMinutes: Int? = null,
    val breakLabel: String? = null,
    val note: String? = null
)

/**
 * Wochentage für den Stundenplan (Montag bis Freitag).
 */
@Serializable
enum class TimetableDay(val id: String, val displayName: String, val shortName: String) {
    MONTAG("mon", "Montag", "Mo"),
    DIENSTAG("tue", "Dienstag", "Di"),
    MITTWOCH("wed", "Mittwoch", "Mi"),
    DONNERSTAG("thu", "Donnerstag", "Do"),
    FREITAG("fri", "Freitag", "Fr")
}

/**
 * Ermittelt den Tag, der standardmäßig im Stundenplan geöffnet werden soll:
 * - Aktueller Schultag (Montag - Freitag), solange der Unterricht noch läuft.
 * - Ist der Schultag vorbei (nach der letzten Stunde, standardmäßig ab 13:05 Uhr, freitags ab 12:15 Uhr),
 *   wird automatisch der nächste Schultag geöffnet.
 * - Freitags nach Schulschluss oder am Wochenende (Samstag/Sonntag) wird direkt Montag geöffnet.
 */
fun getInitialTimetableDay(
    today: de.kjltowinkler.msapp.data.task.SimpleDate = de.kjltowinkler.msapp.data.task.getTodayDate(),
    now: de.kjltowinkler.msapp.data.task.SimpleTime = de.kjltowinkler.msapp.data.task.getCurrentTime(),
    getLessonsForDay: (TimetableDay) -> List<TimetableLesson> = { TimetableRepository.getLessonsForDay(it) }
): TimetableDay {
    val currentMinutes = now.hour * 60 + now.minute

    // dayOfWeek: 0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday
    return when (today.dayOfWeek) {
        1 -> { // Montag
            if (isDayFinished(TimetableDay.MONTAG, currentMinutes, getLessonsForDay, fallbackEndMinutes = 13 * 60 + 5)) {
                TimetableDay.DIENSTAG
            } else {
                TimetableDay.MONTAG
            }
        }
        2 -> { // Dienstag
            if (isDayFinished(TimetableDay.DIENSTAG, currentMinutes, getLessonsForDay, fallbackEndMinutes = 13 * 60 + 5)) {
                TimetableDay.MITTWOCH
            } else {
                TimetableDay.DIENSTAG
            }
        }
        3 -> { // Mittwoch
            if (isDayFinished(TimetableDay.MITTWOCH, currentMinutes, getLessonsForDay, fallbackEndMinutes = 13 * 60 + 5)) {
                TimetableDay.DONNERSTAG
            } else {
                TimetableDay.MITTWOCH
            }
        }
        4 -> { // Donnerstag
            if (isDayFinished(TimetableDay.DONNERSTAG, currentMinutes, getLessonsForDay, fallbackEndMinutes = 13 * 60 + 5)) {
                TimetableDay.FREITAG
            } else {
                TimetableDay.DONNERSTAG
            }
        }
        5 -> { // Freitag
            if (isDayFinished(TimetableDay.FREITAG, currentMinutes, getLessonsForDay, fallbackEndMinutes = 12 * 60 + 15)) {
                TimetableDay.MONTAG
            } else {
                TimetableDay.FREITAG
            }
        }
        6 -> TimetableDay.MONTAG // Samstag -> Montag
        0 -> TimetableDay.MONTAG // Sonntag -> Montag
        else -> TimetableDay.MONTAG
    }
}

private fun isDayFinished(
    day: TimetableDay,
    currentMinutes: Int,
    getLessonsForDay: (TimetableDay) -> List<TimetableLesson>,
    fallbackEndMinutes: Int
): Boolean {
    val lessons = getLessonsForDay(day)
    val endMinutes = lessons.lastOrNull()?.let { lastLesson ->
        parseEndMinutesFromTimeRange(lastLesson.timeRange)
    } ?: fallbackEndMinutes

    return currentMinutes >= endMinutes
}

private fun parseEndMinutesFromTimeRange(timeRange: String): Int? {
    val parts = timeRange.split("-")
    val endPart = parts.lastOrNull()?.trim() ?: return null
    val timeSegments = endPart.split(":")
    if (timeSegments.size >= 2) {
        val h = timeSegments[0].trim().toIntOrNull() ?: return null
        val m = timeSegments[1].trim().take(2).toIntOrNull() ?: return null
        return h * 60 + m
    }
    return null
}

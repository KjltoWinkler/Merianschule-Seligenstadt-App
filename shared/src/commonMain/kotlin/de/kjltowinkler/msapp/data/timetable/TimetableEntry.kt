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

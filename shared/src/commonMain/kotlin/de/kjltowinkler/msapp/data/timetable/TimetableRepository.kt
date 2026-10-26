package de.kjltowinkler.msapp.data.timetable

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Repository zur Bereitstellung der Stundenplandaten.
 * Bildet das Design aus FigmaDesigns/mobile/stundenplan#montag.png 1:1 nach.
 */
object TimetableRepository {

    // Lehrer-Verzeichnis für Detailinformationen bei Klick auf das Lehrer-Kürzel
    val teacherDirectory: Map<String, TeacherInfo> = mapOf(
        "Af" to TeacherInfo(
            code = "Af",
            fullName = "Fr. Affeldt",
            email = "affeldt@merianschule-seligenstadt.de",
            subjects = listOf("Deutsch", "Ethik"),
            officeHours = "Mo. 11:15 - 11:45 Uhr (nach Vereinbarung)"
        ),
        "Ky" to TeacherInfo(
            code = "Ky",
            fullName = "Hr. Kaya",
            email = "kaya@merianschule-seligenstadt.de",
            subjects = listOf("Mathematik", "Informatik"),
            officeHours = "Di. 09:15 - 09:45 Uhr"
        ),
        "Tho / Za" to TeacherInfo(
            code = "Tho / Za",
            fullName = "Hr. Thomas / Fr. Zander",
            email = "thomas@merianschule-seligenstadt.de",
            subjects = listOf("Geschichte", "Politik und Wirtschaft"),
            officeHours = "Mo. 13:10 - 13:40 Uhr"
        ),
        "Des" to TeacherInfo(
            code = "Des",
            fullName = "Fr. Desch",
            email = "desch@merianschule-seligenstadt.de",
            subjects = listOf("Englisch", "Spanisch"),
            officeHours = "Mi. 10:20 - 11:00 Uhr"
        ),
        "Me" to TeacherInfo(
            code = "Me",
            fullName = "Fr. Meyer",
            email = "meyer@merianschule-seligenstadt.de",
            subjects = listOf("Biologie", "Chemie"),
            officeHours = "Do. 08:25 - 09:00 Uhr"
        ),
        "Sn" to TeacherInfo(
            code = "Sn",
            fullName = "Hr. Schneider",
            email = "schneider@merianschule-seligenstadt.de",
            subjects = listOf("Physik", "Mathematik"),
            officeHours = "Fr. 12:15 - 13:00 Uhr"
        ),
        "Wb" to TeacherInfo(
            code = "Wb",
            fullName = "Hr. Weber",
            email = "weber@merianschule-seligenstadt.de",
            subjects = listOf("Sport", "Erdkunde"),
            officeHours = "Di. 13:10 - 13:50 Uhr"
        ),
        "Kr" to TeacherInfo(
            code = "Kr",
            fullName = "Fr. Krause",
            email = "krause@merianschule-seligenstadt.de",
            subjects = listOf("Kunst", "Musik"),
            officeHours = "Fr. 10:20 - 11:00 Uhr"
        )
    )

    // Standard-Stundenplan für Montag entsprechend FigmaDesigns/mobile/stundenplan#montag.png
    private val mondayLessons = listOf(
        TimetableLesson(
            id = "mon_1",
            periodNumber = 1,
            timeRange = "07:40 - 08:25",
            subject = "Deutsch",
            room = "Raum B208",
            teacherCode = "Af",
            teacherName = "Fr. Affeldt",
            teacherEmail = "affeldt@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "mon_2",
            periodNumber = 2,
            timeRange = "08:30 - 09:15",
            subject = "Mathematik",
            room = "Raum B208",
            teacherCode = "Ky",
            teacherName = "Hr. Kaya",
            teacherEmail = "kaya@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "mon_3",
            periodNumber = 3,
            timeRange = "09:35 - 10:20",
            subject = "Geschichte",
            room = "Raum B208",
            teacherCode = "Tho / Za",
            teacherName = "Hr. Thomas / Fr. Zander",
            teacherEmail = "thomas@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "mon_4",
            periodNumber = 4,
            timeRange = "10:25 - 11:10",
            subject = "Englisch",
            room = "Raum B208",
            teacherCode = "Des",
            teacherName = "Fr. Desch",
            teacherEmail = "desch@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "mon_5",
            periodNumber = 5,
            timeRange = "11:30 - 12:15",
            subject = "Biologie",
            room = "Raum BIO1",
            teacherCode = "Me",
            teacherName = "Fr. Meyer",
            teacherEmail = "meyer@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "mon_6",
            periodNumber = 6,
            timeRange = "12:20 - 13:05",
            subject = "Physik",
            room = "Raum PH1",
            teacherCode = "Sn",
            teacherName = "Hr. Schneider",
            teacherEmail = "schneider@merianschule-seligenstadt.de"
        )
    )

    // Dienstag
    private val tuesdayLessons = listOf(
        TimetableLesson(
            id = "tue_1",
            periodNumber = 1,
            timeRange = "07:40 - 08:25",
            subject = "Mathematik",
            room = "Raum B208",
            teacherCode = "Ky",
            teacherName = "Hr. Kaya",
            teacherEmail = "kaya@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "tue_2",
            periodNumber = 2,
            timeRange = "08:30 - 09:15",
            subject = "Deutsch",
            room = "Raum B208",
            teacherCode = "Af",
            teacherName = "Fr. Affeldt",
            teacherEmail = "affeldt@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "tue_3",
            periodNumber = 3,
            timeRange = "09:35 - 10:20",
            subject = "Englisch",
            room = "Raum B208",
            teacherCode = "Des",
            teacherName = "Fr. Desch",
            teacherEmail = "desch@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "tue_4",
            periodNumber = 4,
            timeRange = "10:25 - 11:10",
            subject = "Sport",
            room = "Sporthalle B",
            teacherCode = "Wb",
            teacherName = "Hr. Weber",
            teacherEmail = "weber@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "tue_5",
            periodNumber = 5,
            timeRange = "11:30 - 12:15",
            subject = "Sport",
            room = "Sporthalle B",
            teacherCode = "Wb",
            teacherName = "Hr. Weber",
            teacherEmail = "weber@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "tue_6",
            periodNumber = 6,
            timeRange = "12:20 - 13:05",
            subject = "PoWi",
            room = "Raum B208",
            teacherCode = "Tho / Za",
            teacherName = "Hr. Thomas / Fr. Zander",
            teacherEmail = "thomas@merianschule-seligenstadt.de"
        )
    )

    // Mittwoch
    private val wednesdayLessons = listOf(
        TimetableLesson(
            id = "wed_1",
            periodNumber = 1,
            timeRange = "07:40 - 08:25",
            subject = "Englisch",
            room = "Raum B208",
            teacherCode = "Des",
            teacherName = "Fr. Desch",
            teacherEmail = "desch@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "wed_2",
            periodNumber = 2,
            timeRange = "08:30 - 09:15",
            subject = "Mathematik",
            room = "Raum B208",
            teacherCode = "Ky",
            teacherName = "Hr. Kaya",
            teacherEmail = "kaya@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "wed_3",
            periodNumber = 3,
            timeRange = "09:35 - 10:20",
            subject = "Physik",
            room = "Raum PH1",
            teacherCode = "Sn",
            teacherName = "Hr. Schneider",
            teacherEmail = "schneider@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "wed_4",
            periodNumber = 4,
            timeRange = "10:25 - 11:10",
            subject = "Deutsch",
            room = "Raum B208",
            teacherCode = "Af",
            teacherName = "Fr. Affeldt",
            teacherEmail = "affeldt@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "wed_5",
            periodNumber = 5,
            timeRange = "11:30 - 12:15",
            subject = "Kunst",
            room = "Kunstraum K1",
            teacherCode = "Kr",
            teacherName = "Fr. Krause",
            teacherEmail = "krause@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "wed_6",
            periodNumber = 6,
            timeRange = "12:20 - 13:05",
            subject = "Kunst",
            room = "Kunstraum K1",
            teacherCode = "Kr",
            teacherName = "Fr. Krause",
            teacherEmail = "krause@merianschule-seligenstadt.de"
        )
    )

    // Donnerstag
    private val thursdayLessons = listOf(
        TimetableLesson(
            id = "thu_1",
            periodNumber = 1,
            timeRange = "07:40 - 08:25",
            subject = "Deutsch",
            room = "Raum B208",
            teacherCode = "Af",
            teacherName = "Fr. Affeldt",
            teacherEmail = "affeldt@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "thu_2",
            periodNumber = 2,
            timeRange = "08:30 - 09:15",
            subject = "Geschichte",
            room = "Raum B208",
            teacherCode = "Tho / Za",
            teacherName = "Hr. Thomas / Fr. Zander",
            teacherEmail = "thomas@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "thu_3",
            periodNumber = 3,
            timeRange = "09:35 - 10:20",
            subject = "Mathematik",
            room = "Raum B208",
            teacherCode = "Ky",
            teacherName = "Hr. Kaya",
            teacherEmail = "kaya@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "thu_4",
            periodNumber = 4,
            timeRange = "10:25 - 11:10",
            subject = "Englisch",
            room = "Raum B208",
            teacherCode = "Des",
            teacherName = "Fr. Desch",
            teacherEmail = "desch@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "thu_5",
            periodNumber = 5,
            timeRange = "11:30 - 12:15",
            subject = "Biologie",
            room = "Raum BIO1",
            teacherCode = "Me",
            teacherName = "Fr. Meyer",
            teacherEmail = "meyer@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "thu_6",
            periodNumber = 6,
            timeRange = "12:20 - 13:05",
            subject = "Chemie",
            room = "Raum CH1",
            teacherCode = "Me",
            teacherName = "Fr. Meyer",
            teacherEmail = "meyer@merianschule-seligenstadt.de"
        )
    )

    // Freitag
    private val fridayLessons = listOf(
        TimetableLesson(
            id = "fri_1",
            periodNumber = 1,
            timeRange = "07:40 - 08:25",
            subject = "Mathematik",
            room = "Raum B208",
            teacherCode = "Ky",
            teacherName = "Hr. Kaya",
            teacherEmail = "kaya@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "fri_2",
            periodNumber = 2,
            timeRange = "08:30 - 09:15",
            subject = "Englisch",
            room = "Raum B208",
            teacherCode = "Des",
            teacherName = "Fr. Desch",
            teacherEmail = "desch@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "fri_3",
            periodNumber = 3,
            timeRange = "09:35 - 10:20",
            subject = "Deutsch",
            room = "Raum B208",
            teacherCode = "Af",
            teacherName = "Fr. Affeldt",
            teacherEmail = "affeldt@merianschule-seligenstadt.de",
            breakAfterMinutes = 5,
            breakLabel = "5 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "fri_4",
            periodNumber = 4,
            timeRange = "10:25 - 11:10",
            subject = "Musik",
            room = "Musikraum M1",
            teacherCode = "Kr",
            teacherName = "Fr. Krause",
            teacherEmail = "krause@merianschule-seligenstadt.de",
            breakAfterMinutes = 20,
            breakLabel = "20 Min. Wechselpause"
        ),
        TimetableLesson(
            id = "fri_5",
            periodNumber = 5,
            timeRange = "11:30 - 12:15",
            subject = "Erdkunde",
            room = "Raum B208",
            teacherCode = "Wb",
            teacherName = "Hr. Weber",
            teacherEmail = "weber@merianschule-seligenstadt.de"
        )
    )

    // Reaktiver Speicher für die Wochentage (ermöglicht Hinzufügen/Bearbeiten über den FAB)
    private val timetableMap = mutableStateMapOf<TimetableDay, List<TimetableLesson>>().apply {
        put(TimetableDay.MONTAG, mondayLessons)
        put(TimetableDay.DIENSTAG, tuesdayLessons)
        put(TimetableDay.MITTWOCH, wednesdayLessons)
        put(TimetableDay.DONNERSTAG, thursdayLessons)
        put(TimetableDay.FREITAG, fridayLessons)
    }

    /**
     * Gibt die Stunden für den übergebenen Wochentag zurück.
     */
    fun getLessonsForDay(day: TimetableDay): List<TimetableLesson> {
        return timetableMap[day] ?: emptyList()
    }

    /**
     * Fügt eine neue Stunde für den Wochentag hinzu.
     */
    fun addLesson(day: TimetableDay, lesson: TimetableLesson) {
        val current = timetableMap[day]?.toMutableList() ?: mutableListOf()
        current.add(lesson)
        current.sortBy { it.periodNumber }
        timetableMap[day] = current
    }

    /**
     * Sucht TeacherInfo anhand des Kürzels.
     */
    fun getTeacherInfo(code: String): TeacherInfo {
        return teacherDirectory[code] ?: TeacherInfo(
            code = code,
            fullName = "Lehrkraft ($code)",
            email = "${code.lowercase().replace(" ", "").replace("/", ".")}@merianschule-seligenstadt.de",
            subjects = listOf("Fachunterricht"),
            officeHours = "Nach Vereinbarung"
        )
    }
}

/**
 * Details zu einer Lehrkraft.
 */
@kotlinx.serialization.Serializable
data class TeacherInfo(
    val code: String,
    val fullName: String,
    val email: String,
    val subjects: List<String>,
    val officeHours: String
)

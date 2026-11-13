package de.kjltowinkler.msapp.data.timetable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository zur Bereitstellung der Stundenplandaten.
 * Bildet das Design aus FigmaDesigns/mobile/stundenplan#montag.png 1:1 nach.
 */
object TimetableRepository {

    // Lehrer-Verzeichnis für Detailinformationen bei Klick auf das Lehrer-Kürzel
    val teacherDirectory: Map<String, TeacherInfo> = mapOf(
        "Rd" to TeacherInfo(
            code = "Rd",
            fullName = "Julia Ridel",
            email = "Julia.Ridel@mssof.onmicrosoft.com",
            subjects = listOf("Englisch"),
            officeHours = "Nach Vereinbarung"
        ),
        "Reu" to TeacherInfo(
            code = "Reu",
            fullName = "Alvin Bernhard Perry Reuter",
            email = "Alvin.Reuter@mssof.onmicrosoft.com",
            subjects = listOf("Deutsch", "Englisch"),
            officeHours = "Nach Vereinbarung"
        ),
        "Tho" to TeacherInfo(
            code = "Tho",
            fullName = "Daniel Thomas",
            email = "Daniel.Thomas@mssof.onmicrosoft.com",
            subjects = listOf("Biologie", "Chemie", "Geschichte"),
            officeHours = "Mo. 13:10 - 13:40 Uhr"
        ),
        "Kne" to TeacherInfo(
            code = "Kne",
            fullName = "Gerrit Jens Knebel",
            email = "Gerrit.Knebel@mssof.onmicrosoft.com",
            subjects = listOf("Mathematik"),
            officeHours = "Nach Vereinbarung"
        ),
        "Car" to TeacherInfo(
            code = "Car",
            fullName = "Rui Carvalho",
            email = "rui.carvalho@mssof.onmicrosoft.com",
            subjects = listOf("Ethik"),
            officeHours = "Nach Vereinbarung"
        ),
        "Af" to TeacherInfo(
            code = "Af",
            fullName = "Kati Afrasiabi",
            email = "Kati.Afrasiabi@mssof.onmicrosoft.com",
            subjects = listOf("Deutsch", "Ethik"),
            officeHours = "Mo. 11:15 - 11:45 Uhr (nach Vereinbarung)"
        ),
        "Za" to TeacherInfo(
            code = "Za",
            fullName = "Eleni Zania",
            email = "eleni.zania@mssof.onmicrosoft.com",
            subjects = listOf("Geschichte", "Politik und Wirtschaft"),
            officeHours = "Nach Vereinbarung"
        ),
        "Tho / Za" to TeacherInfo(
            code = "Tho / Za",
            fullName = "Daniel Thomas / Eleni Zania",
            email = "Daniel.Thomas@mssof.onmicrosoft.com",
            subjects = listOf("Geschichte", "Politik und Wirtschaft"),
            officeHours = "Mo. 13:10 - 13:40 Uhr"
        ),
        "Hol" to TeacherInfo(
            code = "Hol",
            fullName = "Iris Holstein",
            email = "Iris.Holstein@mssof.onmicrosoft.com",
            subjects = listOf("Französisch"),
            officeHours = "Nach Vereinbarung"
        ),
        "Hv" to TeacherInfo(
            code = "Hv",
            fullName = "Tibor Horvath-Walther",
            email = "Tibor.Horvath-Walther@mssof.onmicrosoft.com",
            subjects = listOf("Politik und Wirtschaft"),
            officeHours = "Nach Vereinbarung"
        ),
        "Bü" to TeacherInfo(
            code = "Bü",
            fullName = "Dominique Bürkle",
            email = "dominique.buerkle@mssof.onmicrosoft.com",
            subjects = listOf("Kunst"),
            officeHours = "Nach Vereinbarung"
        ),
        "Ky" to TeacherInfo(
            code = "Ky",
            fullName = "Hr. Kaya",
            email = "kaya@merianschule-seligenstadt.de",
            subjects = listOf("Mathematik", "Informatik"),
            officeHours = "Di. 09:15 - 09:45 Uhr"
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
        "Web" to TeacherInfo(
            code = "Web",
            fullName = "Hr. Weber",
            email = "weber@merianschule-seligenstadt.de",
            subjects = listOf("Sport", "Erdkunde"),
            officeHours = "Di. 13:10 - 13:50 Uhr"
        ),
        "Wb" to TeacherInfo(
            code = "Web",
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

    // Zuordnung von Lehrer-Kürzeln und Nachnamen zur gewünschten Anrede "Fr. / Hr. Nachname"
    val teacherSalutations: Map<String, String> = mapOf(
        "Rd" to "Fr. Ridel",
        "Reu" to "Hr. Reuter",
        "Tho" to "Hr. Thomas",
        "Kne" to "Hr. Knebel",
        "Car" to "Hr. Carvalho",
        "Af" to "Fr. Afrasiabi",
        "Za" to "Fr. Zania",
        "Hol" to "Fr. Holstein",
        "Hv" to "Hr. Horvath-Walther",
        "Bü" to "Fr. Bürkle",
        "Ky" to "Hr. Kaya",
        "Des" to "Fr. Desch",
        "Me" to "Fr. Meyer",
        "Sn" to "Hr. Schneider",
        "Web" to "Hr. Weber",
        "Wb" to "Hr. Weber",
        "Kr" to "Fr. Krause",
        // Zusätzliche Nachnamen-Aliase
        "Ridel" to "Fr. Ridel",
        "Riedel" to "Fr. Ridel",
        "Reuter" to "Hr. Reuter",
        "Thomas" to "Hr. Thomas",
        "Knebel" to "Hr. Knebel",
        "Carvalho" to "Hr. Carvalho",
        "Afrasiabi" to "Fr. Afrasiabi",
        "Affeldt" to "Fr. Afrasiabi",
        "Zania" to "Fr. Zania",
        "Zander" to "Fr. Zania",
        "Holstein" to "Fr. Holstein",
        "Horvath-Walther" to "Hr. Horvath-Walther",
        "Horvath" to "Hr. Horvath-Walther",
        "Bürkle" to "Fr. Bürkle",
        "Kaya" to "Hr. Kaya",
        "Desch" to "Fr. Desch",
        "Meyer" to "Fr. Meyer",
        "Schneider" to "Hr. Schneider",
        "Weber" to "Hr. Weber",
        "Krause" to "Fr. Krause"
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
            teacherName = "Fr. Afrasiabi",
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
            teacherName = "Hr. Thomas / Fr. Zania",
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
            teacherName = "Fr. Afrasiabi",
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
            teacherCode = "Web",
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
            teacherCode = "Web",
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
            teacherName = "Hr. Thomas / Fr. Zania",
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
            teacherName = "Fr. Afrasiabi",
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
            teacherName = "Fr. Afrasiabi",
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
            teacherName = "Hr. Thomas / Fr. Zania",
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
            teacherName = "Fr. Afrasiabi",
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
            teacherCode = "Web",
            teacherName = "Hr. Weber",
            teacherEmail = "weber@merianschule-seligenstadt.de"
        )
    )

    private const val KEY_TIMETABLE_CACHE = "cached_timetable_dto"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    var isLoaded: Boolean by mutableStateOf(false)
        private set

    // Reaktiver Speicher für die Wochentage (ermöglicht Hinzufügen/Bearbeiten über den FAB)
    private val timetableMap = mutableStateMapOf<TimetableDay, List<TimetableLesson>>()

    init {
        loadFromCache()
    }

    /**
     * Liefert die statischen Fallback-Stunden (Designentwurf), falls kein Cache und kein Server verfügbar ist.
     */
    fun getDefaultLessonsForDay(day: TimetableDay): List<TimetableLesson> {
        return when (day) {
            TimetableDay.MONTAG -> mondayLessons
            TimetableDay.DIENSTAG -> tuesdayLessons
            TimetableDay.MITTWOCH -> wednesdayLessons
            TimetableDay.DONNERSTAG -> thursdayLessons
            TimetableDay.FREITAG -> fridayLessons
        }
    }

    /**
     * Wendet die statischen Fallback-Stunden an (nur als letzter Notfall-Fallback).
     */
    fun applyDefaultMockLessons() {
        TimetableDay.entries.forEach { day ->
            timetableMap[day] = getDefaultLessonsForDay(day)
        }
        isLoaded = true
    }

    // Zuordnung von Fach-Kürzeln zu den vollständigen Fachbezeichnungen
    val subjectDictionary: Map<String, String> = mapOf(
        "D" to "Deutsch",
        "DEU" to "Deutsch",
        "DEUTSCH" to "Deutsch",
        "E" to "Englisch",
        "ENG" to "Englisch",
        "ENGLISCH" to "Englisch",
        "M" to "Mathematik",
        "MAT" to "Mathematik",
        "MATHE" to "Mathematik",
        "MATHEMATIK" to "Mathematik",
        "B" to "Biologie",
        "BIO" to "Biologie",
        "BIOLOGIE" to "Biologie",
        "CH" to "Chemie",
        "CHE" to "Chemie",
        "CHEMIE" to "Chemie",
        "PH" to "Physik",
        "PHY" to "Physik",
        "PHYSIK" to "Physik",
        "G" to "Geschichte",
        "GE" to "Geschichte",
        "GESCH" to "Geschichte",
        "GESCHICHTE" to "Geschichte",
        "POWI" to "Politik & Wirtschaft",
        "PW" to "Politik & Wirtschaft",
        "POLITIK & WIRTSCHAFT" to "Politik & Wirtschaft",
        "POLITIK UND WIRTSCHAFT" to "Politik & Wirtschaft",
        "ETH" to "Ethik",
        "ETHIK" to "Ethik",
        "REL" to "Religion",
        "EV" to "Ev. Religion",
        "EVR" to "Ev. Religion",
        "KR" to "Kath. Religion",
        "KATH" to "Kath. Religion",
        "RELIGION" to "Religion",
        "KU" to "Kunst",
        "KUNST" to "Kunst",
        "MU" to "Musik",
        "MUSIK" to "Musik",
        "SP" to "Sport",
        "SPO" to "Sport",
        "SPORT" to "Sport",
        "F" to "Französisch",
        "FR" to "Französisch",
        "FRZ" to "Französisch",
        "FRANZÖSISCH" to "Französisch",
        "L" to "Latein",
        "LAT" to "Latein",
        "LATEIN" to "Latein",
        "SPA" to "Spanisch",
        "SPN" to "Spanisch",
        "SPANISCH" to "Spanisch",
        "EK" to "Erdkunde",
        "GEO" to "Erdkunde",
        "ERD" to "Erdkunde",
        "ERDKUNDE" to "Erdkunde",
        "INF" to "Informatik",
        "INFORMATIK" to "Informatik",
        "WPU" to "WPU",
        "KL" to "Klassenstunde",
        "AL" to "Arbeitslehre",
        "ARBEITSLEHRE" to "Arbeitslehre",
        "DAZ" to "Deutsch als Zweitsprache"
    )

    /**
     * Löst Fachkürzel wie "E" oder "D" in vollständige deutsche Wörter auf ("Englisch", "Deutsch").
     */
    fun resolveSubjectName(rawSubject: String): String {
        val trimmed = rawSubject.trim()
        if (trimmed.isBlank()) return trimmed

        val upper = trimmed.uppercase()
        val direct = subjectDictionary[upper]
        if (direct != null) return direct

        val match = subjectDictionary.entries.firstOrNull {
            it.key.equals(trimmed, ignoreCase = true)
        }?.value
        if (match != null) return match

        return trimmed
    }

    fun getEffectiveUserClass(fallback: String = "09AR"): String {
        val rawUserClass = de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.className?.trim()
        val isStaff = rawUserClass.equals("Schulleitung", ignoreCase = true) || 
                      rawUserClass.equals("Lehrer", ignoreCase = true) || 
                      rawUserClass.equals("Kollegium", ignoreCase = true) ||
                      de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.role?.uppercase() in listOf("TEACHER", "ADMIN", "LEHRER")
        return if (isStaff || rawUserClass.isNullOrBlank()) fallback else rawUserClass
    }

    private fun getCacheKeyForClass(className: String): String {
        val clean = className.lowercase().replace(" ", "")
        return "cached_timetable_dto_$clean"
    }

    /**
     * Wendet ein geladenes Stundenplan-DTO auf den lokalen Speicher an.
     */
    fun applyTimetableDto(dto: ClassTimetableDto) {
        if (dto.days.isEmpty()) return
        TimetableDay.entries.forEach { dayEnum ->
            val dayLessons = dto.days.entries.firstOrNull { (dayKey, _) ->
                val trimmed = dayKey.trim()
                dayEnum.name.equals(trimmed, ignoreCase = true) ||
                dayEnum.id.equals(trimmed, ignoreCase = true) ||
                dayEnum.displayName.equals(trimmed, ignoreCase = true) ||
                dayEnum.shortName.equals(trimmed, ignoreCase = true)
            }?.value ?: emptyList()
            val normalizedLessons = dayLessons.map { lesson ->
                lesson.copy(subject = resolveSubjectName(lesson.subject))
            }
            timetableMap[dayEnum] = normalizedLessons
        }
        isLoaded = true
    }

    /**
     * Lädt den zuletzt gespeicherten Stundenplan aus dem lokalen Offline-Cache.
     */
    fun loadFromCache(targetClass: String? = null): Boolean {
        return try {
            val userClass = targetClass ?: getEffectiveUserClass()
            val classKey = getCacheKeyForClass(userClass)
            var cachedJson = PlatformStorage.getString(classKey)
            if (cachedJson.isNullOrBlank() && userClass.equals("09AR", ignoreCase = true)) {
                cachedJson = PlatformStorage.getString(KEY_TIMETABLE_CACHE)
            }
            if (!cachedJson.isNullOrBlank()) {
                val dto = json.decodeFromString<ClassTimetableDto>(cachedJson)
                if (dto.days.isNotEmpty()) {
                    applyTimetableDto(dto)
                    isLoaded = true
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
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
        current.add(lesson.copy(subject = resolveSubjectName(lesson.subject)))
        current.sortBy { it.periodNumber }
        timetableMap[day] = current
    }

    /**
     * Sucht TeacherInfo anhand des Kürzels.
     */
    fun getTeacherInfo(code: String): TeacherInfo {
        val trimmed = code.trim()
        val direct = teacherDirectory[trimmed]
        if (direct != null) return direct

        val match = teacherDirectory.entries.firstOrNull { 
            it.key.equals(trimmed, ignoreCase = true) 
        }?.value
        if (match != null) return match

        return TeacherInfo(
            code = trimmed,
            fullName = "Lehrkraft ($trimmed)",
            email = "${trimmed.lowercase().replace(" ", "").replace("/", ".")}@merianschule-seligenstadt.de",
            subjects = listOf("Fachunterricht"),
            officeHours = "Nach Vereinbarung"
        )
    }

    /**
     * Wandelt ein Lehrerkürzel oder einen Nachnamen in die gewünschte Anrede um (z. B. "Af" -> "Fr. Afrasiabi", "Web" -> "Hr. Weber").
     */
    fun getTeacherDisplayName(codeOrName: String): String {
        val trimmed = codeOrName.trim()
        if (trimmed.isBlank() || trimmed == "-" || trimmed == "–" ||
            trimmed.equals("Vertretung", ignoreCase = true) ||
            trimmed.equals("Unterricht entfällt", ignoreCase = true)
        ) {
            return trimmed
        }

        // Falls bereits Fr. / Hr. vorangestellt ist
        if (trimmed.startsWith("Fr. ") || trimmed.startsWith("Hr. ")) {
            return trimmed
        }
        if (trimmed.startsWith("Frau ")) {
            return "Fr. " + trimmed.removePrefix("Frau ").trim()
        }
        if (trimmed.startsWith("Herr ")) {
            return "Hr. " + trimmed.removePrefix("Herr ").trim()
        }

        // Wenn mehrere Lehrer mit '/' oder ',' getrennt sind
        if (trimmed.contains("/")) {
            return trimmed.split("/")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString(" / ") { getTeacherDisplayName(it) }
        }
        if (trimmed.contains(",")) {
            return trimmed.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString(", ") { getTeacherDisplayName(it) }
        }

        // Direkte Zuordnung über teacherSalutations (Kürzel oder Nachname)
        val directSalutation = teacherSalutations[trimmed]
            ?: teacherSalutations.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
        if (directSalutation != null) {
            return directSalutation
        }

        // Fallback: Prüfen im teacherDirectory
        val info = teacherDirectory[trimmed]
            ?: teacherDirectory.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
        if (info != null) {
            if (info.fullName.startsWith("Fr. ") || info.fullName.startsWith("Hr. ")) {
                return info.fullName
            }
            val lastName = info.fullName.substringAfterLast(" ")
            return lastName
        }

        return trimmed
    }

    /**
     * Formatiert den Vertretungs-Detailtext so, dass alle Lehrerkürzel durch "Fr. / Hr. Nachname" ersetzt werden.
     * Erkennt Muster wie "Sn statt Af", "Af entfällt ersatzlos", "Vertretung durch Sn" oder einzelne Kürzel.
     */
    fun formatDetailsWithTeacherNames(details: String): String {
        val trimmed = details.trim()
        if (trimmed.isBlank()) return trimmed

        // Fall 1: "X statt Y"
        if (trimmed.contains(" statt ")) {
            val parts = trimmed.split(" statt ", limit = 2)
            val v = getTeacherDisplayName(parts[0].trim())
            val l = getTeacherDisplayName(parts[1].trim())
            return "$v statt $l"
        }

        // Fall 2: "X entfällt ersatzlos"
        if (trimmed.endsWith(" entfällt ersatzlos")) {
            val raw = trimmed.removeSuffix(" entfällt ersatzlos").trim()
            val l = getTeacherDisplayName(raw)
            return "$l entfällt ersatzlos"
        }

        // Fall 3: "X entfällt"
        if (trimmed.endsWith(" entfällt") && !trimmed.equals("Unterricht entfällt", ignoreCase = true)) {
            val raw = trimmed.removeSuffix(" entfällt").trim()
            val l = getTeacherDisplayName(raw)
            return "$l entfällt"
        }

        // Fall 4: "Vertretung durch X"
        if (trimmed.startsWith("Vertretung durch ")) {
            val raw = trimmed.removePrefix("Vertretung durch ").trim()
            val v = getTeacherDisplayName(raw)
            return "Vertretung durch $v"
        }

        // Fall 5: Einfacher Lehrer-Code oder Name
        return getTeacherDisplayName(trimmed)
    }

    /**
     * Lädt den aktuellen Stundenplan vom Backend-Server ab.
     */
    suspend fun loadFromServer(className: String = "09AR"): Boolean {
        val userClass = getEffectiveUserClass(className)

        // Falls noch nicht geladen oder Speicher leer, versuche den Cache für diese Klasse
        if (!isLoaded || timetableMap.isEmpty()) {
            loadFromCache(userClass)
        }

        return try {
            var res = de.kjltowinkler.msapp.data.backend.BackendApiClient.fetchTimetable(userClass)
            var dto = res.getOrNull()

            // Falls die Klasse z. B. mit oder ohne führende 0 hinterlegt ist (09AR vs 9AR)
            if (dto == null || dto.days.isEmpty()) {
                val altClass = when {
                    userClass.startsWith("0") && userClass.length > 1 && userClass[1].isDigit() -> userClass.substring(1)
                    userClass.isNotEmpty() && userClass[0].isDigit() -> "0$userClass"
                    else -> userClass
                }
                if (altClass != userClass) {
                    val altRes = de.kjltowinkler.msapp.data.backend.BackendApiClient.fetchTimetable(altClass)
                    if (altRes.isSuccess && altRes.getOrNull()?.days?.isNotEmpty() == true) {
                        dto = altRes.getOrNull()
                    }
                }
            }

            if (dto != null && dto.days.isNotEmpty()) {
                applyTimetableDto(dto)
                try {
                    val classKey = getCacheKeyForClass(userClass)
                    val jsonStr = json.encodeToString(dto)
                    PlatformStorage.setString(classKey, jsonStr)
                    PlatformStorage.setString(KEY_TIMETABLE_CACHE, jsonStr)
                } catch (_: Exception) {}
                true
            } else {
                if (!isLoaded && timetableMap.isEmpty() && userClass.equals("09AR", ignoreCase = true)) {
                    applyDefaultMockLessons()
                }
                false
            }
        } catch (_: Exception) {
            if (!isLoaded && timetableMap.isEmpty() && userClass.equals("09AR", ignoreCase = true)) {
                applyDefaultMockLessons()
            }
            false
        }
    }

    /**
     * Ermittelt, ob und welcher Vertretungsplan-Eintrag für eine Unterrichtsstunde an einem bestimmten Tag vorliegt.
     */
    fun findMatchingSubstitution(
        day: TimetableDay,
        lesson: TimetableLesson,
        substitutions: List<de.kjltowinkler.msapp.data.substitution.SubstitutionEntry> = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.getInitialOrCachedEntries(),
        userClass: String = de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.className ?: "09AR"
    ): de.kjltowinkler.msapp.data.substitution.SubstitutionEntry? {
        if (substitutions.isEmpty()) return null

        val today = de.kjltowinkler.msapp.data.task.getTodayDate()
        val (monday, _) = today.getWeekRange()
        val dayOffset = when (day) {
            TimetableDay.MONTAG -> 0
            TimetableDay.DIENSTAG -> 1
            TimetableDay.MITTWOCH -> 2
            TimetableDay.DONNERSTAG -> 3
            TimetableDay.FREITAG -> 4
        }
        val dayIso = monday.plusDays(dayOffset).toIsoString()
        val targetDayOfWeek = day.ordinal + 1 // Montag = 1, Freitag = 5

        val relevant = substitutions.filter {
            de.kjltowinkler.msapp.data.courses.CourseRepository.isEntryRelevant(it, userClass)
        }
        val hasExactDateMatch = relevant.any { it.date == dayIso }

        val dayEntries = relevant.filter { entry ->
            if (hasExactDateMatch) {
                entry.date == dayIso
            } else {
                val parsed = de.kjltowinkler.msapp.data.task.parseIsoDate(entry.date)
                parsed?.dayOfWeek == targetDayOfWeek || (entry.date.isBlank() && day == TimetableDay.MONTAG)
            }
        }
        if (dayEntries.isEmpty()) return null

        fun matchesPeriod(periodStr: String, periodNumber: Int): Boolean {
            val range = Regex("""(\d+)\s*[-/]\s*(\d+)""").find(periodStr)
            if (range != null) {
                val start = range.groupValues[1].toIntOrNull() ?: -1
                val end = range.groupValues[2].toIntOrNull() ?: -1
                if (periodNumber in start..end) return true
            }
            val numbers = Regex("""\b\d+\b""").findAll(periodStr).mapNotNull { it.value.toIntOrNull() }.toList()
            return numbers.contains(periodNumber)
        }

        val resolvedLessonSubject = resolveSubjectName(lesson.subject)

        // 1. Priorisierte Suche: Übereinstimmung von Stunde UND Fach / Lehrkraft
        val exactMatch = dayEntries.firstOrNull { entry ->
            if (!matchesPeriod(entry.period, lesson.periodNumber)) return@firstOrNull false
            if (entry.subject.isNotBlank()) {
                val resolvedEntrySubject = resolveSubjectName(entry.subject)
                resolvedLessonSubject.equals(resolvedEntrySubject, ignoreCase = true) ||
                    resolvedLessonSubject.contains(resolvedEntrySubject, ignoreCase = true) ||
                    resolvedEntrySubject.contains(resolvedLessonSubject, ignoreCase = true)
            } else {
                entry.details.contains(lesson.teacherCode, ignoreCase = true) ||
                    (lesson.teacherName.isNotBlank() && entry.details.contains(lesson.teacherName, ignoreCase = true))
            }
        }
        if (exactMatch != null) return exactMatch

        // 2. Fallback: Übereinstimmung der Stunde in der relevanten Klasse
        return dayEntries.firstOrNull { entry ->
            matchesPeriod(entry.period, lesson.periodNumber)
        }
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

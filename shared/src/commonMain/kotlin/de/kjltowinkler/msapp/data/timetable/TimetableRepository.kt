package de.kjltowinkler.msapp.data.timetable

import androidx.compose.runtime.mutableStateMapOf
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
            email = "j.ridel@merianschule-seligenstadt.de",
            subjects = listOf("Englisch"),
            officeHours = "Nach Vereinbarung"
        ),
        "Reu" to TeacherInfo(
            code = "Reu",
            fullName = "Alvin Bernhard Perry Reuter",
            email = "a.reuter@merianschule-seligenstadt.de",
            subjects = listOf("Deutsch", "Englisch"),
            officeHours = "Nach Vereinbarung"
        ),
        "Tho" to TeacherInfo(
            code = "Tho",
            fullName = "Daniel Thomas",
            email = "d.thomas@merianschule-seligenstadt.de",
            subjects = listOf("Biologie", "Chemie", "Geschichte"),
            officeHours = "Mo. 13:10 - 13:40 Uhr"
        ),
        "Kne" to TeacherInfo(
            code = "Kne",
            fullName = "Gerrit Jens Knebel",
            email = "g.knebel@merianschule-seligenstadt.de",
            subjects = listOf("Mathematik"),
            officeHours = "Nach Vereinbarung"
        ),
        "Car" to TeacherInfo(
            code = "Car",
            fullName = "Rui Carvalho",
            email = "r.carvalho@merianschule-seligenstadt.de",
            subjects = listOf("Ethik"),
            officeHours = "Nach Vereinbarung"
        ),
        "Af" to TeacherInfo(
            code = "Af",
            fullName = "Kati Afrasiabi",
            email = "k.afrasiabi@merianschule-seligenstadt.de",
            subjects = listOf("Deutsch", "Ethik"),
            officeHours = "Mo. 11:15 - 11:45 Uhr (nach Vereinbarung)"
        ),
        "Za" to TeacherInfo(
            code = "Za",
            fullName = "Eleni Zania",
            email = "e.zania@merianschule-seligenstadt.de",
            subjects = listOf("Geschichte", "Politik und Wirtschaft"),
            officeHours = "Nach Vereinbarung"
        ),
        "Tho / Za" to TeacherInfo(
            code = "Tho / Za",
            fullName = "Daniel Thomas / Eleni Zania",
            email = "d.thomas@merianschule-seligenstadt.de",
            subjects = listOf("Geschichte", "Politik und Wirtschaft"),
            officeHours = "Mo. 13:10 - 13:40 Uhr"
        ),
        "Hol" to TeacherInfo(
            code = "Hol",
            fullName = "Iris Holstein",
            email = "i.holstein@merianschule-seligenstadt.de",
            subjects = listOf("Französisch"),
            officeHours = "Nach Vereinbarung"
        ),
        "Hv" to TeacherInfo(
            code = "Hv",
            fullName = "Tibor Horvath-Walther",
            email = "t.horvath-walther@merianschule-seligenstadt.de",
            subjects = listOf("Politik und Wirtschaft"),
            officeHours = "Nach Vereinbarung"
        ),
        "Bü" to TeacherInfo(
            code = "Bü",
            fullName = "Dominique Bürkle",
            email = "d.buerkle@merianschule-seligenstadt.de",
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

    private const val KEY_TIMETABLE_CACHE = "cached_timetable_dto"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Reaktiver Speicher für die Wochentage (ermöglicht Hinzufügen/Bearbeiten über den FAB)
    private val timetableMap = mutableStateMapOf<TimetableDay, List<TimetableLesson>>().apply {
        put(TimetableDay.MONTAG, mondayLessons)
        put(TimetableDay.DIENSTAG, tuesdayLessons)
        put(TimetableDay.MITTWOCH, wednesdayLessons)
        put(TimetableDay.DONNERSTAG, thursdayLessons)
        put(TimetableDay.FREITAG, fridayLessons)
    }

    init {
        loadFromCache()
    }

    /**
     * Wendet ein geladenes Stundenplan-DTO auf den lokalen Speicher an.
     */
    fun applyTimetableDto(dto: ClassTimetableDto) {
        if (dto.days.isEmpty()) return
        TimetableDay.entries.forEach { dayEnum ->
            val dayLessons = dto.days.entries.firstOrNull { (dayKey, _) ->
                dayEnum.name.equals(dayKey, ignoreCase = true) ||
                dayEnum.id.equals(dayKey, ignoreCase = true) ||
                dayEnum.displayName.equals(dayKey, ignoreCase = true) ||
                dayEnum.shortName.equals(dayKey, ignoreCase = true)
            }?.value ?: emptyList()
            timetableMap[dayEnum] = dayLessons
        }
    }

    /**
     * Lädt den zuletzt gespeicherten Stundenplan aus dem lokalen Offline-Cache.
     */
    fun loadFromCache() {
        try {
            val cachedJson = PlatformStorage.getString(KEY_TIMETABLE_CACHE)
            if (!cachedJson.isNullOrBlank()) {
                val dto = json.decodeFromString<ClassTimetableDto>(cachedJson)
                applyTimetableDto(dto)
            }
        } catch (_: Exception) {}
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
     * Lädt den aktuellen Stundenplan vom Backend-Server ab.
     */
    suspend fun loadFromServer(className: String = "09AR"): Boolean {
        return try {
            val rawUserClass = de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.className?.trim()
            val isStaff = rawUserClass.equals("Schulleitung", ignoreCase = true) || 
                          rawUserClass.equals("Lehrer", ignoreCase = true) || 
                          rawUserClass.equals("Kollegium", ignoreCase = true) ||
                          de.kjltowinkler.msapp.data.backend.BackendApiClient.currentUser?.role?.uppercase() in listOf("TEACHER", "ADMIN", "LEHRER")
            val userClass = if (isStaff || rawUserClass.isNullOrBlank()) className else rawUserClass

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
                    PlatformStorage.setString(KEY_TIMETABLE_CACHE, json.encodeToString(dto))
                } catch (_: Exception) {}
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
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

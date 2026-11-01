package de.kjltowinkler.msapp.data.courses

import androidx.compose.runtime.mutableStateListOf
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CourseRepository {

    // Liste der konfigurierten Kurse des Schülers
    val courses = mutableStateListOf<UserCourse>()

    init {
        // Standard-Initialisierung mit typischen Beispiel-Kursen der Merianschule
        courses.addAll(
            listOf(
                UserCourse(
                    id = "course_ethik",
                    subject = "Ethik",
                    teacher = "Web",
                    name = "Ethik (Web)"
                ),
                UserCourse(
                    id = "course_wpu",
                    subject = "WPU",
                    teacher = "Mül",
                    name = "WPU Informatik"
                ),
                UserCourse(
                    id = "course_fs2",
                    subject = "Französisch",
                    teacher = "Hol",
                    name = "Französisch"
                )
            )
        )
    }

    fun addCourse(course: UserCourse) {
        courses.removeAll { it.id == course.id || (it.subject.equals(course.subject, ignoreCase = true) && !course.isCustom) }
        courses.add(course)
        syncWithBackendIfPossible()
    }

    fun updateCourse(course: UserCourse) {
        val index = courses.indexOfFirst { it.id == course.id }
        if (index != -1) {
            courses[index] = course
            syncWithBackendIfPossible()
        } else {
            addCourse(course)
        }
    }

    fun removeCourse(id: String) {
        courses.removeAll { it.id == id }
        syncWithBackendIfPossible()
    }

    fun setCourses(newCourses: List<UserCourse>) {
        courses.clear()
        courses.addAll(newCourses)
    }

    /**
     * Prüft, ob ein Vertretungsplan-Eintrag für diesen Schüler relevant ist.
     * Wenn der Schüler z.B. Ethik bei "Web" hat, werden Einträge für
     * "Religion" oder "Ethik (Car)" herausgefiltert.
     */
    fun isEntryRelevant(entry: SubstitutionEntry, userClassName: String = "09AR"): Boolean {
        // 1. Wenn die Klasse des Nutzers nicht im Eintrag vorkommt, ist es generell nicht seine Klasse
        if (!isClassMatching(entry.className, userClassName)) {
            return false
        }

        val subjectClean = entry.subject.trim()
        val details = entry.details
        val roomInfo = entry.roomInfo

        // 2. Religion / Ethik Gruppe
        val isReligionOrEthik = subjectClean.contains("Ethik", ignoreCase = true) ||
                                subjectClean.contains("Religion", ignoreCase = true)

        if (isReligionOrEthik) {
            val userRelEthCourses = courses.filter {
                it.subject.contains("Ethik", ignoreCase = true) || it.subject.contains("Religion", ignoreCase = true)
            }
            if (userRelEthCourses.isNotEmpty()) {
                // Prüfe, ob einer der Kurse passt
                return userRelEthCourses.any { myCourse ->
                    val subjectMatches = when {
                        myCourse.subject.contains("Ethik", ignoreCase = true) -> subjectClean.contains("Ethik", ignoreCase = true)
                        myCourse.subject.contains("Religion", ignoreCase = true) -> subjectClean.contains("Religion", ignoreCase = true)
                        else -> true
                    }
                    if (!subjectMatches) return@any false

                    // Falls der Nutzer ein Lehrerkürzel angegeben hat, prüfe ob das Kürzel im Eintrag vorkommt
                    if (myCourse.teacher.isNotBlank()) {
                        containsTeacherToken(details, myCourse.teacher) || containsTeacherToken(roomInfo, myCourse.teacher)
                    } else {
                        true
                    }
                }
            }
        }

        // 3. WPU (Wahlpflichtunterricht) Gruppe
        val isWpu = subjectClean.contains("WPU", ignoreCase = true) || entry.className.contains("WPU", ignoreCase = true)
        if (isWpu) {
            val userWpuCourses = courses.filter { it.subject.contains("WPU", ignoreCase = true) }
            if (userWpuCourses.isNotEmpty()) {
                return userWpuCourses.any { myCourse ->
                    if (myCourse.teacher.isNotBlank()) {
                        containsTeacherToken(details, myCourse.teacher) || containsTeacherToken(roomInfo, myCourse.teacher)
                    } else if (myCourse.name.isNotBlank()) {
                        details.contains(myCourse.name, ignoreCase = true) || subjectClean.contains(myCourse.name, ignoreCase = true)
                    } else {
                        true
                    }
                }
            }
        }

        // 4. 2. Fremdsprache (Französisch / Latein / Spanisch)
        val isLanguage = subjectClean.equals("Französisch", ignoreCase = true) ||
                         subjectClean.equals("Latein", ignoreCase = true) ||
                         subjectClean.equals("Spanisch", ignoreCase = true)

        if (isLanguage) {
            val userLangCourses = courses.filter {
                it.subject.equals("Französisch", ignoreCase = true) ||
                it.subject.equals("Latein", ignoreCase = true) ||
                it.subject.equals("Spanisch", ignoreCase = true)
            }
            if (userLangCourses.isNotEmpty()) {
                return userLangCourses.any { myCourse ->
                    if (!myCourse.subject.equals(subjectClean, ignoreCase = true)) return@any false
                    if (myCourse.teacher.isNotBlank()) {
                        containsTeacherToken(details, myCourse.teacher) || containsTeacherToken(roomInfo, myCourse.teacher)
                    } else {
                        true
                    }
                }
            }
        }

        // 5. Andere benutzerdefinierte Fächer
        val matchingCustomCourses = courses.filter { it.subject.equals(subjectClean, ignoreCase = true) }
        if (matchingCustomCourses.isNotEmpty()) {
            val teacherSpecified = matchingCustomCourses.filter { it.teacher.isNotBlank() }
            if (teacherSpecified.isNotEmpty()) {
                // Wenn ein bestimmter Lehrer konfiguriert ist und der Eintrag einen anderen Lehrer nennt
                return teacherSpecified.any {
                    containsTeacherToken(details, it.teacher) || containsTeacherToken(roomInfo, it.teacher)
                }
            }
        }

        // Für reguläre Klassenstunden (z.B. Deutsch, Mathe) ist der Eintrag relevant
        return true
    }

    private fun isClassMatching(entryClassName: String, userClass: String): Boolean {
        if (entryClassName.isBlank()) return true
        if (userClass.isBlank()) return true

        // Normalisiere Klassenbezeichnungen z. B. "9aR" vs "09AR"
        fun normalize(c: String): String {
            return c.trim().uppercase()
                .replace("^0+".toRegex(), "") // "09AR" -> "9AR"
                .replace("\\s+".toRegex(), "")
        }

        val normUser = normalize(userClass)
        val classesInEntry = entryClassName.split(",", ";", "/").map { normalize(it) }

        return classesInEntry.any { it.contains(normUser) || normUser.contains(it) }
    }

    private fun containsTeacherToken(text: String, teacherCode: String): Boolean {
        if (teacherCode.isBlank()) return true
        val regex = Regex("""(?i)\b${Regex.escape(teacherCode.trim())}\b""")
        return regex.containsMatchIn(text)
    }

    private fun syncWithBackendIfPossible() {
        if (de.kjltowinkler.msapp.data.backend.BackendApiClient.isLoggedIn) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    de.kjltowinkler.msapp.data.backend.BackendApiClient.syncUserCourses(courses.toList())
                } catch (_: Exception) {}
            }
        }
    }
}

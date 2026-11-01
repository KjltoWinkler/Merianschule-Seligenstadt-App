package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
import de.kjltowinkler.msapp.data.sync.ClassTimetableSummary
import de.kjltowinkler.msapp.data.timetable.TeacherInfo
import de.kjltowinkler.msapp.data.timetable.TimetableDay
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.ResultSet

object TimetableServerRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getJsonFile(className: String): File = 
        File(ServerConfig.dataDir, "timetable_${className.lowercase().replace(" ", "_")}.json")

    suspend fun initDefaultsIfEmpty() {
        val count = getCount()
        if (count == 0) {
            // Seed Monday timetable for class 09AR from TimetableRepository
            val defaultLessons = TimetableRepository.getLessonsForDay(TimetableDay.MONTAG)
            val daysMap = mutableMapOf<String, List<TimetableLesson>>()
            TimetableDay.entries.forEach { day ->
                daysMap[day.id] = TimetableRepository.getLessonsForDay(day)
            }
            saveClassTimetable("09AR", daysMap, validFor = "Schuljahr 2026/2027")
        }
    }

    suspend fun getClassTimetable(className: String): ClassTimetableDto = DatabaseFactory.query { conn ->
        val trimmed = className.trim()
        val queryClass = trimmed.lowercase()

        // Generate possible aliases (e.g., "09ar" <-> "9ar")
        val altClass = when {
            queryClass.startsWith("0") && queryClass.length > 1 && queryClass[1].isDigit() -> queryClass.substring(1)
            queryClass.isNotEmpty() && queryClass[0].isDigit() -> "0$queryClass"
            else -> queryClass
        }

        fun fetchForClass(targetClass: String): ClassTimetableDto? {
            val sql = "SELECT day_id, lessons_json, valid_for, updated_at FROM timetables WHERE lower(class_name) = ?;"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, targetClass.lowercase())
                stmt.executeQuery().use { rs ->
                    val days = mutableMapOf<String, List<TimetableLesson>>()
                    var lastUpdated = 0L
                    var validFor = ""
                    var foundAny = false
                    while (rs.next()) {
                        foundAny = true
                        val dayId = rs.getString("day_id")
                        val raw = rs.getString("lessons_json")
                        val vFor = rs.getString("valid_for") ?: ""
                        if (vFor.isNotBlank()) validFor = vFor
                        val updated = rs.getLong("updated_at")
                        if (updated > lastUpdated) lastUpdated = updated
                        try {
                            days[dayId] = json.decodeFromString<List<TimetableLesson>>(raw)
                        } catch (e: Exception) {
                            days[dayId] = emptyList()
                        }
                    }
                    if (foundAny && days.isNotEmpty()) {
                        return ClassTimetableDto(
                            className = targetClass.uppercase(),
                            days = days,
                            lastUpdated = lastUpdated,
                            validFor = validFor
                        )
                    }
                }
            }
            return null
        }

        // 1. Try exact requested class
        var result = fetchForClass(queryClass)

        // 2. Try alias (with/without leading zero)
        if (result == null && altClass != queryClass) {
            result = fetchForClass(altClass)
        }

        // 3. Fallback: If still nothing found (e.g. "Schulleitung", "Lehrer", or non-existent class),
        // return the most recently updated class timetable in the database
        if (result == null) {
            val fallbackSql = "SELECT class_name FROM timetables ORDER BY updated_at DESC LIMIT 1;"
            conn.createStatement().use { stmt ->
                stmt.executeQuery(fallbackSql).use { rs ->
                    if (rs.next()) {
                        val availableClass = rs.getString("class_name")
                        if (!availableClass.isNullOrBlank()) {
                            result = fetchForClass(availableClass)
                        }
                    }
                }
            }
        }

        result ?: ClassTimetableDto(
            className = className,
            days = emptyMap(),
            lastUpdated = 0L,
            validFor = ""
        )
    }

    suspend fun saveClassTimetable(
        className: String, 
        days: Map<String, List<TimetableLesson>>,
        validFor: String = "",
        rawJsonContent: String? = null
    ): Int = DatabaseFactory.query { conn ->
        val now = System.currentTimeMillis()
        val normalizedClass = className.trim().uppercase()

        conn.autoCommit = false
        var count = 0
        try {
            conn.prepareStatement("DELETE FROM timetables WHERE class_name = ?;").use { stmt ->
                stmt.setString(1, normalizedClass)
                stmt.executeUpdate()
            }

            val sql = "INSERT INTO timetables (class_name, day_id, lessons_json, valid_for, updated_at) VALUES (?, ?, ?, ?, ?);"
            conn.prepareStatement(sql).use { stmt ->
                for ((dayId, lessons) in days) {
                    stmt.setString(1, normalizedClass)
                    stmt.setString(2, dayId)
                    stmt.setString(3, json.encodeToString(lessons))
                    stmt.setString(4, validFor)
                    stmt.setLong(5, now)
                    stmt.addBatch()
                    count += lessons.size
                }
                stmt.executeBatch()
            }
            conn.commit()
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }

        // Persist file backup to disk
        try {
            ServerConfig.ensureDataDirectory()
            val textToSave = rawJsonContent ?: json.encodeToString(
                ClassTimetableDto(className = normalizedClass, days = days, lastUpdated = now, validFor = validFor)
            )
            getJsonFile(normalizedClass).writeText(textToSave)
        } catch (e: Exception) {
            println("Fehler beim Speichern der timetable_${normalizedClass}.json: ${e.message}")
        }

        count
    }

    suspend fun getAllClassesWithTimetables(): List<ClassTimetableSummary> = DatabaseFactory.query { conn ->
        val sql = """
            SELECT class_name, valid_for, MAX(updated_at) as last_updated, COUNT(day_id) as day_count 
            FROM timetables 
            GROUP BY class_name, valid_for
            ORDER BY class_name ASC;
        """.trimIndent()
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val list = mutableListOf<ClassTimetableSummary>()
                while (rs.next()) {
                    list.add(
                        ClassTimetableSummary(
                            className = rs.getString("class_name"),
                            validFor = rs.getString("valid_for") ?: "",
                            lastUpdated = rs.getLong("last_updated"),
                            daysCount = rs.getInt("day_count")
                        )
                    )
                }
                list
            }
        }
    }

    suspend fun deleteClassTimetable(className: String): Boolean = DatabaseFactory.query { conn ->
        val normalized = className.trim().uppercase()
        val deleted = conn.prepareStatement("DELETE FROM timetables WHERE upper(class_name) = ?;").use { stmt ->
            stmt.setString(1, normalized)
            stmt.executeUpdate() > 0
        }
        try {
            val file = getJsonFile(normalized)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
        deleted
    }

    suspend fun getTeacherDirectory(): Map<String, TeacherInfo> {
        return TimetableRepository.teacherDirectory
    }

    suspend fun getCount(): Int = DatabaseFactory.query { conn ->
        val sql = "SELECT COUNT(*) FROM timetables;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}

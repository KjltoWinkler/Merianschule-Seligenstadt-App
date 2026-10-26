package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
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
            saveClassTimetable("09AR", daysMap)
        }
    }

    suspend fun getClassTimetable(className: String): ClassTimetableDto = DatabaseFactory.query { conn ->
        val sql = "SELECT day_id, lessons_json, updated_at FROM timetables WHERE lower(class_name) = ?;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, className.trim().lowercase())
            stmt.executeQuery().use { rs ->
                val days = mutableMapOf<String, List<TimetableLesson>>()
                var lastUpdated = 0L
                while (rs.next()) {
                    val dayId = rs.getString("day_id")
                    val raw = rs.getString("lessons_json")
                    val updated = rs.getLong("updated_at")
                    if (updated > lastUpdated) lastUpdated = updated
                    try {
                        days[dayId] = json.decodeFromString<List<TimetableLesson>>(raw)
                    } catch (e: Exception) {
                        days[dayId] = emptyList()
                    }
                }
                ClassTimetableDto(
                    className = className,
                    days = days,
                    lastUpdated = lastUpdated
                )
            }
        }
    }

    suspend fun saveClassTimetable(
        className: String, 
        days: Map<String, List<TimetableLesson>>,
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

            val sql = "INSERT INTO timetables (class_name, day_id, lessons_json, updated_at) VALUES (?, ?, ?, ?);"
            conn.prepareStatement(sql).use { stmt ->
                for ((dayId, lessons) in days) {
                    stmt.setString(1, normalizedClass)
                    stmt.setString(2, dayId)
                    stmt.setString(3, json.encodeToString(lessons))
                    stmt.setLong(4, now)
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
                ClassTimetableDto(className = normalizedClass, days = days, lastUpdated = now)
            )
            getJsonFile(normalizedClass).writeText(textToSave)
        } catch (e: Exception) {
            println("Fehler beim Speichern der timetable_${normalizedClass}.json: ${e.message}")
        }

        count
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

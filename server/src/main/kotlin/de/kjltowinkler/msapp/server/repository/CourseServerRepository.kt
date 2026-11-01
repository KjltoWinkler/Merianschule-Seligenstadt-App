package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.courses.UserCourse
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import java.sql.ResultSet

object CourseServerRepository {

    private fun mapResultSet(rs: ResultSet): UserCourse = UserCourse(
        id = rs.getString("id"),
        subject = rs.getString("subject"),
        teacher = rs.getString("teacher"),
        name = rs.getString("name"),
        room = rs.getString("room"),
        isCustom = rs.getInt("is_custom") == 1
    )

    suspend fun getUserCourses(userId: String): List<UserCourse> = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM user_courses WHERE user_id = ? ORDER BY subject ASC;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<UserCourse>()
                while (rs.next()) {
                    list.add(mapResultSet(rs))
                }
                list
            }
        }
    }

    suspend fun saveUserCourses(userId: String, courses: List<UserCourse>): Boolean = DatabaseFactory.query { conn ->
        conn.autoCommit = false
        try {
            // 1. Vorhandene Kurse des Nutzers löschen
            conn.prepareStatement("DELETE FROM user_courses WHERE user_id = ?;").use { stmt ->
                stmt.setString(1, userId)
                stmt.executeUpdate()
            }

            // 2. Neue Kurse einfügen
            val sql = """
                INSERT INTO user_courses (id, user_id, subject, teacher, name, room, is_custom, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()

            val now = System.currentTimeMillis()
            conn.prepareStatement(sql).use { stmt ->
                for (c in courses) {
                    stmt.setString(1, c.id)
                    stmt.setString(2, userId)
                    stmt.setString(3, c.subject)
                    stmt.setString(4, c.teacher)
                    stmt.setString(5, c.name)
                    stmt.setString(6, c.room)
                    stmt.setInt(7, if (c.isCustom) 1 else 0)
                    stmt.setLong(8, now)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }

            conn.commit()
            true
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }
}

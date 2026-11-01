package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.task.SchoolTaskItem
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import java.sql.ResultSet

object SchoolTaskRepository {

    private fun mapSchoolTask(rs: ResultSet): SchoolTaskItem = SchoolTaskItem(
        id = rs.getString("id"),
        title = rs.getString("title"),
        description = rs.getString("description") ?: "",
        subject = rs.getString("subject") ?: "",
        dueDate = rs.getString("due_date") ?: "",
        dueTime = rs.getString("due_time") ?: "",
        deadlineDate = rs.getString("deadline_date") ?: "",
        createdDate = rs.getString("created_date") ?: "",
        targetClass = rs.getString("target_class") ?: "09AR",
        creatorName = rs.getString("creator_name") ?: "Lehrkraft",
        updatedAt = rs.getLong("updated_at")
    )

    suspend fun getAllSchoolTasks(): List<SchoolTaskItem> = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM school_tasks ORDER BY updated_at DESC;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<SchoolTaskItem>()
                while (rs.next()) {
                    list.add(mapSchoolTask(rs))
                }
                list
            }
        }
    }

    suspend fun getSchoolTasksForClass(className: String): List<SchoolTaskItem> = DatabaseFactory.query { conn ->
        val trimmed = className.trim()
        val queryClass = trimmed.lowercase()
        val altClass = when {
            queryClass.startsWith("0") && queryClass.length > 1 && queryClass[1].isDigit() -> queryClass.substring(1)
            queryClass.isNotEmpty() && queryClass[0].isDigit() -> "0$queryClass"
            else -> queryClass
        }

        val sql = """
            SELECT * FROM school_tasks 
            WHERE lower(target_class) = 'all' 
               OR lower(target_class) = 'alle' 
               OR lower(target_class) = ? 
               OR lower(target_class) = ? 
            ORDER BY updated_at DESC;
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, queryClass)
            stmt.setString(2, altClass)
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<SchoolTaskItem>()
                while (rs.next()) {
                    list.add(mapSchoolTask(rs))
                }
                list
            }
        }
    }

    suspend fun upsertSchoolTask(task: SchoolTaskItem): SchoolTaskItem = DatabaseFactory.query { conn ->
        val now = if (task.updatedAt > 0L) task.updatedAt else System.currentTimeMillis()
        val taskId = task.id.ifBlank { "school_task_${System.currentTimeMillis()}" }
        val resolvedSubject = TimetableRepository.resolveSubjectName(task.subject)
        val targetClass = task.targetClass.trim().uppercase().ifBlank { "09AR" }
        val creatorName = task.creatorName.trim().ifBlank { "Lehrkraft" }

        val sql = """
            INSERT INTO school_tasks (
                id, title, description, subject, due_date, due_time,
                deadline_date, created_date, target_class, creator_name, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                subject = excluded.subject,
                due_date = excluded.due_date,
                due_time = excluded.due_time,
                deadline_date = excluded.deadline_date,
                created_date = excluded.created_date,
                target_class = excluded.target_class,
                creator_name = excluded.creator_name,
                updated_at = excluded.updated_at;
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, taskId)
            stmt.setString(2, task.title.trim())
            stmt.setString(3, task.description.trim())
            stmt.setString(4, resolvedSubject)
            stmt.setString(5, task.dueDate.trim())
            stmt.setString(6, task.dueTime.trim())
            stmt.setString(7, task.deadlineDate.trim())
            stmt.setString(8, task.createdDate.trim().ifBlank {
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            })
            stmt.setString(9, targetClass)
            stmt.setString(10, creatorName)
            stmt.setLong(11, now)
            stmt.executeUpdate()
        }

        // Auch alle bestehenden Schüler-Task-Einträge für diese Schulaufgabe aktualisieren (Titel, Beschreibung, Fach, Frist)
        val updateExistingTasksSql = """
            UPDATE tasks SET 
                title = ?, 
                description = ?, 
                subject = ?, 
                due_date = ?, 
                due_time = ?, 
                deadline_date = ?, 
                updated_at = ?
            WHERE id = ? AND is_school_task = 1;
        """.trimIndent()
        conn.prepareStatement(updateExistingTasksSql).use { stmt ->
            stmt.setString(1, task.title.trim())
            stmt.setString(2, task.description.trim())
            stmt.setString(3, resolvedSubject)
            stmt.setString(4, task.dueDate.trim())
            stmt.setString(5, task.dueTime.trim())
            stmt.setString(6, task.deadlineDate.trim())
            stmt.setLong(7, now)
            stmt.setString(8, taskId)
            stmt.executeUpdate()
        }

        task.copy(
            id = taskId,
            subject = resolvedSubject,
            targetClass = targetClass,
            creatorName = creatorName,
            updatedAt = now
        )
    }

    suspend fun deleteSchoolTask(id: String): Boolean = DatabaseFactory.query { conn ->
        val sql = "DELETE FROM school_tasks WHERE id = ?;"
        val deleted = conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate() > 0
        }
        if (deleted) {
            conn.prepareStatement("DELETE FROM tasks WHERE id = ? AND is_school_task = 1;").use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate()
            }
        }
        deleted
    }

    suspend fun getCount(): Int = DatabaseFactory.query { conn ->
        val sql = "SELECT COUNT(*) FROM school_tasks;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}

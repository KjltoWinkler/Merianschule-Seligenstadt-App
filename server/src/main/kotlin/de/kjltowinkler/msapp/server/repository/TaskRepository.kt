package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.sync.SyncTasksRequest
import de.kjltowinkler.msapp.data.sync.SyncTasksResponse
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TaskList
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import de.kjltowinkler.msapp.server.security.DataEncryptionService
import java.sql.ResultSet
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserTaskDto(
    val id: String,
    val userId: String,
    val username: String = "",
    val fullName: String = "",
    val className: String = "",
    val title: String,
    val description: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val dueTime: String = "",
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val isSchoolTask: Boolean = false,
    val listId: String = "my_tasks",
    val updatedAt: Long = 0L
)

object TaskRepository {

    private fun mapTask(rs: ResultSet): TaskItem = TaskItem(
        id = rs.getString("id"),
        title = DataEncryptionService.decrypt(rs.getString("title")),
        description = DataEncryptionService.decrypt(rs.getString("description") ?: ""),
        subject = DataEncryptionService.decrypt(rs.getString("subject") ?: ""),
        dueDate = rs.getString("due_date") ?: "",
        dueTime = rs.getString("due_time") ?: "",
        isCompleted = rs.getInt("is_completed") == 1,
        completedDate = DataEncryptionService.decrypt(rs.getString("completed_date") ?: ""),
        createdDate = DataEncryptionService.decrypt(rs.getString("created_date") ?: ""),
        deadlineDate = DataEncryptionService.decrypt(rs.getString("deadline_date") ?: ""),
        isStarred = rs.getInt("is_starred") == 1,
        isSchoolTask = rs.getInt("is_school_task") == 1,
        listId = rs.getString("list_id") ?: "my_tasks",
        updatedAt = rs.getLong("updated_at")
    )

    private fun mapList(rs: ResultSet): TaskList = TaskList(
        id = rs.getString("id"),
        title = if (rs.getString("id") == "my_tasks") "Meine Aufgaben" else DataEncryptionService.decrypt(rs.getString("title")),
        updatedAt = rs.getLong("updated_at")
    )

    suspend fun getTasks(userId: String): List<TaskItem> {
        val user = UserRepository.findById(userId)
        val userClass = user?.className?.ifBlank { "09AR" } ?: "09AR"
        val schoolTasks = SchoolTaskRepository.getSchoolTasksForClass(userClass)

        return DatabaseFactory.query { conn ->
            if (schoolTasks.isNotEmpty()) {
                val insertSchoolTaskSql = """
                    INSERT OR IGNORE INTO tasks (
                        id, user_id, list_id, title, description, subject, 
                        due_date, due_time, is_completed, completed_date, 
                        created_date, deadline_date, is_starred, is_school_task, updated_at
                    ) VALUES (?, ?, 'my_tasks', ?, ?, ?, ?, ?, 0, '', ?, ?, 0, 1, ?);
                """.trimIndent()
                conn.prepareStatement(insertSchoolTaskSql).use { stmt ->
                    for (st in schoolTasks) {
                        stmt.setString(1, st.id)
                        stmt.setString(2, userId)
                        stmt.setString(3, DataEncryptionService.encrypt(st.title))
                        stmt.setString(4, DataEncryptionService.encrypt(st.description))
                        stmt.setString(5, DataEncryptionService.encrypt(st.subject))
                        stmt.setString(6, st.dueDate)
                        stmt.setString(7, st.dueTime)
                        stmt.setString(8, DataEncryptionService.encrypt(st.createdDate))
                        stmt.setString(9, DataEncryptionService.encrypt(st.deadlineDate))
                        stmt.setLong(10, st.updatedAt)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            val sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY is_completed ASC, updated_at DESC;"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<TaskItem>()
                    while (rs.next()) {
                        list.add(mapTask(rs))
                    }
                    list
                }
            }
        }
    }

    suspend fun getLists(userId: String): List<TaskList> = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM task_lists WHERE user_id = ? ORDER BY (id = 'my_tasks') DESC, updated_at ASC;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<TaskList>()
                while (rs.next()) {
                    list.add(mapList(rs))
                }
                if (list.none { it.id == "my_tasks" }) {
                    list.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
                }
                list
            }
        }
    }

    suspend fun upsertTask(userId: String, task: TaskItem): TaskItem = DatabaseFactory.query { conn ->
        val now = if (task.updatedAt > 0L) task.updatedAt else System.currentTimeMillis()
        val sql = """
            INSERT INTO tasks (
                id, user_id, list_id, title, description, subject, 
                due_date, due_time, is_completed, completed_date, 
                created_date, deadline_date, is_starred, is_school_task, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id, user_id) DO UPDATE SET
                list_id = excluded.list_id,
                title = excluded.title,
                description = excluded.description,
                subject = excluded.subject,
                due_date = excluded.due_date,
                due_time = excluded.due_time,
                is_completed = excluded.is_completed,
                completed_date = excluded.completed_date,
                created_date = excluded.created_date,
                deadline_date = excluded.deadline_date,
                is_starred = excluded.is_starred,
                is_school_task = excluded.is_school_task,
                updated_at = excluded.updated_at
            WHERE excluded.updated_at >= tasks.updated_at;
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, task.id)
            stmt.setString(2, userId)
            stmt.setString(3, task.listId)
            stmt.setString(4, DataEncryptionService.encrypt(task.title))
            stmt.setString(5, DataEncryptionService.encrypt(task.description))
            stmt.setString(6, DataEncryptionService.encrypt(task.subject))
            stmt.setString(7, task.dueDate)
            stmt.setString(8, task.dueTime)
            stmt.setInt(9, if (task.isCompleted) 1 else 0)
            stmt.setString(10, DataEncryptionService.encrypt(task.completedDate))
            stmt.setString(11, DataEncryptionService.encrypt(task.createdDate))
            stmt.setString(12, DataEncryptionService.encrypt(task.deadlineDate))
            stmt.setInt(13, if (task.isStarred) 1 else 0)
            stmt.setInt(14, if (task.isSchoolTask) 1 else 0)
            stmt.setLong(15, now)
            stmt.executeUpdate()
        }
        task.copy(updatedAt = now)
    }

    suspend fun deleteTask(userId: String, taskId: String): Boolean = DatabaseFactory.query { conn ->
        val sql = "DELETE FROM tasks WHERE id = ? AND user_id = ? AND (is_school_task = 0 OR id NOT LIKE 'school_task_%');"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, taskId)
            stmt.setString(2, userId)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun adminDeleteTask(taskId: String): Boolean = DatabaseFactory.query { conn ->
        val sql = "DELETE FROM tasks WHERE id = ?;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, taskId)
            stmt.executeUpdate() > 0
        }
    }

    /**
     * Aus Datenschutzgründen (DSGVO Art. 5 & 9) werden Schüleraufgaben
     * nicht an das Admin-Panel oder Administratoren herausgegeben.
     */
    suspend fun getAllUserTasks(): List<AdminUserTaskDto> {
        return emptyList()
    }

    suspend fun upsertList(userId: String, list: TaskList): TaskList = DatabaseFactory.query { conn ->
        val now = if (list.updatedAt > 0L) list.updatedAt else System.currentTimeMillis()
        val sql = """
            INSERT INTO task_lists (id, user_id, title, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id, user_id) DO UPDATE SET
                title = excluded.title,
                updated_at = excluded.updated_at
            WHERE excluded.updated_at >= task_lists.updated_at;
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, list.id)
            stmt.setString(2, userId)
            stmt.setString(3, if (list.id == "my_tasks") "Meine Aufgaben" else DataEncryptionService.encrypt(list.title))
            stmt.setLong(4, now)
            stmt.executeUpdate()
        }
        list.copy(updatedAt = now)
    }

    suspend fun deleteList(userId: String, listId: String): Boolean = DatabaseFactory.query { conn ->
        if (listId == "my_tasks") return@query false // Cannot delete default list

        conn.prepareStatement("DELETE FROM tasks WHERE list_id = ? AND user_id = ? AND (is_school_task = 0 OR id NOT LIKE 'school_task_%');").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeUpdate()
        }

        conn.prepareStatement("DELETE FROM task_lists WHERE id = ? AND user_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun sync(userId: String, request: SyncTasksRequest): SyncTasksResponse {
        val user = UserRepository.findById(userId)
        val userClass = user?.className?.ifBlank { "09AR" } ?: "09AR"
        val schoolTasks = SchoolTaskRepository.getSchoolTasksForClass(userClass)

        return DatabaseFactory.query { conn ->
            val now = System.currentTimeMillis()

            // 0. Löschungen des Clients in der Datenbank anwenden (Nur offizielle school_task_ Schulaufgaben können vom Client nicht gelöscht werden)
            if (request.deletedTaskIds.isNotEmpty()) {
                conn.prepareStatement("DELETE FROM tasks WHERE user_id = ? AND id = ? AND (is_school_task = 0 OR id NOT LIKE 'school_task_%');").use { stmt ->
                    for (id in request.deletedTaskIds) {
                        stmt.setString(1, userId)
                        stmt.setString(2, id)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        if (request.deletedListIds.isNotEmpty()) {
            conn.prepareStatement("DELETE FROM tasks WHERE user_id = ? AND list_id = ?;").use { stmt ->
                for (id in request.deletedListIds) {
                    stmt.setString(1, userId)
                    stmt.setString(2, id)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
            conn.prepareStatement("DELETE FROM task_lists WHERE user_id = ? AND id = ?;").use { stmt ->
                for (id in request.deletedListIds) {
                    if (id != "my_tasks") {
                        stmt.setString(1, userId)
                        stmt.setString(2, id)
                        stmt.addBatch()
                    }
                }
                stmt.executeBatch()
            }
        }

        // 1. Upsert lists from client
        if (request.lists.isNotEmpty()) {
            val listSql = """
                INSERT INTO task_lists (id, user_id, title, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(id, user_id) DO UPDATE SET
                    title = excluded.title,
                    updated_at = excluded.updated_at
                WHERE excluded.updated_at >= task_lists.updated_at;
            """.trimIndent()
            conn.prepareStatement(listSql).use { stmt ->
                for (l in request.lists) {
                    val updateTime = if (l.updatedAt > 0L) l.updatedAt else now
                    stmt.setString(1, l.id)
                    stmt.setString(2, userId)
                    stmt.setString(3, if (l.id == "my_tasks") "Meine Aufgaben" else DataEncryptionService.encrypt(l.title))
                    stmt.setLong(4, updateTime)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }

        // 2. Upsert tasks from client
        if (request.tasks.isNotEmpty()) {
            val taskSql = """
                INSERT INTO tasks (
                    id, user_id, list_id, title, description, subject, 
                    due_date, due_time, is_completed, completed_date, 
                    created_date, deadline_date, is_starred, is_school_task, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id, user_id) DO UPDATE SET
                    list_id = excluded.list_id,
                    title = excluded.title,
                    description = excluded.description,
                    subject = excluded.subject,
                    due_date = excluded.due_date,
                    due_time = excluded.due_time,
                    is_completed = excluded.is_completed,
                    completed_date = excluded.completed_date,
                    created_date = excluded.created_date,
                    deadline_date = excluded.deadline_date,
                    is_starred = excluded.is_starred,
                    is_school_task = excluded.is_school_task,
                    updated_at = excluded.updated_at
                WHERE excluded.updated_at >= tasks.updated_at;
            """.trimIndent()
            conn.prepareStatement(taskSql).use { stmt ->
                for (t in request.tasks) {
                    val updateTime = if (t.updatedAt > 0L) t.updatedAt else now
                    stmt.setString(1, t.id)
                    stmt.setString(2, userId)
                    stmt.setString(3, t.listId)
                    stmt.setString(4, DataEncryptionService.encrypt(t.title))
                    stmt.setString(5, DataEncryptionService.encrypt(t.description))
                    stmt.setString(6, DataEncryptionService.encrypt(t.subject))
                    stmt.setString(7, t.dueDate)
                    stmt.setString(8, t.dueTime)
                    stmt.setInt(9, if (t.isCompleted) 1 else 0)
                    stmt.setString(10, DataEncryptionService.encrypt(t.completedDate))
                    stmt.setString(11, DataEncryptionService.encrypt(t.createdDate))
                    stmt.setString(12, DataEncryptionService.encrypt(t.deadlineDate))
                    stmt.setInt(13, if (t.isStarred) 1 else 0)
                    stmt.setInt(14, if (t.isSchoolTask) 1 else 0)
                    stmt.setLong(15, updateTime)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }

        // 2.5 Sicherstellen, dass alle Schulaufgaben für die Klasse des Schülers vorhanden sind
        if (schoolTasks.isNotEmpty()) {
            val insertSchoolTaskSql = """
                INSERT OR IGNORE INTO tasks (
                    id, user_id, list_id, title, description, subject, 
                    due_date, due_time, is_completed, completed_date, 
                    created_date, deadline_date, is_starred, is_school_task, updated_at
                ) VALUES (?, ?, 'my_tasks', ?, ?, ?, ?, ?, 0, '', ?, ?, 0, 1, ?);
            """.trimIndent()
            conn.prepareStatement(insertSchoolTaskSql).use { stmt ->
                for (st in schoolTasks) {
                    stmt.setString(1, st.id)
                    stmt.setString(2, userId)
                    stmt.setString(3, DataEncryptionService.encrypt(st.title))
                    stmt.setString(4, DataEncryptionService.encrypt(st.description))
                    stmt.setString(5, DataEncryptionService.encrypt(st.subject))
                    stmt.setString(6, st.dueDate)
                    stmt.setString(7, st.dueTime)
                    stmt.setString(8, DataEncryptionService.encrypt(st.createdDate))
                    stmt.setString(9, DataEncryptionService.encrypt(st.deadlineDate))
                    stmt.setLong(10, st.updatedAt)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }

        // 3. Return all current tasks and lists from server
        val serverTasks = mutableListOf<TaskItem>()
        conn.prepareStatement("SELECT * FROM tasks WHERE user_id = ? ORDER BY is_completed ASC, updated_at DESC;").use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) serverTasks.add(mapTask(rs))
            }
        }

        val serverLists = mutableListOf<TaskList>()
        conn.prepareStatement("SELECT * FROM task_lists WHERE user_id = ? ORDER BY (id = 'my_tasks') DESC, updated_at ASC;").use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) serverLists.add(mapList(rs))
            }
        }
        if (serverLists.none { it.id == "my_tasks" }) {
            serverLists.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
        }

        SyncTasksResponse(
            tasks = serverTasks,
            lists = serverLists,
            serverTimestamp = now
        )
    }
}

    suspend fun countTasks(): Int = DatabaseFactory.query { conn ->
        val sql = "SELECT COUNT(*) FROM tasks;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}

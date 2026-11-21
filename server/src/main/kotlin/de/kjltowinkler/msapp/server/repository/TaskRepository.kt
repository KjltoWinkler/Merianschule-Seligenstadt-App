package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.sync.SyncTasksRequest
import de.kjltowinkler.msapp.data.sync.SyncTasksResponse
import de.kjltowinkler.msapp.data.task.*
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import de.kjltowinkler.msapp.server.security.DataEncryptionService
import java.sql.ResultSet
import java.util.UUID
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

    private fun mapList(rs: ResultSet): TaskList {
        val id = rs.getString("id")
        val shareToken = runCatching { rs.getString("share_token") }.getOrNull()
        val ownerId = runCatching { rs.getString("owner_id") }.getOrNull()
        val ownerName = runCatching { rs.getString("owner_name") }.getOrNull()
        val roleStr = runCatching { rs.getString("role") }.getOrNull()
        val role = if (!roleStr.isNullOrBlank()) {
            runCatching { TaskMemberRole.valueOf(roleStr) }.getOrDefault(TaskMemberRole.OWNER)
        } else {
            TaskMemberRole.OWNER
        }
        return TaskList(
            id = id,
            title = if (id == "my_tasks") "Meine Aufgaben" else DataEncryptionService.decrypt(rs.getString("title")),
            updatedAt = rs.getLong("updated_at"),
            isShared = !shareToken.isNullOrBlank(),
            shareToken = shareToken,
            ownerId = ownerId,
            ownerName = ownerName,
            role = role
        )
    }

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

            val list = mutableListOf<TaskItem>()
            val sql = "SELECT * FROM tasks WHERE user_id = ? AND id NOT LIKE 'sample_%' ORDER BY is_completed ASC, updated_at DESC;"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapTask(rs))
                    }
                }
            }

            // Geteilte Aufgaben aus Listen von anderen Nutzern abrufen
            val sharedTasksSql = """
                SELECT t.* FROM tasks t
                JOIN shared_task_lists stl ON t.list_id = stl.list_id AND t.user_id = stl.owner_id
                JOIN shared_task_list_members stlm ON stl.list_id = stlm.list_id
                WHERE stlm.user_id = ? AND (stlm.status = 'ACCEPTED' OR stlm.status IS NULL) AND t.is_school_task = 0;
            """.trimIndent()
            conn.prepareStatement(sharedTasksSql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val task = mapTask(rs)
                        if (list.none { it.id == task.id }) {
                            list.add(task)
                        }
                    }
                }
            }
            list
        }
    }

    suspend fun getLists(userId: String): List<TaskList> = DatabaseFactory.query { conn ->
        val list = mutableListOf<TaskList>()

        // 1. Eigene Listen
        val ownSql = """
            SELECT tl.*, stl.share_token, stl.owner_id 
            FROM task_lists tl 
            LEFT JOIN shared_task_lists stl ON tl.id = stl.list_id AND tl.user_id = stl.owner_id 
            WHERE tl.user_id = ? 
            ORDER BY (tl.id = 'my_tasks') DESC, tl.updated_at ASC;
        """.trimIndent()
        conn.prepareStatement(ownSql).use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val shareTok = rs.getString("share_token")
                    val isShared = !shareTok.isNullOrBlank()
                    list.add(
                        TaskList(
                            id = rs.getString("id"),
                            title = if (rs.getString("id") == "my_tasks") "Meine Aufgaben" else DataEncryptionService.decrypt(rs.getString("title")),
                            updatedAt = rs.getLong("updated_at"),
                            isShared = isShared,
                            shareToken = shareTok,
                            ownerId = userId,
                            role = TaskMemberRole.OWNER
                        )
                    )
                }
            }
        }
        if (list.none { it.id == "my_tasks" }) {
            list.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
        }

        // 2. Beigetretene geteilte Listen
        val sharedSql = """
            SELECT tl.id, tl.title, tl.updated_at, stl.share_token, stl.owner_id, u.full_name as owner_name, stlm.role
            FROM shared_task_list_members stlm
            JOIN shared_task_lists stl ON stlm.list_id = stl.list_id
            JOIN task_lists tl ON stl.list_id = tl.id AND stl.owner_id = tl.user_id
            JOIN users u ON stl.owner_id = u.id
            WHERE stlm.user_id = ? AND (stlm.status = 'ACCEPTED' OR stlm.status IS NULL);
        """.trimIndent()
        conn.prepareStatement(sharedSql).use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val lId = rs.getString("id")
                    if (list.none { it.id == lId }) {
                        list.add(
                            TaskList(
                                id = lId,
                                title = DataEncryptionService.decrypt(rs.getString("title")),
                                updatedAt = rs.getLong("updated_at"),
                                isShared = true,
                                shareToken = rs.getString("share_token"),
                                ownerId = rs.getString("owner_id"),
                                ownerName = rs.getString("owner_name"),
                                role = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER)
                            )
                        )
                    }
                }
            }
        }
        list
    }

    suspend fun upsertTask(userId: String, task: TaskItem): TaskItem = DatabaseFactory.query { conn ->
        val now = if (task.updatedAt > 0L) task.updatedAt else System.currentTimeMillis()

        // Prüfen, ob die Aufgabe zu einer geteilten Liste eines anderen Nutzers gehört
        var effectiveUserId = userId
        conn.prepareStatement("SELECT owner_id FROM shared_task_lists WHERE list_id = ?;").use { sStmt ->
            sStmt.setString(1, task.listId)
            sStmt.executeQuery().use { sRs ->
                if (sRs.next()) {
                    val owner = sRs.getString("owner_id")
                    if (owner != userId) {
                        val canEdit = conn.prepareStatement("SELECT role FROM shared_task_list_members WHERE list_id = ? AND user_id = ? AND (status = 'ACCEPTED' OR status IS NULL);").use { mStmt ->
                            mStmt.setString(1, task.listId)
                            mStmt.setString(2, userId)
                            mStmt.executeQuery().use { mRs ->
                                if (mRs.next()) {
                                    val r = mRs.getString("role")
                                    r == "EDITOR" || r == "OWNER"
                                } else false
                            }
                        }
                        if (!canEdit) error("Keine Schreibberechtigung für diese geteilte Liste")
                        effectiveUserId = owner
                    }
                }
            }
        }

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
            stmt.setString(2, effectiveUserId)
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
        var deleted = false
        val sql = "DELETE FROM tasks WHERE id = ? AND user_id = ? AND (is_school_task = 0 OR id NOT LIKE 'school_task_%');"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, taskId)
            stmt.setString(2, userId)
            deleted = stmt.executeUpdate() > 0
        }
        if (!deleted) {
            // Prüfen, ob die Aufgabe in einer geteilten Liste liegt, in der der Nutzer Bearbeiter ist
            val sharedDelSql = """
                DELETE FROM tasks 
                WHERE id = ? AND list_id IN (
                    SELECT stl.list_id FROM shared_task_lists stl
                    JOIN shared_task_list_members stlm ON stl.list_id = stlm.list_id
                    WHERE stlm.user_id = ? AND stlm.role = 'EDITOR' AND (stlm.status = 'ACCEPTED' OR stlm.status IS NULL)
                ) AND (is_school_task = 0 OR id NOT LIKE 'school_task_%');
            """.trimIndent()
            conn.prepareStatement(sharedDelSql).use { stmt ->
                stmt.setString(1, taskId)
                stmt.setString(2, userId)
                deleted = stmt.executeUpdate() > 0
            }
        }
        deleted
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

        var isOwner = false
        conn.prepareStatement("SELECT 1 FROM task_lists WHERE id = ? AND user_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeQuery().use { rs -> isOwner = rs.next() }
        }

        if (isOwner) {
            conn.prepareStatement("DELETE FROM tasks WHERE list_id = ? AND user_id = ? AND (is_school_task = 0 OR id NOT LIKE 'school_task_%');").use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, userId)
                stmt.executeUpdate()
            }

            conn.prepareStatement("DELETE FROM shared_task_list_members WHERE list_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.executeUpdate()
            }

            conn.prepareStatement("DELETE FROM shared_task_lists WHERE list_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.executeUpdate()
            }

            conn.prepareStatement("DELETE FROM task_lists WHERE id = ? AND user_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, userId)
                stmt.executeUpdate() > 0
            }
        } else {
            // Als Mitglied geteilte Liste verlassen
            conn.prepareStatement("DELETE FROM shared_task_list_members WHERE list_id = ? AND user_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, userId)
                stmt.executeUpdate() > 0
            }
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
                // Auch als Mitglied verlassen wenn in deletedListIds
                conn.prepareStatement("DELETE FROM shared_task_list_members WHERE user_id = ? AND list_id = ?;").use { stmt ->
                    for (id in request.deletedListIds) {
                        stmt.setString(1, userId)
                        stmt.setString(2, id)
                        stmt.addBatch()
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

            // 2. Upsert tasks from client (reject any legacy sample tasks)
            val cleanTasks = request.tasks.filterNot { it.id.startsWith("sample_") }
            if (cleanTasks.isNotEmpty()) {
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
                    for (t in cleanTasks) {
                        var effectiveUserId = userId
                        conn.prepareStatement("SELECT owner_id FROM shared_task_lists WHERE list_id = ?;").use { sStmt ->
                            sStmt.setString(1, t.listId)
                            sStmt.executeQuery().use { sRs ->
                                if (sRs.next()) {
                                    effectiveUserId = sRs.getString("owner_id")
                                }
                            }
                        }

                        val updateTime = if (t.updatedAt > 0L) t.updatedAt else now
                        stmt.setString(1, t.id)
                        stmt.setString(2, effectiveUserId)
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

            // 3. Eigene Aufgaben + Schulaufgaben
            val serverTasks = mutableListOf<TaskItem>()
            conn.prepareStatement("SELECT * FROM tasks WHERE user_id = ? ORDER BY is_completed ASC, updated_at DESC;").use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) serverTasks.add(mapTask(rs))
                }
            }

            // 3.1 Geteilte Aufgaben abrufen
            val sharedTasksSql = """
                SELECT t.* FROM tasks t
                JOIN shared_task_lists stl ON t.list_id = stl.list_id AND t.user_id = stl.owner_id
                JOIN shared_task_list_members stlm ON stl.list_id = stlm.list_id
                WHERE stlm.user_id = ? AND t.is_school_task = 0;
            """.trimIndent()
            conn.prepareStatement(sharedTasksSql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val task = mapTask(rs)
                        if (serverTasks.none { it.id == task.id }) {
                            serverTasks.add(task)
                        }
                    }
                }
            }

            // 4. Eigene Listen
            val serverLists = mutableListOf<TaskList>()
            val ownSql = """
                SELECT tl.*, stl.share_token, stl.owner_id 
                FROM task_lists tl 
                LEFT JOIN shared_task_lists stl ON tl.id = stl.list_id AND tl.user_id = stl.owner_id 
                WHERE tl.user_id = ? 
                ORDER BY (tl.id = 'my_tasks') DESC, tl.updated_at ASC;
            """.trimIndent()
            conn.prepareStatement(ownSql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val shareTok = rs.getString("share_token")
                        val isShared = !shareTok.isNullOrBlank()
                        serverLists.add(
                            TaskList(
                                id = rs.getString("id"),
                                title = if (rs.getString("id") == "my_tasks") "Meine Aufgaben" else DataEncryptionService.decrypt(rs.getString("title")),
                                updatedAt = rs.getLong("updated_at"),
                                isShared = isShared,
                                shareToken = shareTok,
                                ownerId = userId,
                                role = TaskMemberRole.OWNER
                            )
                        )
                    }
                }
            }
            if (serverLists.none { it.id == "my_tasks" }) {
                serverLists.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
            }

            // 4.1 Geteilte beigetretene Listen
            val sharedSql = """
                SELECT tl.id, tl.title, tl.updated_at, stl.share_token, stl.owner_id, u.full_name as owner_name, stlm.role
                FROM shared_task_list_members stlm
                JOIN shared_task_lists stl ON stlm.list_id = stl.list_id
                JOIN task_lists tl ON stl.list_id = tl.id AND stl.owner_id = tl.user_id
                JOIN users u ON stl.owner_id = u.id
                WHERE stlm.user_id = ?;
            """.trimIndent()
            conn.prepareStatement(sharedSql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val lId = rs.getString("id")
                        if (serverLists.none { it.id == lId }) {
                            serverLists.add(
                                TaskList(
                                    id = lId,
                                    title = DataEncryptionService.decrypt(rs.getString("title")),
                                    updatedAt = rs.getLong("updated_at"),
                                    isShared = true,
                                    shareToken = rs.getString("share_token"),
                                    ownerId = rs.getString("owner_id"),
                                    ownerName = rs.getString("owner_name"),
                                    role = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER)
                                )
                            )
                        }
                    }
                }
            }

            SyncTasksResponse(
                tasks = serverTasks,
                lists = serverLists,
                serverTimestamp = now
            )
        }
    }

    // ==========================================
    // SHARING FUNKTIONEN (GOOGLE DOCS STYLE)
    // ==========================================

    suspend fun getShareInfo(userId: String, listId: String): SharedTaskListInfo? = DatabaseFactory.query { conn ->
        var ownerId: String? = null
        var listTitle = ""

        // 1. Zuerst prüfen, ob der anfragende Nutzer der Eigentümer der Liste ist
        conn.prepareStatement("SELECT user_id, title FROM task_lists WHERE id = ? AND user_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    ownerId = rs.getString("user_id")
                    listTitle = if (listId == "my_tasks") "Meine Aufgaben" else DataEncryptionService.decrypt(rs.getString("title"))
                }
            }
        }

        // Falls die Standardliste "my_tasks" für diesen Nutzer noch nicht in der Datenbank existiert, anlegen!
        if (ownerId == null && listId == "my_tasks") {
            ownerId = userId
            listTitle = "Meine Aufgaben"
            val now = System.currentTimeMillis()
            conn.prepareStatement("INSERT OR IGNORE INTO task_lists (id, user_id, title, updated_at) VALUES ('my_tasks', ?, 'Meine Aufgaben', ?);").use { stmt ->
                stmt.setString(1, userId)
                stmt.setLong(2, now)
                stmt.executeUpdate()
            }
        }

        // 2. Falls nicht Eigentümer, prüfen ob der Nutzer Mitglied einer bereits geteilten Liste ist
        if (ownerId == null) {
            conn.prepareStatement("""
                SELECT stl.owner_id, tl.title 
                FROM shared_task_lists stl
                JOIN task_lists tl ON stl.list_id = tl.id AND stl.owner_id = tl.user_id
                JOIN shared_task_list_members stlm ON stl.list_id = stlm.list_id
                WHERE stl.list_id = ? AND stlm.user_id = ?;
            """.trimIndent()).use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, userId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        ownerId = rs.getString("owner_id")
                        listTitle = if (listId == "my_tasks") "Meine Aufgaben" else DataEncryptionService.decrypt(rs.getString("title"))
                    }
                }
            }
        }

        val effOwnerId = ownerId ?: return@query null

        val isOwner = effOwnerId == userId
        var callerRole: TaskMemberRole = if (isOwner) TaskMemberRole.OWNER else TaskMemberRole.VIEWER

        if (!isOwner) {
            conn.prepareStatement("SELECT role FROM shared_task_list_members WHERE list_id = ? AND user_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, userId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        callerRole = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER)
                    } else {
                        return@query null
                    }
                }
            }
        }

        var shareToken = ""
        var privacyMode = TaskPrivacyMode.RESTRICTED
        var linkRole = TaskMemberRole.VIEWER
        var exists = false

        conn.prepareStatement("SELECT share_token, privacy_mode, link_role FROM shared_task_lists WHERE list_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    exists = true
                    shareToken = rs.getString("share_token")
                    privacyMode = runCatching { TaskPrivacyMode.valueOf(rs.getString("privacy_mode")) }.getOrDefault(TaskPrivacyMode.RESTRICTED)
                    linkRole = runCatching { TaskMemberRole.valueOf(rs.getString("link_role")) }.getOrDefault(TaskMemberRole.VIEWER)
                }
            }
        }

        if (!exists) {
            if (!isOwner) return@query null
            shareToken = UUID.randomUUID().toString().replace("-", "").take(12)
            val now = System.currentTimeMillis()
            conn.prepareStatement("INSERT INTO shared_task_lists (list_id, owner_id, share_token, privacy_mode, link_role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?);").use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, effOwnerId)
                stmt.setString(3, shareToken)
                stmt.setString(4, privacyMode.name)
                stmt.setString(5, linkRole.name)
                stmt.setLong(6, now)
                stmt.setLong(7, now)
                stmt.executeUpdate()
            }
        }

        var ownerName = "Unbekannt"
        conn.prepareStatement("SELECT full_name FROM users WHERE id = ?;").use { stmt ->
            stmt.setString(1, effOwnerId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) ownerName = rs.getString("full_name")
            }
        }

        val members = mutableListOf<SharedListMember>()
        val memberSql = """
            SELECT m.user_id, m.role, m.status, u.username, u.full_name, u.email, u.class_name, u.avatar_type, u.avatar_color_hex, u.avatar_url
            FROM shared_task_list_members m
            JOIN users u ON m.user_id = u.id
            WHERE m.list_id = ?
            ORDER BY m.added_at ASC;
        """.trimIndent()
        conn.prepareStatement(memberSql).use { stmt ->
            stmt.setString(1, listId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    members.add(
                        SharedListMember(
                            userId = rs.getString("user_id"),
                            username = rs.getString("username"),
                            fullName = rs.getString("full_name"),
                            email = rs.getString("email") ?: "",
                            className = rs.getString("class_name") ?: "",
                            avatarType = rs.getString("avatar_type") ?: "initials",
                            avatarColorHex = rs.getLong("avatar_color_hex"),
                            avatarUrl = runCatching { rs.getString("avatar_url") }.getOrNull(),
                            role = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER),
                            status = runCatching { rs.getString("status") }.getOrNull() ?: "ACCEPTED"
                        )
                    )
                }
            }
        }

        SharedTaskListInfo(
            listId = listId,
            title = listTitle,
            ownerId = effOwnerId,
            ownerName = ownerName,
            shareToken = shareToken,
            shareUrl = "https://veloschulplaner.de/share/$shareToken",
            privacyMode = privacyMode,
            linkRole = linkRole,
            members = members,
            currentUserRole = callerRole
        )
    }

data class ShareSettingsUpdateResult(
    val updatedInfo: SharedTaskListInfo,
    val removedUserIds: List<String>,
    val invitedUserIds: List<String>
)

    suspend fun updateShareSettings(
        userId: String,
        listId: String,
        request: UpdateShareSettingsRequest
    ): ShareSettingsUpdateResult? {
        val resultPair = DatabaseFactory.query { conn ->
            val isOwner = conn.prepareStatement("SELECT 1 FROM task_lists WHERE id = ? AND user_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.setString(2, userId)
                stmt.executeQuery().use { rs -> rs.next() }
            }
            if (!isOwner) return@query null

            val now = System.currentTimeMillis()
            val updateSql = """
                UPDATE shared_task_lists
                SET privacy_mode = ?, link_role = ?, updated_at = ?
                WHERE list_id = ? AND owner_id = ?;
            """.trimIndent()
            conn.prepareStatement(updateSql).use { stmt ->
                stmt.setString(1, request.privacyMode.name)
                stmt.setString(2, request.linkRole.name)
                stmt.setLong(3, now)
                stmt.setString(4, listId)
                stmt.setString(5, userId)
                stmt.executeUpdate()
            }

            // Mitglieder aktualisieren
            val keepUserIds = request.members.map { it.userId }.toSet()
            val currentMemberIds = mutableListOf<String>()
            conn.prepareStatement("SELECT user_id FROM shared_task_list_members WHERE list_id = ?;").use { stmt ->
                stmt.setString(1, listId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) currentMemberIds.add(rs.getString("user_id"))
                }
            }
            val toRemove = currentMemberIds.filter { it !in keepUserIds }
            val newlyAdded = request.members.map { it.userId }.filter { it !in currentMemberIds && it != userId }

            if (toRemove.isNotEmpty()) {
                conn.prepareStatement("DELETE FROM shared_task_list_members WHERE list_id = ? AND user_id = ?;").use { stmt ->
                    for (uid in toRemove) {
                        stmt.setString(1, listId)
                        stmt.setString(2, uid)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            val upsertMemberSql = """
                INSERT INTO shared_task_list_members (list_id, user_id, role, added_at, status)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(list_id, user_id) DO UPDATE SET
                    role = excluded.role;
            """.trimIndent()
            conn.prepareStatement(upsertMemberSql).use { stmt ->
                for (m in request.members) {
                    if (m.userId != userId) {
                        val isNew = m.userId !in currentMemberIds
                        val initialStatus = if (isNew) "PENDING" else (m.status.ifBlank { "ACCEPTED" })
                        stmt.setString(1, listId)
                        stmt.setString(2, m.userId)
                        stmt.setString(3, m.role.name)
                        stmt.setLong(4, now)
                        stmt.setString(5, initialStatus)
                        stmt.addBatch()
                    }
                }
                stmt.executeBatch()
            }
            Pair(toRemove, newlyAdded)
        }

        if (resultPair == null) return null
        val info = getShareInfo(userId, listId) ?: return null
        return ShareSettingsUpdateResult(info, resultPair.first, resultPair.second)
    }

    suspend fun getPendingInvitations(userId: String): List<SharedListInvitation> = DatabaseFactory.query { conn ->
        val list = mutableListOf<SharedListInvitation>()
        val sql = """
            SELECT stlm.list_id, tl.title, stl.owner_id, u.full_name as owner_name, stl.share_token, stlm.role, stlm.added_at
            FROM shared_task_list_members stlm
            JOIN shared_task_lists stl ON stlm.list_id = stl.list_id
            JOIN task_lists tl ON stl.list_id = tl.id AND stl.owner_id = tl.user_id
            JOIN users u ON stl.owner_id = u.id
            WHERE stlm.user_id = ? AND stlm.status = 'PENDING'
            ORDER BY stlm.added_at DESC;
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, userId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val listId = rs.getString("list_id")
                    val title = DataEncryptionService.decrypt(rs.getString("title"))
                    val ownerId = rs.getString("owner_id")
                    val ownerName = rs.getString("owner_name")
                    val shareToken = rs.getString("share_token")
                    val role = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER)
                    val addedAt = rs.getLong("added_at")

                    val previewTasks = mutableListOf<TaskItem>()
                    conn.prepareStatement("SELECT * FROM tasks WHERE list_id = ? AND is_school_task = 0 ORDER BY is_completed ASC, updated_at DESC LIMIT 3;").use { tStmt ->
                        tStmt.setString(1, listId)
                        tStmt.executeQuery().use { tRs ->
                            while (tRs.next()) {
                                previewTasks.add(mapTask(tRs))
                            }
                        }
                    }

                    list.add(
                        SharedListInvitation(
                            listId = listId,
                            title = title,
                            ownerId = ownerId,
                            ownerName = ownerName,
                            shareToken = shareToken,
                            role = role,
                            createdAt = addedAt,
                            previewTasks = previewTasks
                        )
                    )
                }
            }
        }
        list
    }

    suspend fun acceptInvitation(userId: String, listId: String): Boolean = DatabaseFactory.query { conn ->
        val count = conn.prepareStatement("UPDATE shared_task_list_members SET status = 'ACCEPTED' WHERE list_id = ? AND user_id = ? AND status = 'PENDING';").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeUpdate()
        }
        count > 0
    }

    suspend fun declineInvitation(userId: String, listId: String): Boolean = DatabaseFactory.query { conn ->
        val count = conn.prepareStatement("DELETE FROM shared_task_list_members WHERE list_id = ? AND user_id = ? AND status = 'PENDING';").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeUpdate()
        }
        count > 0
    }

    suspend fun leaveSharedList(userId: String, listId: String): Boolean = DatabaseFactory.query { conn ->
        val isOwner = conn.prepareStatement("SELECT 1 FROM shared_task_lists WHERE list_id = ? AND owner_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeQuery().use { rs -> rs.next() }
        }
        if (isOwner) return@query false

        val count = conn.prepareStatement("DELETE FROM shared_task_list_members WHERE list_id = ? AND user_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.setString(2, userId)
            stmt.executeUpdate()
        }
        count > 0
    }

    suspend fun getSharedListOwnerId(listId: String): String? = DatabaseFactory.query { conn ->
        conn.prepareStatement("SELECT owner_id FROM shared_task_lists WHERE list_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getString("owner_id") else null
            }
        }
    }

    suspend fun getSharedListByToken(token: String, callerUserId: String?): SharedListDetailsDto? = DatabaseFactory.query { conn ->
        var listId: String? = null
        var ownerId: String? = null
        var privacyMode = TaskPrivacyMode.RESTRICTED
        var linkRole = TaskMemberRole.VIEWER

        conn.prepareStatement("SELECT list_id, owner_id, privacy_mode, link_role FROM shared_task_lists WHERE share_token = ?;").use { stmt ->
            stmt.setString(1, token)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    listId = rs.getString("list_id")
                    ownerId = rs.getString("owner_id")
                    privacyMode = runCatching { TaskPrivacyMode.valueOf(rs.getString("privacy_mode")) }.getOrDefault(TaskPrivacyMode.RESTRICTED)
                    linkRole = runCatching { TaskMemberRole.valueOf(rs.getString("link_role")) }.getOrDefault(TaskMemberRole.VIEWER)
                }
            }
        }
        val targetListId = listId ?: return@query null
        val effOwnerId = ownerId ?: return@query null

        var callerRole: TaskMemberRole? = null
        if (callerUserId != null) {
            if (callerUserId == effOwnerId) {
                callerRole = TaskMemberRole.OWNER
            } else {
                conn.prepareStatement("SELECT role FROM shared_task_list_members WHERE list_id = ? AND user_id = ?;").use { stmt ->
                    stmt.setString(1, targetListId)
                    stmt.setString(2, callerUserId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            callerRole = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER)
                        }
                    }
                }
            }
        }

        if (callerRole == null) {
            when (privacyMode) {
                TaskPrivacyMode.RESTRICTED -> return@query null
                TaskPrivacyMode.SCHOOL_USERS -> {
                    if (callerUserId == null) return@query null
                    callerRole = linkRole
                }
                TaskPrivacyMode.ANYONE_WITH_LINK -> {
                    callerRole = linkRole
                }
            }
        }

        val effRole = callerRole ?: TaskMemberRole.VIEWER
        val canEdit = effRole == TaskMemberRole.OWNER || effRole == TaskMemberRole.EDITOR

        var title = "Geteilte Aufgaben"
        conn.prepareStatement("SELECT title FROM task_lists WHERE id = ?;").use { stmt ->
            stmt.setString(1, targetListId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) title = DataEncryptionService.decrypt(rs.getString("title"))
            }
        }

        var ownerName = "Unbekannt"
        conn.prepareStatement("SELECT full_name FROM users WHERE id = ?;").use { stmt ->
            stmt.setString(1, effOwnerId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) ownerName = rs.getString("full_name")
            }
        }

        val tasks = mutableListOf<TaskItem>()
        val sql = "SELECT * FROM tasks WHERE list_id = ? AND is_school_task = 0 ORDER BY is_completed ASC, updated_at DESC;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, targetListId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    tasks.add(mapTask(rs))
                }
            }
        }

        val info = SharedTaskListInfo(
            listId = targetListId,
            title = title,
            ownerId = effOwnerId,
            ownerName = ownerName,
            shareToken = token,
            shareUrl = "https://veloschulplaner.de/share/$token",
            privacyMode = privacyMode,
            linkRole = linkRole,
            members = emptyList(),
            currentUserRole = effRole
        )

        SharedListDetailsDto(
            info = info,
            tasks = tasks,
            canEdit = canEdit
        )
    }

    suspend fun joinSharedList(token: String, userId: String): SharedListJoinResponse = DatabaseFactory.query { conn ->
        var listId: String? = null
        var ownerId: String? = null
        var privacyMode = TaskPrivacyMode.RESTRICTED
        var linkRole = TaskMemberRole.VIEWER

        conn.prepareStatement("SELECT list_id, owner_id, privacy_mode, link_role FROM shared_task_lists WHERE share_token = ?;").use { stmt ->
            stmt.setString(1, token)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    listId = rs.getString("list_id")
                    ownerId = rs.getString("owner_id")
                    privacyMode = runCatching { TaskPrivacyMode.valueOf(rs.getString("privacy_mode")) }.getOrDefault(TaskPrivacyMode.RESTRICTED)
                    linkRole = runCatching { TaskMemberRole.valueOf(rs.getString("link_role")) }.getOrDefault(TaskMemberRole.VIEWER)
                }
            }
        }

        val targetListId = listId ?: return@query SharedListJoinResponse(false, "Geteilte Liste nicht gefunden.")
        val effOwnerId = ownerId ?: return@query SharedListJoinResponse(false, "Eigentümer nicht gefunden.")

        if (userId == effOwnerId) {
            var title = "Meine Aufgaben"
            conn.prepareStatement("SELECT title FROM task_lists WHERE id = ?;").use { stmt ->
                stmt.setString(1, targetListId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) title = DataEncryptionService.decrypt(rs.getString("title"))
                }
            }
            return@query SharedListJoinResponse(
                success = true,
                message = "Du bist der Eigentümer dieser Liste.",
                list = TaskList(id = targetListId, title = title, isShared = true, shareToken = token, ownerId = effOwnerId, role = TaskMemberRole.OWNER)
            )
        }

        var existingRole: TaskMemberRole? = null
        conn.prepareStatement("SELECT role FROM shared_task_list_members WHERE list_id = ? AND user_id = ?;").use { stmt ->
            stmt.setString(1, targetListId)
            stmt.setString(2, userId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    existingRole = runCatching { TaskMemberRole.valueOf(rs.getString("role")) }.getOrDefault(TaskMemberRole.VIEWER)
                }
            }
        }

        val effectiveRole = if (existingRole != null) {
            conn.prepareStatement("UPDATE shared_task_list_members SET status = 'ACCEPTED' WHERE list_id = ? AND user_id = ?;").use { stmt ->
                stmt.setString(1, targetListId)
                stmt.setString(2, userId)
                stmt.executeUpdate()
            }
            existingRole
        } else {
            if (privacyMode == TaskPrivacyMode.RESTRICTED) {
                return@query SharedListJoinResponse(false, "Diese Liste ist eingeschränkt. Bitte den Eigentümer um Freigabe.")
            }
            val now = System.currentTimeMillis()
            conn.prepareStatement("INSERT OR REPLACE INTO shared_task_list_members (list_id, user_id, role, added_at, status) VALUES (?, ?, ?, ?, 'ACCEPTED');").use { stmt ->
                stmt.setString(1, targetListId)
                stmt.setString(2, userId)
                stmt.setString(3, linkRole.name)
                stmt.setLong(4, now)
                stmt.executeUpdate()
            }
            linkRole
        }

        var title = "Geteilte Aufgaben"
        conn.prepareStatement("SELECT title FROM task_lists WHERE id = ?;").use { stmt ->
            stmt.setString(1, targetListId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) title = DataEncryptionService.decrypt(rs.getString("title"))
            }
        }
        var ownerName = "Unbekannt"
        conn.prepareStatement("SELECT full_name FROM users WHERE id = ?;").use { stmt ->
            stmt.setString(1, effOwnerId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) ownerName = rs.getString("full_name")
            }
        }

        val tasks = mutableListOf<TaskItem>()
        conn.prepareStatement("SELECT * FROM tasks WHERE list_id = ? AND is_school_task = 0 ORDER BY is_completed ASC, updated_at DESC;").use { stmt ->
            stmt.setString(1, targetListId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) tasks.add(mapTask(rs))
            }
        }

        SharedListJoinResponse(
            success = true,
            message = "Erfolgreich beigetreten.",
            list = TaskList(
                id = targetListId,
                title = title,
                isShared = true,
                shareToken = token,
                ownerId = effOwnerId,
                ownerName = ownerName,
                role = effectiveRole
            ),
            tasks = tasks
        )
    }

    suspend fun getSharedListMemberAndOwnerIds(listId: String): List<String> = DatabaseFactory.query { conn ->
        val ids = mutableListOf<String>()
        conn.prepareStatement("SELECT owner_id FROM shared_task_lists WHERE list_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) ids.add(rs.getString("owner_id"))
            }
        }
        conn.prepareStatement("SELECT user_id FROM shared_task_list_members WHERE list_id = ?;").use { stmt ->
            stmt.setString(1, listId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) ids.add(rs.getString("user_id"))
            }
        }
        ids.distinct()
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

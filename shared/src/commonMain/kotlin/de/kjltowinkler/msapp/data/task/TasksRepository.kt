package de.kjltowinkler.msapp.data.task

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object TasksRepository {
    private val _tasks = mutableStateListOf<TaskItem>()
    val tasks: List<TaskItem> get() = _tasks

    val privateLists = mutableStateListOf(
        TaskList(id = "my_tasks", title = "Meine Aufgaben")
    )

    var myTasksTitle: String
        get() = privateLists.firstOrNull { it.id == "my_tasks" }?.title ?: "Meine Aufgaben"
        set(value) {
            val index = privateLists.indexOfFirst { it.id == "my_tasks" }
            if (index != -1) {
                privateLists[index] = privateLists[index].copy(title = value)
            }
        }

    init {
        _tasks.addAll(
            listOf(
                TaskItem(
                    id = "sample_1",
                    title = "Bis",
                    description = "Nö",
                    isCompleted = false,
                    createdDate = "Di., 13. Okt.",
                    deadlineDate = "Do., 15. Okt.",
                    isStarred = true,
                    isSchoolTask = true
                ),
                TaskItem(
                    id = "sample_2",
                    title = "Yes",
                    description = "",
                    isCompleted = false,
                    createdDate = "Di., 13. Okt.",
                    isStarred = true,
                    isSchoolTask = true
                ),
                TaskItem(
                    id = "sample_3",
                    title = "Yes",
                    description = "",
                    isCompleted = false,
                    createdDate = "Di., 13. Okt.",
                    isStarred = true,
                    isSchoolTask = false
                ),
                TaskItem(
                    id = "sample_4",
                    title = "Hausaufgaben Deutsch",
                    description = "S. 42 Nr. 3",
                    isCompleted = false,
                    createdDate = "Di., 13. Okt.",
                    dueDate = "15.10.2026",
                    deadlineDate = "Do., 15. Okt.",
                    isStarred = false,
                    isSchoolTask = true
                ),
                TaskItem(
                    id = "sample_5",
                    title = "VG1",
                    description = "",
                    isCompleted = true,
                    createdDate = "Di., 13. Okt.",
                    completedDate = "Mi., 14. Okt.",
                    deadlineDate = "Do., 15. Okt.",
                    isStarred = true,
                    isSchoolTask = true
                ),
                TaskItem(
                    id = "sample_6",
                    title = "VG2",
                    description = "",
                    isCompleted = true,
                    createdDate = "Di., 13. Okt.",
                    completedDate = "Mi., 14. Okt.",
                    isStarred = true,
                    isSchoolTask = false
                )
            )
        )
    }

    fun addTask(task: TaskItem) {
        val taskWithDate = if (task.createdDate.isBlank()) {
            task.copy(createdDate = getTodayDate().formattedCompletedGerman())
        } else task
        _tasks.add(0, taskWithDate)
    }

    fun updateTask(task: TaskItem) {
        val index = _tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            _tasks[index] = task
        }
    }

    fun toggleStarred(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val current = _tasks[index]
            _tasks[index] = current.copy(isStarred = !current.isStarred)
        }
    }

    fun toggleCompleted(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val current = _tasks[index]
            val willBeCompleted = !current.isCompleted
            val completedDate = if (willBeCompleted) getTodayDate().formattedCompletedGerman() else ""
            _tasks[index] = current.copy(
                isCompleted = willBeCompleted,
                completedDate = completedDate
            )
        }
    }

    fun deleteTask(taskId: String) {
        val task = _tasks.find { it.id == taskId }
        if (task?.isSchoolTask == true) {
            return
        }
        _tasks.removeAll { it.id == taskId }
    }

    fun deleteCompletedTasks(isSchoolTask: Boolean = false) {
        _tasks.removeAll { it.isSchoolTask == isSchoolTask && it.isCompleted }
    }

    fun deleteAllMyTasks() {
        _tasks.removeAll { !it.isSchoolTask }
    }

    fun createPrivateList(title: String): TaskList {
        val id = "list_${(100000..999999).random()}"
        val newList = TaskList(id = id, title = title.trim())
        privateLists.add(newList)
        return newList
    }

    fun renamePrivateList(listId: String, newTitle: String) {
        val index = privateLists.indexOfFirst { it.id == listId }
        if (index != -1) {
            privateLists[index] = privateLists[index].copy(title = newTitle.trim())
        }
    }

    fun deletePrivateList(listId: String) {
        if (listId == "my_tasks") {
            deleteAllMyTasks()
            return
        }
        _tasks.removeAll { it.listId == listId }
        privateLists.removeAll { it.id == listId }
    }

    fun deleteCompletedTasksInList(listId: String) {
        if (listId == "my_tasks") {
            _tasks.removeAll { !it.isSchoolTask && (it.listId == "my_tasks" || it.listId.isEmpty()) && it.isCompleted }
        } else {
            _tasks.removeAll { it.listId == listId && it.isCompleted }
        }
    }

    fun deleteAllTasksInList(listId: String) {
        if (listId == "my_tasks") {
            _tasks.removeAll { !it.isSchoolTask && (it.listId == "my_tasks" || it.listId.isEmpty()) }
        } else {
            _tasks.removeAll { it.listId == listId }
        }
    }

    fun markAllTasksInListCompleted(listId: String) {
        val today = getTodayDate().formattedCompletedGerman()
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && !task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = true, completedDate = today)
            }
        }
    }

    fun markAllTasksInListUncompleted(listId: String) {
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = false, completedDate = "")
            }
        }
    }

    fun markAllTasksInListStarred(listId: String) {
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && !task.isStarred) {
                _tasks[index] = task.copy(isStarred = true)
            }
        }
    }

    fun unstarAllTasksInList(listId: String) {
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && task.isStarred) {
                _tasks[index] = task.copy(isStarred = false)
            }
        }
    }

    fun markAllSchoolTasksStarred() {
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && !task.isStarred) {
                _tasks[index] = task.copy(isStarred = true)
            }
        }
    }

    fun unstarAllSchoolTasks() {
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && task.isStarred) {
                _tasks[index] = task.copy(isStarred = false)
            }
        }
    }

    fun markAllSchoolTasksCompleted() {
        val today = getTodayDate().formattedCompletedGerman()
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && !task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = true, completedDate = today)
            }
        }
    }

    fun markAllSchoolTasksUncompleted() {
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = false, completedDate = "")
            }
        }
    }

    fun deleteCompletedStarredTasks() {
        _tasks.removeAll { it.isStarred && it.isCompleted && !it.isSchoolTask }
    }

    fun deleteAllStarredTasks() {
        // Markierte Schulaufgaben können nicht gelöscht werden
        _tasks.removeAll { it.isStarred && !it.isSchoolTask }
    }

    fun unstarAllTasks() {
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred) {
                _tasks[index] = task.copy(isStarred = false)
            }
        }
    }

    fun markAllStarredTasksCompleted() {
        val today = getTodayDate().formattedCompletedGerman()
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred && !task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = true, completedDate = today)
            }
        }
    }

    fun markAllStarredTasksUncompleted() {
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred && task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = false, completedDate = "")
            }
        }
    }

    /**
     * Synchronisiert alle Aufgaben und Listen mit dem Ktor-Backend.
     */
    suspend fun syncWithServer(): Boolean {
        if (!de.kjltowinkler.msapp.data.backend.BackendApiClient.isLoggedIn) return false
        val result = de.kjltowinkler.msapp.data.backend.BackendApiClient.syncTasks(_tasks.toList(), privateLists.toList())
        if (result.isSuccess) {
            val response = result.getOrNull() ?: return false
            _tasks.clear()
            _tasks.addAll(response.tasks)

            privateLists.clear()
            privateLists.addAll(response.lists)
            return true
        }
        return false
    }
}

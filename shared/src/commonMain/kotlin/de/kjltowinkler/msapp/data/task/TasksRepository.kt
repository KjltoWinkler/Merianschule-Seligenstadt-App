package de.kjltowinkler.msapp.data.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TasksRepository {
    private const val KEY_CACHED_TASKS = "cached_tasks"
    private const val KEY_CACHED_LISTS = "cached_task_lists"
    private const val KEY_PENDING_DEL_TASKS = "pending_deleted_tasks"
    private const val KEY_PENDING_DEL_LISTS = "pending_deleted_lists"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _tasks = mutableStateListOf<TaskItem>()
    val tasks: List<TaskItem> get() = _tasks

    val privateLists = mutableStateListOf(
        TaskList(id = "my_tasks", title = "Meine Aufgaben")
    )

    private val _pendingDeletedTaskIds = mutableSetOf<String>()
    private val _pendingDeletedListIds = mutableSetOf<String>()

    var isSyncing by mutableStateOf(false)
        private set

    var lastSyncError by mutableStateOf<String?>(null)
        private set

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncJob: Job? = null

    var myTasksTitle: String
        get() = privateLists.firstOrNull { it.id == "my_tasks" }?.title ?: "Meine Aufgaben"
        set(value) {
            val index = privateLists.indexOfFirst { it.id == "my_tasks" }
            if (index != -1) {
                privateLists[index] = privateLists[index].copy(title = value)
                saveToCache()
                triggerBackgroundSync()
            }
        }

    init {
        val loaded = loadFromCache()
        if (!loaded) {
            if (!BackendApiClient.isLoggedIn) {
                _tasks.addAll(getSampleTasks())
            }
        }
        if (BackendApiClient.isLoggedIn) {
            triggerBackgroundSync(debounceMs = 0)
        }
    }

    private fun getSampleTasks(): List<TaskItem> = listOf(
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

    private fun saveToCache() {
        runCatching {
            PlatformStorage.setString(KEY_CACHED_TASKS, json.encodeToString(_tasks.toList()))
            PlatformStorage.setString(KEY_CACHED_LISTS, json.encodeToString(privateLists.toList()))
            PlatformStorage.setString(KEY_PENDING_DEL_TASKS, json.encodeToString(_pendingDeletedTaskIds.toList()))
            PlatformStorage.setString(KEY_PENDING_DEL_LISTS, json.encodeToString(_pendingDeletedListIds.toList()))
        }
    }

    private fun loadFromCache(): Boolean {
        return runCatching {
            val delTasksJson = PlatformStorage.getString(KEY_PENDING_DEL_TASKS)
            val delListsJson = PlatformStorage.getString(KEY_PENDING_DEL_LISTS)
            if (!delTasksJson.isNullOrBlank()) {
                val ids = json.decodeFromString<List<String>>(delTasksJson)
                _pendingDeletedTaskIds.addAll(ids)
            }
            if (!delListsJson.isNullOrBlank()) {
                val ids = json.decodeFromString<List<String>>(delListsJson)
                _pendingDeletedListIds.addAll(ids)
            }

            val listsJson = PlatformStorage.getString(KEY_CACHED_LISTS)
            if (!listsJson.isNullOrBlank()) {
                val loadedLists = json.decodeFromString<List<TaskList>>(listsJson)
                privateLists.clear()
                privateLists.addAll(loadedLists)
                if (privateLists.none { it.id == "my_tasks" }) {
                    privateLists.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
                }
            }

            val tasksJson = PlatformStorage.getString(KEY_CACHED_TASKS)
            if (!tasksJson.isNullOrBlank()) {
                val loadedTasks = json.decodeFromString<List<TaskItem>>(tasksJson)
                _tasks.clear()
                _tasks.addAll(loadedTasks)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun triggerBackgroundSync(debounceMs: Long = 500) {
        if (!BackendApiClient.isLoggedIn) return
        syncJob?.cancel()
        syncJob = repositoryScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            syncWithServer()
        }
    }

    /**
     * Synchronisiert alle Aufgaben und Listen mit dem Ktor-Backend.
     */
    suspend fun syncWithServer(): Boolean {
        if (!BackendApiClient.isLoggedIn) return false
        isSyncing = true
        lastSyncError = null
        return try {
            val currentTasks = _tasks.filterNot { it.id.startsWith("sample_") }
            val currentLists = privateLists.toList()
            val delTasks = _pendingDeletedTaskIds.toList()
            val delLists = _pendingDeletedListIds.toList()

            val result = BackendApiClient.syncTasks(
                tasks = currentTasks,
                lists = currentLists,
                deletedTaskIds = delTasks,
                deletedListIds = delLists
            )
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    _pendingDeletedTaskIds.removeAll(delTasks.toSet())
                    _pendingDeletedListIds.removeAll(delLists.toSet())

                    _tasks.clear()
                    _tasks.addAll(response.tasks)

                    privateLists.clear()
                    privateLists.addAll(response.lists)
                    if (privateLists.none { it.id == "my_tasks" }) {
                        privateLists.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
                    }
                    saveToCache()
                    true
                } else false
            } else {
                lastSyncError = result.exceptionOrNull()?.message
                false
            }
        } catch (e: Exception) {
            lastSyncError = e.message
            false
        } finally {
            isSyncing = false
        }
    }

    fun clearAndReset() {
        syncJob?.cancel()
        _tasks.clear()
        privateLists.clear()
        privateLists.add(TaskList(id = "my_tasks", title = "Meine Aufgaben"))
        _pendingDeletedTaskIds.clear()
        _pendingDeletedListIds.clear()
        PlatformStorage.remove(KEY_CACHED_TASKS)
        PlatformStorage.remove(KEY_CACHED_LISTS)
        PlatformStorage.remove(KEY_PENDING_DEL_TASKS)
        PlatformStorage.remove(KEY_PENDING_DEL_LISTS)
        _tasks.addAll(getSampleTasks())
    }

    fun addTask(task: TaskItem) {
        val taskWithDate = if (task.createdDate.isBlank()) {
            task.copy(createdDate = getTodayDate().formattedCompletedGerman())
        } else task
        _tasks.add(0, taskWithDate)
        saveToCache()
        triggerBackgroundSync()
    }

    fun updateTask(task: TaskItem) {
        val index = _tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            _tasks[index] = task
            saveToCache()
            triggerBackgroundSync()
        }
    }

    fun toggleStarred(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val current = _tasks[index]
            _tasks[index] = current.copy(isStarred = !current.isStarred)
            saveToCache()
            triggerBackgroundSync()
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
            saveToCache()
            triggerBackgroundSync()
        }
    }

    fun deleteTask(taskId: String) {
        val task = _tasks.find { it.id == taskId }
        if (task?.isSchoolTask == true) {
            return
        }
        if (!taskId.startsWith("sample_")) {
            _pendingDeletedTaskIds.add(taskId)
        }
        _tasks.removeAll { it.id == taskId }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteCompletedTasks(isSchoolTask: Boolean = false) {
        val toDelete = _tasks.filter { it.isSchoolTask == isSchoolTask && it.isCompleted }
        toDelete.forEach {
            if (!it.isSchoolTask && !it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        _tasks.removeAll { it.isSchoolTask == isSchoolTask && it.isCompleted }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteAllMyTasks() {
        val toDelete = _tasks.filter { !it.isSchoolTask }
        toDelete.forEach {
            if (!it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        _tasks.removeAll { !it.isSchoolTask }
        saveToCache()
        triggerBackgroundSync()
    }

    fun createPrivateList(title: String): TaskList {
        val id = "list_${(100000..999999).random()}"
        val newList = TaskList(id = id, title = title.trim())
        privateLists.add(newList)
        saveToCache()
        triggerBackgroundSync()
        return newList
    }

    fun renamePrivateList(listId: String, newTitle: String) {
        val index = privateLists.indexOfFirst { it.id == listId }
        if (index != -1) {
            privateLists[index] = privateLists[index].copy(title = newTitle.trim())
            saveToCache()
            triggerBackgroundSync()
        }
    }

    fun deletePrivateList(listId: String) {
        if (listId == "my_tasks") {
            deleteAllMyTasks()
            return
        }
        val toDeleteTasks = _tasks.filter { it.listId == listId }
        toDeleteTasks.forEach {
            if (!it.isSchoolTask && !it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        _tasks.removeAll { it.listId == listId }
        privateLists.removeAll { it.id == listId }
        _pendingDeletedListIds.add(listId)
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteCompletedTasksInList(listId: String) {
        val toDelete = if (listId == "my_tasks") {
            _tasks.filter { !it.isSchoolTask && (it.listId == "my_tasks" || it.listId.isEmpty()) && it.isCompleted }
        } else {
            _tasks.filter { it.listId == listId && it.isCompleted }
        }
        toDelete.forEach {
            if (!it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        if (listId == "my_tasks") {
            _tasks.removeAll { !it.isSchoolTask && (it.listId == "my_tasks" || it.listId.isEmpty()) && it.isCompleted }
        } else {
            _tasks.removeAll { it.listId == listId && it.isCompleted }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteAllTasksInList(listId: String) {
        val toDelete = if (listId == "my_tasks") {
            _tasks.filter { !it.isSchoolTask && (it.listId == "my_tasks" || it.listId.isEmpty()) }
        } else {
            _tasks.filter { it.listId == listId }
        }
        toDelete.forEach {
            if (!it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        if (listId == "my_tasks") {
            _tasks.removeAll { !it.isSchoolTask && (it.listId == "my_tasks" || it.listId.isEmpty()) }
        } else {
            _tasks.removeAll { it.listId == listId }
        }
        saveToCache()
        triggerBackgroundSync()
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
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllTasksInListUncompleted(listId: String) {
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = false, completedDate = "")
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllTasksInListStarred(listId: String) {
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && !task.isStarred) {
                _tasks[index] = task.copy(isStarred = true)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun unstarAllTasksInList(listId: String) {
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && task.isStarred) {
                _tasks[index] = task.copy(isStarred = false)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllSchoolTasksStarred() {
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && !task.isStarred) {
                _tasks[index] = task.copy(isStarred = true)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun unstarAllSchoolTasks() {
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && task.isStarred) {
                _tasks[index] = task.copy(isStarred = false)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllSchoolTasksCompleted() {
        val today = getTodayDate().formattedCompletedGerman()
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && !task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = true, completedDate = today)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllSchoolTasksUncompleted() {
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = false, completedDate = "")
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteCompletedStarredTasks() {
        val toDelete = _tasks.filter { it.isStarred && it.isCompleted && !it.isSchoolTask }
        toDelete.forEach {
            if (!it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        _tasks.removeAll { it.isStarred && it.isCompleted && !it.isSchoolTask }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteAllStarredTasks() {
        val toDelete = _tasks.filter { it.isStarred && !it.isSchoolTask }
        toDelete.forEach {
            if (!it.id.startsWith("sample_")) {
                _pendingDeletedTaskIds.add(it.id)
            }
        }
        _tasks.removeAll { it.isStarred && !it.isSchoolTask }
        saveToCache()
        triggerBackgroundSync()
    }

    fun unstarAllTasks() {
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred) {
                _tasks[index] = task.copy(isStarred = false)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllStarredTasksCompleted() {
        val today = getTodayDate().formattedCompletedGerman()
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred && !task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = true, completedDate = today)
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllStarredTasksUncompleted() {
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred && task.isCompleted) {
                _tasks[index] = task.copy(isCompleted = false, completedDate = "")
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }
}

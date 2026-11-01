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
import kotlinx.coroutines.sync.Mutex
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
    private val _dirtyTaskIds = mutableSetOf<String>()
    private val _dirtyListIds = mutableSetOf<String>()

    var isSyncing by mutableStateOf(false)
        private set

    var lastSyncError by mutableStateOf<String?>(null)
        private set

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncJob: Job? = null
    private val syncMutex = Mutex()
    private var hasPendingSync = false

    var myTasksTitle: String
        get() = privateLists.firstOrNull { it.id == "my_tasks" }?.title ?: "Meine Aufgaben"
        set(value) {
            val index = privateLists.indexOfFirst { it.id == "my_tasks" }
            if (index != -1) {
                val now = getCurrentEpochMillis()
                privateLists[index] = privateLists[index].copy(title = value, updatedAt = now)
                _dirtyListIds.add("my_tasks")
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
            startRealtimeSync()
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

    fun triggerBackgroundSync(debounceMs: Long = 300) {
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
        if (!syncMutex.tryLock()) {
            hasPendingSync = true
            return false
        }
        isSyncing = true
        lastSyncError = null
        return try {
            val currentTasks = _tasks.toList()
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
                    _dirtyTaskIds.clear()
                    _dirtyListIds.clear()

                    // In-place Abgleich, damit kein Flackern oder Statusverlust in Compose entsteht
                    val serverIds = response.tasks.map { it.id }.toSet()
                    _tasks.removeAll { !serverIds.contains(it.id) }
                    for (serverTask in response.tasks) {
                        val index = _tasks.indexOfFirst { it.id == serverTask.id }
                        if (index != -1) {
                            if (_tasks[index] != serverTask) {
                                _tasks[index] = serverTask
                            }
                        } else {
                            _tasks.add(serverTask)
                        }
                    }

                    val serverListIds = response.lists.map { it.id }.toSet()
                    privateLists.removeAll { it.id != "my_tasks" && !serverListIds.contains(it.id) }
                    for (serverList in response.lists) {
                        val index = privateLists.indexOfFirst { it.id == serverList.id }
                        if (index != -1) {
                            if (privateLists[index] != serverList) {
                                privateLists[index] = serverList
                            }
                        } else {
                            privateLists.add(serverList)
                        }
                    }
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
            syncMutex.unlock()
            if (hasPendingSync) {
                hasPendingSync = false
                triggerBackgroundSync(debounceMs = 100)
            }
        }
    }

    /**
     * Zieht die neuesten Aufgaben und Listen vom Server (z.B. ausgelöst durch ein WebSocket-Event eines anderen Geräts),
     * ohne lokale ungespeicherte Daten blind zu überschreiben oder einen Push-Loop auszulösen.
     */
    suspend fun pullFromServer(): Boolean {
        if (!BackendApiClient.isLoggedIn) return false
        if (!syncMutex.tryLock()) {
            return false
        }
        return try {
            val tasksResult = BackendApiClient.fetchTasks()
            val listsResult = BackendApiClient.fetchTaskLists()
            if (tasksResult.isSuccess && listsResult.isSuccess) {
                val serverTasks = tasksResult.getOrNull() ?: emptyList()
                val serverLists = listsResult.getOrNull() ?: emptyList()

                val serverIds = serverTasks.map { it.id }.toSet()
                _tasks.removeAll { !serverIds.contains(it.id) && !_dirtyTaskIds.contains(it.id) }
                for (serverTask in serverTasks) {
                    val index = _tasks.indexOfFirst { it.id == serverTask.id }
                    if (index != -1) {
                        val local = _tasks[index]
                        if (!_dirtyTaskIds.contains(local.id) || serverTask.updatedAt > local.updatedAt) {
                            if (local != serverTask) {
                                _tasks[index] = serverTask
                                _dirtyTaskIds.remove(local.id)
                            }
                        }
                    } else {
                        _tasks.add(serverTask)
                    }
                }

                val serverListIds = serverLists.map { it.id }.toSet()
                privateLists.removeAll { it.id != "my_tasks" && !serverListIds.contains(it.id) && !_dirtyListIds.contains(it.id) }
                for (serverList in serverLists) {
                    val index = privateLists.indexOfFirst { it.id == serverList.id }
                    if (index != -1) {
                        val local = privateLists[index]
                        if (!_dirtyListIds.contains(local.id) || serverList.updatedAt > local.updatedAt) {
                            if (local != serverList) {
                                privateLists[index] = serverList
                                _dirtyListIds.remove(local.id)
                            }
                        }
                    } else {
                        privateLists.add(serverList)
                    }
                }
                if (privateLists.none { it.id == "my_tasks" }) {
                    privateLists.add(0, TaskList(id = "my_tasks", title = "Meine Aufgaben"))
                }
                saveToCache()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        } finally {
            syncMutex.unlock()
        }
    }

    /**
     * Startet die permanente WebSocket-Echtzeitsynchronisation.
     */
    fun startRealtimeSync() {
        if (!BackendApiClient.isLoggedIn) return
        BackendApiClient.startRealtimeTaskUpdates {
            pullFromServer()
        }
    }

    fun stopRealtimeSync() {
        BackendApiClient.stopRealtimeTaskUpdates()
    }

    fun clearAndReset() {
        stopRealtimeSync()
        syncJob?.cancel()
        _tasks.clear()
        privateLists.clear()
        privateLists.add(TaskList(id = "my_tasks", title = "Meine Aufgaben"))
        _pendingDeletedTaskIds.clear()
        _pendingDeletedListIds.clear()
        _dirtyTaskIds.clear()
        _dirtyListIds.clear()
        hasPendingSync = false
        PlatformStorage.remove(KEY_CACHED_TASKS)
        PlatformStorage.remove(KEY_CACHED_LISTS)
        PlatformStorage.remove(KEY_PENDING_DEL_TASKS)
        PlatformStorage.remove(KEY_PENDING_DEL_LISTS)
        _tasks.addAll(getSampleTasks())
    }

    fun addTask(task: TaskItem) {
        val now = getCurrentEpochMillis()
        val taskWithDate = if (task.createdDate.isBlank()) {
            task.copy(createdDate = getTodayDate().formattedCompletedGerman(), updatedAt = now)
        } else task.copy(updatedAt = now)
        _dirtyTaskIds.add(taskWithDate.id)
        _tasks.add(0, taskWithDate)
        saveToCache()
        triggerBackgroundSync()
    }

    fun updateTask(task: TaskItem) {
        val index = _tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            val now = getCurrentEpochMillis()
            val updated = task.copy(updatedAt = now)
            _dirtyTaskIds.add(updated.id)
            _tasks[index] = updated
            saveToCache()
            triggerBackgroundSync()
        }
    }

    fun toggleStarred(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val current = _tasks[index]
            val now = getCurrentEpochMillis()
            val updated = current.copy(isStarred = !current.isStarred, updatedAt = now)
            _dirtyTaskIds.add(updated.id)
            _tasks[index] = updated
            saveToCache()
            triggerBackgroundSync()
        }
    }

    fun toggleCompleted(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val current = _tasks[index]
            val now = getCurrentEpochMillis()
            val willBeCompleted = !current.isCompleted
            val completedDate = if (willBeCompleted) getTodayDate().formattedCompletedGerman() else ""
            val updated = current.copy(
                isCompleted = willBeCompleted,
                completedDate = completedDate,
                updatedAt = now
            )
            _dirtyTaskIds.add(updated.id)
            _tasks[index] = updated
            saveToCache()
            triggerBackgroundSync()
        }
    }

    fun deleteTask(taskId: String) {
        val task = _tasks.find { it.id == taskId }
        if (task?.isSchoolTask == true) {
            return
        }
        _pendingDeletedTaskIds.add(taskId)
        _dirtyTaskIds.remove(taskId)
        _tasks.removeAll { it.id == taskId }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteCompletedTasks(isSchoolTask: Boolean = false) {
        val toDelete = _tasks.filter { it.isSchoolTask == isSchoolTask && it.isCompleted }
        toDelete.forEach {
            if (!it.isSchoolTask) {
                _pendingDeletedTaskIds.add(it.id)
                _dirtyTaskIds.remove(it.id)
            }
        }
        _tasks.removeAll { it.isSchoolTask == isSchoolTask && it.isCompleted }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteAllMyTasks() {
        val toDelete = _tasks.filter { !it.isSchoolTask }
        toDelete.forEach {
            _pendingDeletedTaskIds.add(it.id)
            _dirtyTaskIds.remove(it.id)
        }
        _tasks.removeAll { !it.isSchoolTask }
        saveToCache()
        triggerBackgroundSync()
    }

    fun createPrivateList(title: String): TaskList {
        val now = getCurrentEpochMillis()
        val id = "list_${(100000..999999).random()}"
        val newList = TaskList(id = id, title = title.trim(), updatedAt = now)
        _dirtyListIds.add(newList.id)
        privateLists.add(newList)
        saveToCache()
        triggerBackgroundSync()
        return newList
    }

    fun renamePrivateList(listId: String, newTitle: String) {
        val index = privateLists.indexOfFirst { it.id == listId }
        if (index != -1) {
            val now = getCurrentEpochMillis()
            val updated = privateLists[index].copy(title = newTitle.trim(), updatedAt = now)
            _dirtyListIds.add(updated.id)
            privateLists[index] = updated
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
            if (!it.isSchoolTask) {
                _pendingDeletedTaskIds.add(it.id)
                _dirtyTaskIds.remove(it.id)
            }
        }
        _tasks.removeAll { it.listId == listId }
        privateLists.removeAll { it.id == listId }
        _dirtyListIds.remove(listId)
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
            _pendingDeletedTaskIds.add(it.id)
            _dirtyTaskIds.remove(it.id)
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
            _pendingDeletedTaskIds.add(it.id)
            _dirtyTaskIds.remove(it.id)
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
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && !task.isCompleted) {
                val updated = task.copy(isCompleted = true, completedDate = today, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllTasksInListUncompleted(listId: String) {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && task.isCompleted) {
                val updated = task.copy(isCompleted = false, completedDate = "", updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllTasksInListStarred(listId: String) {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && !task.isStarred) {
                val updated = task.copy(isStarred = true, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun unstarAllTasksInList(listId: String) {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            val matches = if (listId == "my_tasks") (!task.isSchoolTask && (task.listId == "my_tasks" || task.listId.isEmpty()))
                          else (!task.isSchoolTask && task.listId == listId)
            if (matches && task.isStarred) {
                val updated = task.copy(isStarred = false, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllSchoolTasksStarred() {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && !task.isStarred) {
                val updated = task.copy(isStarred = true, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun unstarAllSchoolTasks() {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && task.isStarred) {
                val updated = task.copy(isStarred = false, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllSchoolTasksCompleted() {
        val today = getTodayDate().formattedCompletedGerman()
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && !task.isCompleted) {
                val updated = task.copy(isCompleted = true, completedDate = today, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllSchoolTasksUncompleted() {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isSchoolTask && task.isCompleted) {
                val updated = task.copy(isCompleted = false, completedDate = "", updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteCompletedStarredTasks() {
        val toDelete = _tasks.filter { it.isStarred && it.isCompleted && !it.isSchoolTask }
        toDelete.forEach {
            _pendingDeletedTaskIds.add(it.id)
            _dirtyTaskIds.remove(it.id)
        }
        _tasks.removeAll { it.isStarred && it.isCompleted && !it.isSchoolTask }
        saveToCache()
        triggerBackgroundSync()
    }

    fun deleteAllStarredTasks() {
        val toDelete = _tasks.filter { it.isStarred && !it.isSchoolTask }
        toDelete.forEach {
            _pendingDeletedTaskIds.add(it.id)
            _dirtyTaskIds.remove(it.id)
        }
        _tasks.removeAll { it.isStarred && !it.isSchoolTask }
        saveToCache()
        triggerBackgroundSync()
    }

    fun unstarAllTasks() {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred) {
                val updated = task.copy(isStarred = false, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllStarredTasksCompleted() {
        val today = getTodayDate().formattedCompletedGerman()
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred && !task.isCompleted) {
                val updated = task.copy(isCompleted = true, completedDate = today, updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }

    fun markAllStarredTasksUncompleted() {
        val now = getCurrentEpochMillis()
        _tasks.forEachIndexed { index, task ->
            if (task.isStarred && task.isCompleted) {
                val updated = task.copy(isCompleted = false, completedDate = "", updatedAt = now)
                _dirtyTaskIds.add(updated.id)
                _tasks[index] = updated
            }
        }
        saveToCache()
        triggerBackgroundSync()
    }
}

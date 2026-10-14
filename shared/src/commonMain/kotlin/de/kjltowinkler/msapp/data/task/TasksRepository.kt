package de.kjltowinkler.msapp.data.task

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object TasksRepository {
    private val _tasks = mutableStateListOf<TaskItem>()
    val tasks: List<TaskItem> get() = _tasks

    var myTasksTitle by mutableStateOf("Meine Aufgaben")

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
        _tasks.removeAll { it.id == taskId }
    }

    fun deleteCompletedTasks(isSchoolTask: Boolean = false) {
        _tasks.removeAll { it.isSchoolTask == isSchoolTask && it.isCompleted }
    }

    fun deleteAllMyTasks() {
        _tasks.removeAll { !it.isSchoolTask }
    }
}

package de.kjltowinkler.msapp.data.task

import kotlinx.serialization.Serializable

@Serializable
enum class TaskCategory {
    STARRED,
    SCHOOL,
    MY_TASKS
}

@Serializable
data class TaskList(
    val id: String,
    val title: String
)

@Serializable
data class TaskItem(
    val id: String,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val dueTime: String = "",
    val isCompleted: Boolean = false,
    val completedDate: String = "",
    val createdDate: String = "",
    val deadlineDate: String = "",
    val isStarred: Boolean = false,
    val isSchoolTask: Boolean = false,
    val listId: String = "my_tasks"
)

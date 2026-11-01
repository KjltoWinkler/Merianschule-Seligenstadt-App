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
    val title: String,
    val updatedAt: Long = 0L
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
    val listId: String = "my_tasks",
    val updatedAt: Long = 0L
)

/**
 * Repräsentiert eine offizielle Schulaufgabe / Hausaufgabe, die von Lehrkräften oder Admins
 * für eine Schulklasse im Admin-Panel angelegt und gepflegt wird.
 */
@Serializable
data class SchoolTaskItem(
    val id: String = "",
    val title: String,
    val description: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val dueTime: String = "",
    val deadlineDate: String = "",
    val createdDate: String = "",
    val targetClass: String = "09AR",
    val creatorName: String = "Lehrkraft",
    val updatedAt: Long = 0L
)

fun SchoolTaskItem.toTaskItem(
    isCompleted: Boolean = false,
    completedDate: String = "",
    isStarred: Boolean = false
): TaskItem = TaskItem(
    id = id,
    title = title,
    description = description,
    subject = subject,
    dueDate = dueDate,
    dueTime = dueTime,
    deadlineDate = deadlineDate,
    createdDate = createdDate,
    isCompleted = isCompleted,
    completedDate = completedDate,
    isStarred = isStarred,
    isSchoolTask = true,
    listId = "my_tasks",
    updatedAt = updatedAt
)

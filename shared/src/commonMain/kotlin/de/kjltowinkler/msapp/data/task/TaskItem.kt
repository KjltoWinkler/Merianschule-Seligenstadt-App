package de.kjltowinkler.msapp.data.task

import kotlinx.serialization.Serializable

@Serializable
enum class TaskCategory {
    STARRED,
    SCHOOL,
    MY_TASKS
}

@Serializable
enum class TaskPrivacyMode {
    RESTRICTED,       // Eingeschränkt (nur hinzugefügte Personen)
    SCHOOL_USERS,     // Nutzer der Schule / Nutzer allgemein
    ANYONE_WITH_LINK  // Jeder mit dem Link
}

@Serializable
enum class TaskMemberRole {
    OWNER,      // Eigentümer
    EDITOR,     // Mitarbeiter (kann bearbeiten/abhaken/hinzufügen)
    COMMENTER,  // Kommentator (kann kommentieren/ansehen)
    VIEWER      // Betrachter (kann nur ansehen)
}

@Serializable
data class SharedListMember(
    val userId: String,
    val username: String,
    val fullName: String,
    val email: String = "",
    val className: String = "",
    val avatarType: String = "initials",
    val avatarColorHex: Long = 0xFF395A3CL,
    val avatarUrl: String? = null,
    val role: TaskMemberRole = TaskMemberRole.VIEWER,
    val status: String = "ACCEPTED"
)

@Serializable
data class SharedListMemberUpdate(
    val userId: String,
    val role: TaskMemberRole,
    val status: String = "ACCEPTED"
)

@Serializable
data class SharedListInvitation(
    val listId: String,
    val title: String,
    val ownerId: String,
    val ownerName: String,
    val shareToken: String,
    val role: TaskMemberRole = TaskMemberRole.VIEWER,
    val createdAt: Long = 0L,
    val previewTasks: List<TaskItem> = emptyList()
)

@Serializable
data class SharedTaskListInfo(
    val listId: String,
    val title: String,
    val ownerId: String,
    val ownerName: String,
    val shareToken: String,
    val shareUrl: String,
    val privacyMode: TaskPrivacyMode = TaskPrivacyMode.RESTRICTED,
    val linkRole: TaskMemberRole = TaskMemberRole.VIEWER,
    val members: List<SharedListMember> = emptyList(),
    val currentUserRole: TaskMemberRole = TaskMemberRole.VIEWER
)

@Serializable
data class UpdateShareSettingsRequest(
    val privacyMode: TaskPrivacyMode,
    val linkRole: TaskMemberRole,
    val members: List<SharedListMemberUpdate> = emptyList()
)

@Serializable
data class SharedListJoinResponse(
    val success: Boolean,
    val message: String,
    val list: TaskList? = null,
    val tasks: List<TaskItem> = emptyList()
)

@Serializable
data class SharedListDetailsDto(
    val info: SharedTaskListInfo,
    val tasks: List<TaskItem> = emptyList(),
    val canEdit: Boolean = false
)

@Serializable
data class UserSearchResultDto(
    val id: String,
    val username: String,
    val fullName: String,
    val className: String = "",
    val avatarType: String = "initials",
    val avatarColorHex: Long = 0xFF395A3CL,
    val avatarUrl: String? = null,
    val role: String = "USER"
)

@Serializable
data class TaskList(
    val id: String,
    val title: String,
    val updatedAt: Long = 0L,
    val isShared: Boolean = false,
    val shareToken: String? = null,
    val ownerId: String? = null,
    val ownerName: String? = null,
    val role: TaskMemberRole = TaskMemberRole.OWNER
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

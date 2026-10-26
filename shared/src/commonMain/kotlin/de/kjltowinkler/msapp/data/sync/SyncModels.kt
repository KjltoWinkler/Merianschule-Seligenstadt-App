package de.kjltowinkler.msapp.data.sync

import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TaskList
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import kotlinx.serialization.Serializable

@Serializable
data class SyncTasksRequest(
    val tasks: List<TaskItem>,
    val lists: List<TaskList> = emptyList(),
    val lastSyncTimestamp: Long = 0L
)

@Serializable
data class SyncTasksResponse(
    val tasks: List<TaskItem>,
    val lists: List<TaskList>,
    val serverTimestamp: Long
)

@Serializable
data class ClassTimetableDto(
    val className: String,
    val days: Map<String, List<TimetableLesson>> = emptyMap(),
    val lastUpdated: Long = 0L
)

@Serializable
data class AdminStatusResponse(
    val success: Boolean,
    val message: String,
    val entriesCount: Int = 0,
    val timestamp: Long = 0L
)

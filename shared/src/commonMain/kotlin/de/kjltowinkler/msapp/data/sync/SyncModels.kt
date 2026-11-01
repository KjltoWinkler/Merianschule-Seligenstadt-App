package de.kjltowinkler.msapp.data.sync

import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TaskList
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import kotlinx.serialization.Serializable

@Serializable
data class SyncTasksRequest(
    val tasks: List<TaskItem> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val deletedTaskIds: List<String> = emptyList(),
    val deletedListIds: List<String> = emptyList(),
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
    val lastUpdated: Long = 0L,
    val validFor: String = ""
)

@Serializable
data class AdminStatusResponse(
    val success: Boolean,
    val message: String,
    val entriesCount: Int = 0,
    val timestamp: Long = 0L
)

@Serializable
data class ClassTimetableSummary(
    val className: String,
    val validFor: String = "",
    val lastUpdated: Long = 0L,
    val daysCount: Int = 0
)

@Serializable
data class GeminiTimetableScanRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val className: String,
    val validFor: String = "",
    val apiKey: String? = null,
    val autoSave: Boolean = false
)

@Serializable
data class GeminiTimetableScanResponse(
    val success: Boolean,
    val message: String,
    val timetable: ClassTimetableDto? = null
)

@Serializable
data class GeminiKeyConfig(
    val hasKey: Boolean,
    val maskedKey: String
)

@Serializable
data class SetGeminiKeyRequest(
    val apiKey: String
)

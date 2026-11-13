package de.kjltowinkler.msapp.util

actual object PlatformNotifier {
    actual fun sendNotification(title: String, summary: String, details: List<String>, entryId: String?) {
        println("[iOS Notification] $title: $summary (${details.joinToString("; ")})")
    }

    actual fun sendTaskNotification(title: String, summary: String, details: List<String>, taskId: String?) {
        println("[iOS Task Notification] $title: $summary (${details.joinToString("; ")})")
    }

    actual fun sendUpdateNotification(title: String, summary: String, details: List<String>): Boolean {
        println("[iOS Update Notification] $title: $summary (${details.joinToString("; ")})")
        return true
    }

    actual fun sendTimetableNotification(title: String, summary: String, details: List<String>) {
        println("[iOS Timetable Notification] $title: $summary (${details.joinToString("; ")})")
    }
}


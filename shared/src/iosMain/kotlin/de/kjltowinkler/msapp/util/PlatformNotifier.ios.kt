package de.kjltowinkler.msapp.util

actual object PlatformNotifier {
    actual fun sendNotification(title: String, summary: String, details: List<String>) {
        println("[iOS Notification] $title: $summary (${details.joinToString("; ")})")
    }

    actual fun sendTaskNotification(title: String, summary: String, details: List<String>) {
        println("[iOS Task Notification] $title: $summary (${details.joinToString("; ")})")
    }

    actual fun sendUpdateNotification(title: String, summary: String, details: List<String>) {
        println("[iOS Update Notification] $title: $summary (${details.joinToString("; ")})")
    }
}


package de.kjltowinkler.msapp.util

actual object PlatformNotifier {
    actual fun sendNotification(title: String, summary: String, details: List<String>) {
        println("[iOS Notification] $title: $summary (${details.joinToString("; ")})")
    }
}

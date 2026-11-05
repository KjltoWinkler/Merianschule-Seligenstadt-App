package de.kjltowinkler.msapp.util

actual object PlatformNotifier {
    actual fun sendNotification(title: String, summary: String, details: List<String>) {
        println("[Web-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
    }

    actual fun sendTaskNotification(title: String, summary: String, details: List<String>) {
        println("[Web-Aufgaben-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
    }

    actual fun sendUpdateNotification(title: String, summary: String, details: List<String>): Boolean {
        println("[Web-Update-Benachrichtigung] $title: $summary (${details.joinToString("; ")})")
        return true
    }
}


package de.kjltowinkler.msapp.data.notifications

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    INVITATION,    // Einladung zu einer geteilten Liste
    SUBSTITUTION,  // Vertretungsplan (Unterrichtsentfall, Vertretung, Raumänderung)
    TIMETABLE,     // Stundenplan (Aktualisierung oder neuer Plan)
    TASK,          // Aufgaben & Fristen
    GENERAL        // App-Updates oder allgemeine Mitteilungen
}

@Serializable
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val targetId: String? = null,
    val isRead: Boolean = false,
    val isArchived: Boolean = false
)

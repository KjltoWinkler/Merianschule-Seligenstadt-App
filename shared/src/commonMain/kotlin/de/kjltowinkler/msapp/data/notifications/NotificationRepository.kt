package de.kjltowinkler.msapp.data.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kjltowinkler.msapp.data.storage.PlatformStorage
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.getCurrentEpochMillis
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.task.parseIsoDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository zur Verwaltung aller In-App Benachrichtigungen.
 * Speichert Benachrichtigungen lokal in [PlatformStorage], sodass sie
 * auch nach einem Neustart erhalten bleiben.
 */
object NotificationRepository {

    private const val STORAGE_KEY = "app_notifications_list_v1"
    private val json = Json { ignoreUnknownKeys = true }

    var notifications by mutableStateOf<List<AppNotification>>(loadPersistedNotifications())
        private set

    val activeNotifications: List<AppNotification>
        get() = notifications.filter { !it.isArchived }

    val unreadCount: Int
        get() = activeNotifications.count { !it.isRead }

    private fun loadPersistedNotifications(): List<AppNotification> {
        val raw = PlatformStorage.getString(STORAGE_KEY)
        if (!raw.isNullOrBlank()) {
            try {
                return json.decodeFromString<List<AppNotification>>(raw)
            } catch (_: Throwable) {}
        }
        return createDefaultNotifications()
    }

    private fun persist(list: List<AppNotification>) {
        notifications = list
        try {
            val encoded = json.encodeToString(list)
            PlatformStorage.setString(STORAGE_KEY, encoded)
        } catch (_: Throwable) {}
    }

    /**
     * Erstellt Standard-Benachrichtigungen exakt gemäß Figma-Entwurf `benachrichtigungen#few.png`.
     */
    private fun createDefaultNotifications(): List<AppNotification> {
        val today = try { getTodayDate() } catch (_: Throwable) { SimpleDate(2026, 11, 21) }
        val yesterday = today.minusDays(1)
        val now = try { getCurrentEpochMillis() } catch (_: Throwable) { 1795369600000L }

        return listOf(
            AppNotification(
                id = "default_notif_1",
                type = NotificationType.INVITATION,
                title = "Einladung zu einer Liste",
                message = "Sebastian Wagner hat dich zu seiner Liste “Ethik-Prä...” eingeladen.",
                timestamp = now - 3600_000L,
                targetId = "shared_list_ethik",
                isRead = false,
                isArchived = false
            ),
            AppNotification(
                id = "default_notif_2",
                type = NotificationType.SUBSTITUTION,
                title = "Morgen erste Stunde entfall",
                message = "Morgen fällt bei dir die 1. Stunde aus.\nLehrer1 ist nicht da.",
                timestamp = now - 7200_000L,
                targetId = "sph_entfall_1",
                isRead = false,
                isArchived = false
            ),
            AppNotification(
                id = "default_notif_3",
                type = NotificationType.INVITATION,
                title = "Einladung zu einer Liste",
                message = "Sebastian Wagner hat dich zu seiner Liste “Ethik-Prä...” eingeladen.",
                timestamp = now - 86400_000L - 1800_000L,
                targetId = "shared_list_ethik",
                isRead = true,
                isArchived = false
            ),
            AppNotification(
                id = "default_notif_4",
                type = NotificationType.SUBSTITUTION,
                title = "Morgen erste Stunde entfall",
                message = "Morgen fällt bei dir die 1. Stunde aus.\nLehrer1 ist nicht da.",
                timestamp = now - 86400_000L - 7200_000L,
                targetId = "sph_entfall_2",
                isRead = true,
                isArchived = false
            )
        )
    }

    fun addNotification(
        title: String,
        message: String,
        type: NotificationType,
        targetId: String? = null,
        timestamp: Long = try { getCurrentEpochMillis() } catch (_: Throwable) { 0L }
    ): AppNotification {
        val newNotif = AppNotification(
            id = "notif_${timestamp}_${notifications.size}",
            type = type,
            title = title,
            message = message,
            timestamp = timestamp,
            targetId = targetId,
            isRead = false,
            isArchived = false
        )
        val updated = listOf(newNotif) + notifications
        persist(updated)
        return newNotif
    }

    fun markAsRead(id: String) {
        val updated = notifications.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        persist(updated)
    }

    fun markAllAsRead() {
        val updated = notifications.map { it.copy(isRead = true) }
        persist(updated)
    }

    fun archiveNotification(id: String) {
        val updated = notifications.map {
            if (it.id == id) it.copy(isArchived = true) else it
        }
        persist(updated)
    }

    fun deleteNotification(id: String) {
        val updated = notifications.filterNot { it.id == id }
        persist(updated)
    }

    fun clearAll() {
        persist(emptyList())
    }

    fun resetToDefaults() {
        persist(createDefaultNotifications())
    }

    /**
     * Gruppiert aktive Benachrichtigungen nach Datums-Labels wie "Heute (21.11.26)", "Gestern (20.11.26)".
     */
    fun getGroupedNotifications(): Map<String, List<AppNotification>> {
        val today = try { getTodayDate() } catch (_: Throwable) { SimpleDate(2026, 11, 21) }
        val yesterday = today.minusDays(1)
        val todayIso = today.toIsoString()
        val yesterdayIso = yesterday.toIsoString()

        val active = activeNotifications.sortedByDescending { it.timestamp }
        val map = linkedMapOf<String, MutableList<AppNotification>>()

        for (item in active) {
            val label = formatNotificationDateLabel(item.timestamp, today, yesterday, todayIso, yesterdayIso)
            map.getOrPut(label) { mutableListOf() }.add(item)
        }

        return map
    }

    private fun formatNotificationDateLabel(
        timestamp: Long,
        today: SimpleDate,
        yesterday: SimpleDate,
        todayIso: String,
        yesterdayIso: String
    ): String {
        val now = try { getCurrentEpochMillis() } catch (_: Throwable) { 0L }
        val diffDays = if (now > 0L) {
            ((now - timestamp) / 86400_000L).toInt()
        } else 0

        val itemDate = when (diffDays) {
            0 -> today
            1 -> yesterday
            else -> today.minusDays(diffDays)
        }

        val dateDisplay = "${itemDate.dayOfMonth.toString().padStart(2, '0')}.${itemDate.monthNumber.toString().padStart(2, '0')}.${(itemDate.year % 100).toString().padStart(2, '0')}"

        return when (diffDays) {
            0 -> "Heute ($dateDisplay)"
            1 -> "Gestern ($dateDisplay)"
            else -> "$dateDisplay"
        }
    }
}

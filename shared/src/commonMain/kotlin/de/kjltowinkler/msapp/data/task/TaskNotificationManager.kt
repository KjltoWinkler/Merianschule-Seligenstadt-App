package de.kjltowinkler.msapp.data.task

import de.kjltowinkler.msapp.data.storage.PlatformStorage
import de.kjltowinkler.msapp.util.PlatformNotifier

/**
 * Verwaltet Push-Benachrichtigungen und Erinnerungen für Aufgaben:
 * - Vorab-Erinnerung gemäß Einstellung (z.B. 10 Min. vorher, 1 Tag vorher)
 * - Sofortige Überfällig-Benachrichtigung und periodisches Nachhaken
 */
object TaskNotificationManager {

    private const val KEY_REMINDERS_ENABLED = "task_reminders_enabled"
    private const val KEY_LEAD_TIME = "task_reminder_lead_time"
    private const val DEFAULT_LEAD_TIME = "10 Minuten vorher"

    // Mindestabstand zwischen "Nachhaken"-Benachrichtigungen bei überfälligen Aufgaben (3 Stunden)
    private const val OVERDUE_FOLLOWUP_INTERVAL_MS = 3 * 60 * 60 * 1000L

    var isRemindersEnabled: Boolean
        get() = PlatformStorage.getString(KEY_REMINDERS_ENABLED)?.toBooleanStrictOrNull() ?: true
        set(value) {
            PlatformStorage.setString(KEY_REMINDERS_ENABLED, value.toString())
        }

    var reminderLeadTime: String
        get() = PlatformStorage.getString(KEY_LEAD_TIME) ?: DEFAULT_LEAD_TIME
        set(value) {
            PlatformStorage.setString(KEY_LEAD_TIME, value)
        }

    /**
     * Ermittelt den Vorlauf in Millisekunden aus der gespeicherten Einstellung
     * (z.B. "10 Minuten vorher", "1 Tag vorher", etc.)
     */
    fun parseLeadTimeToMillis(leadTimeStr: String): Long {
        val lower = leadTimeStr.lowercase().trim()
        val numMatch = Regex("""(\d+)""").find(lower)
        val num = numMatch?.groupValues?.get(1)?.toLongOrNull() ?: 10L

        return when {
            lower.contains("tag") -> num * 24 * 60 * 60 * 1000L
            lower.contains("stunde") -> num * 60 * 60 * 1000L
            else -> num * 60 * 1000L // Minuten
        }
    }

    /**
     * Prüft alle aktiven Aufgaben und versendet:
     * 1. Vorab-Erinnerung gemäß eingestellter Frist (z. B. 10 Min. / 1 Tag vorher).
     * 2. Sofortige Überfällig-Warnung und wiederholtes Nachhaken, solange die Aufgabe unvollständig bleibt.
     */
    fun checkAndNotify(nowMillis: Long = getCurrentEpochMillis()) {
        if (!isRemindersEnabled) return

        val activeTasks = TasksRepository.tasks.filter { !it.isCompleted }
        if (activeTasks.isEmpty()) return

        val leadMillis = parseLeadTimeToMillis(reminderLeadTime)

        for (task in activeTasks) {
            val dueMillis = task.getDueEpochMillis() ?: continue

            // FALL 1: Aufgabe ist Überfällig
            if (nowMillis > dueMillis) {
                val overdueKey = "task_notif_overdue_${task.id}"
                val lastSentStr = PlatformStorage.getString(overdueKey)
                val lastSent = lastSentStr?.toLongOrNull() ?: 0L

                val shouldNotify = if (lastSent == 0L) {
                    // Erstes Mal überfällig: Sofort alarmieren!
                    true
                } else {
                    // Bereits gemeldet: Nachhaken nach Ablauf des Intervalls
                    (nowMillis - lastSent) >= OVERDUE_FOLLOWUP_INTERVAL_MS
                }

                if (shouldNotify) {
                    val overdueMinutes = ((nowMillis - dueMillis) / 60000L).coerceAtLeast(1)
                    val overdueFormatted = formatOverdueDuration(overdueMinutes)
                    val isFollowUp = lastSent > 0L

                    val title = if (isFollowUp) {
                        "⚠️ Nachhaken: „${task.title}“ ist immer noch überfällig!"
                    } else {
                        "⚠️ Überfällige Aufgabe: „${task.title}“"
                    }

                    val summary = if (task.subject.isNotBlank()) {
                        "Fach: ${task.subject} • Überfällig seit $overdueFormatted"
                    } else {
                        "Überfällig seit $overdueFormatted – bitte erledigen!"
                    }

                    val details = mutableListOf<String>()
                    if (task.subject.isNotBlank()) details.add("Fach: ${task.subject}")
                    val deadlineText = if (task.deadlineDate.isNotBlank()) task.deadlineDate else task.dueDate
                    if (deadlineText.isNotBlank()) details.add("Frist war: $deadlineText")
                    if (task.description.isNotBlank()) details.add(task.description)
                    details.add("Status: Nicht erledigt (dringend)")

                    PlatformNotifier.sendTaskNotification(title, summary, details)
                    PlatformStorage.setString(overdueKey, nowMillis.toString())
                }
            }
            // FALL 2: Aufgabe wird bald fällig (innerhalb der Vorlaufzeit)
            else if (nowMillis >= dueMillis - leadMillis) {
                val upcomingKey = "task_notif_upcoming_${task.id}_$dueMillis"
                val alreadySent = PlatformStorage.getString(upcomingKey) != null

                if (!alreadySent) {
                    val remainingMinutes = ((dueMillis - nowMillis) / 60000L).coerceAtLeast(1)
                    val timeRemainingText = if (remainingMinutes >= 60) {
                        "${remainingMinutes / 60} Std. ${remainingMinutes % 60} Min."
                    } else {
                        "$remainingMinutes Min."
                    }

                    val title = "⏰ Aufgabe bald fällig: „${task.title}“"
                    val summary = if (task.subject.isNotBlank()) {
                        "Fach: ${task.subject} • Fällig in $timeRemainingText"
                    } else {
                        "Fällig in $timeRemainingText"
                    }

                    val details = mutableListOf<String>()
                    if (task.subject.isNotBlank()) details.add("Fach: ${task.subject}")
                    val deadlineText = if (task.deadlineDate.isNotBlank()) task.deadlineDate else task.dueDate
                    if (deadlineText.isNotBlank()) details.add("Frist: $deadlineText")
                    if (task.description.isNotBlank()) details.add(task.description)

                    PlatformNotifier.sendTaskNotification(title, summary, details)
                    PlatformStorage.setString(upcomingKey, nowMillis.toString())
                }
            }
        }
    }

    private fun formatOverdueDuration(minutes: Long): String {
        return when {
            minutes < 60 -> "$minutes Minute${if (minutes == 1L) "" else "n"}"
            minutes < 1440 -> {
                val h = minutes / 60
                val m = minutes % 60
                if (m == 0L) "$h Stunde${if (h == 1L) "" else "n"}"
                else "$h Std. $m Min."
            }
            else -> {
                val d = minutes / 1440
                val h = (minutes % 1440) / 60
                if (h == 0L) "$d Tag${if (d == 1L) "" else "en"}"
                else "$d T. $h Std."
            }
        }
    }

    fun syncReminders() {
        checkAndNotify()
    }
}

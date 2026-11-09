package de.kjltowinkler.msapp.ui.tasks

import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.getTodayDate
import de.kjltowinkler.msapp.data.task.isOverdue

data class TaskGroup(
    val title: String,
    val isErrorColor: Boolean = false,
    val tasks: List<TaskItem>
)

/**
 * Parst das Datum einer Aufgabe für die Gruppierung und Sortierung.
 */
fun parseTaskDate(task: TaskItem): SimpleDate? {
    val dateCandidate = task.dueDate.ifBlank { task.deadlineDate }.trim()
    val today = getTodayDate()

    if (dateCandidate.isBlank()) {
        return if (task.dueTime.isNotBlank()) today else null
    }

    if (dateCandidate.contains("vor ", ignoreCase = true) || dateCandidate.equals("gestern", ignoreCase = true)) {
        return today.minusDays(1)
    }
    if (dateCandidate.equals("heute", ignoreCase = true)) {
        return today
    }
    if (dateCandidate.equals("morgen", ignoreCase = true)) {
        return today.plusDays(1)
    }

    // 1. ISO YYYY-MM-DD
    val isoMatch = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""").find(dateCandidate)
    if (isoMatch != null) {
        val y = isoMatch.groupValues[1].toIntOrNull()
        val m = isoMatch.groupValues[2].toIntOrNull()
        val d = isoMatch.groupValues[3].toIntOrNull()
        if (y != null && m != null && d != null) return SimpleDate(y, m, d)
    }

    // 2. dd.MM.yyyy oder d.M.yyyy
    val numMatch = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""").find(dateCandidate)
    if (numMatch != null) {
        val d = numMatch.groupValues[1].toIntOrNull()
        val m = numMatch.groupValues[2].toIntOrNull()
        val y = numMatch.groupValues[3].toIntOrNull()
        if (y != null && m != null && d != null) return SimpleDate(y, m, d)
    }

    // 3. Format mit Wochentag & Monatsname: "Fr., 13. Nov." oder "13. Nov."
    val dayMatch = Regex("""(\d{1,2})\.\s*([A-Za-zäÄöÖüÜ]+)""").find(dateCandidate)
    if (dayMatch != null) {
        val d = dayMatch.groupValues[1].toIntOrNull()
        val mName = dayMatch.groupValues[2].lowercase().trimEnd('.')
        val monthMap = mapOf(
            "jan" to 1, "jan." to 1, "januar" to 1,
            "feb" to 2, "feb." to 2, "februar" to 2,
            "mär" to 3, "mär." to 3, "märz" to 3, "mar" to 3, "maerz" to 3,
            "apr" to 4, "apr." to 4, "april" to 4,
            "mai" to 5,
            "jun" to 6, "juni" to 6,
            "jul" to 7, "juli" to 7,
            "aug" to 8, "aug." to 8, "august" to 8,
            "sep" to 9, "sep." to 9, "sept" to 9, "sept." to 9, "september" to 9,
            "okt" to 10, "okt." to 10, "oktober" to 10,
            "nov" to 11, "nov." to 11, "november" to 11,
            "dez" to 12, "dez." to 12, "dezember" to 12
        )
        val m = monthMap[mName]
        val yearMatch = Regex("""\b(20\d{2})\b""").find(dateCandidate)
        val y = yearMatch?.groupValues?.get(1)?.toIntOrNull() ?: today.year
        if (d != null && m != null) return SimpleDate(y, m, d)
    }

    if (task.dueTime.isNotBlank()) {
        return today
    }

    return null
}

/**
 * Gruppiert Aufgaben nach "Datum":
 * - "Vergangenheit" (Überfällige Termine vor heute)
 * - "Heute" (Heutige Termine)
 * - "Morgen" (Morgige Termine)
 * - Datum in der Zukunft (z. B. "Fr., 13. Nov.")
 * - "Kein Datum" (Aufgaben ohne Zeit/Datum)
 */
fun groupTasksByDate(tasks: List<TaskItem>): List<TaskGroup> {
    if (tasks.isEmpty()) return emptyList()

    val today = getTodayDate()
    val todayIso = today.toIsoString()
    val tomorrowIso = today.plusDays(1).toIsoString()

    val pastTasks = mutableListOf<TaskItem>()
    val todayTasks = mutableListOf<TaskItem>()
    val tomorrowTasks = mutableListOf<TaskItem>()
    val futureTasksByDate = linkedMapOf<String, MutableList<TaskItem>>()
    val noDateTasks = mutableListOf<TaskItem>()

    for (task in tasks) {
        val date = parseTaskDate(task)
        if (date == null) {
            noDateTasks.add(task)
        } else {
            val iso = date.toIsoString()
            when {
                iso < todayIso || task.isOverdue() -> pastTasks.add(task)
                iso == todayIso -> todayTasks.add(task)
                iso == tomorrowIso -> tomorrowTasks.add(task)
                else -> {
                    val label = date.formattedCompletedGerman()
                    futureTasksByDate.getOrPut(label) { mutableListOf() }.add(task)
                }
            }
        }
    }

    val groups = mutableListOf<TaskGroup>()
    if (pastTasks.isNotEmpty()) {
        groups.add(TaskGroup(title = "Vergangenheit", isErrorColor = true, tasks = pastTasks))
    }
    if (todayTasks.isNotEmpty()) {
        groups.add(TaskGroup(title = "Heute", isErrorColor = false, tasks = todayTasks))
    }
    if (tomorrowTasks.isNotEmpty()) {
        groups.add(TaskGroup(title = "Morgen", isErrorColor = false, tasks = tomorrowTasks))
    }
    for ((dateLabel, taskList) in futureTasksByDate) {
        groups.add(TaskGroup(title = dateLabel, isErrorColor = false, tasks = taskList))
    }
    if (noDateTasks.isNotEmpty()) {
        groups.add(TaskGroup(title = "Kein Datum", isErrorColor = false, tasks = noDateTasks))
    }

    return groups
}

/**
 * Gruppiert Aufgaben nach "Frist":
 * - "Fällig am <Datum>" (für Aufgaben mit Fälligkeitsdatum)
 * - "Keine Frist" (für Aufgaben ohne definierte Frist)
 */
fun groupTasksByDeadline(tasks: List<TaskItem>): List<TaskGroup> {
    if (tasks.isEmpty()) return emptyList()

    val deadlineTasksByDate = linkedMapOf<String, MutableList<TaskItem>>()
    val noDeadlineTasks = mutableListOf<TaskItem>()

    for (task in tasks) {
        val hasDeadline = task.deadlineDate.isNotBlank() || task.dueDate.isNotBlank()
        if (!hasDeadline) {
            noDeadlineTasks.add(task)
        } else {
            val date = parseTaskDate(task)
            val headerTitle = if (date != null) {
                "Fällig am ${date.formattedCompletedGerman()}"
            } else {
                val clean = task.deadlineDate.ifBlank { task.dueDate }.split("•").first().trim()
                if (clean.startsWith("Fällig am ", ignoreCase = true)) clean else "Fällig am $clean"
            }
            deadlineTasksByDate.getOrPut(headerTitle) { mutableListOf() }.add(task)
        }
    }

    val groups = mutableListOf<TaskGroup>()
    for ((title, taskList) in deadlineTasksByDate) {
        groups.add(TaskGroup(title = title, isErrorColor = false, tasks = taskList))
    }
    if (noDeadlineTasks.isNotEmpty()) {
        groups.add(TaskGroup(title = "Keine Frist", isErrorColor = false, tasks = noDeadlineTasks))
    }

    return groups
}

/**
 * Gruppiert Aufgaben nach "Vor Kurzem markiert":
 * - "Vor Kurzem markiert" (im Markiert-Tab) bzw. "Markiert" (in anderen Listen)
 * - "Nicht markiert" (nicht markierte Aufgaben)
 */
fun groupTasksByStarred(tasks: List<TaskItem>, isStarredTab: Boolean = false): List<TaskGroup> {
    if (tasks.isEmpty()) return emptyList()

    val starredTasks = tasks.filter { it.isStarred }
    val unstarredTasks = tasks.filter { !it.isStarred }

    val groups = mutableListOf<TaskGroup>()
    if (starredTasks.isNotEmpty()) {
        val title = if (isStarredTab) "Vor Kurzem markiert" else "Markiert"
        groups.add(TaskGroup(title = title, isErrorColor = false, tasks = starredTasks))
    }
    if (unstarredTasks.isNotEmpty()) {
        groups.add(TaskGroup(title = "Nicht markiert", isErrorColor = false, tasks = unstarredTasks))
    }

    return groups
}

/**
 * Liefert die Gruppenaufteilung für eine Aufgabenliste anhand der gewählten Sortierung,
 * oder null, wenn keine Indikatoren angezeigt werden sollen (z. B. Titel oder Meine Reihenfolge).
 */
fun groupTasksByOption(
    tasks: List<TaskItem>,
    option: TaskSortOption,
    isStarredTab: Boolean = false
): List<TaskGroup>? {
    return when (option) {
        TaskSortOption.DATE -> groupTasksByDate(tasks)
        TaskSortOption.DEADLINE -> groupTasksByDeadline(tasks)
        TaskSortOption.RECENTLY_STARRED -> groupTasksByStarred(tasks, isStarredTab)
        TaskSortOption.TITLE, TaskSortOption.MY_ORDER -> null
    }
}

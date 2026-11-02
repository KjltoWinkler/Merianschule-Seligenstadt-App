package de.kjltowinkler.msapp

import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.data.task.isOverdue
import de.kjltowinkler.msapp.ui.tasks.TaskSortOption
import de.kjltowinkler.msapp.ui.tasks.sortedByOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testSchoolTasksOptions() {
        // Mark all school tasks starred
        TasksRepository.markAllSchoolTasksStarred()
        val schoolTasks = TasksRepository.tasks.filter { it.isSchoolTask }
        assertTrue(schoolTasks.all { it.isStarred })

        // Mark all school tasks completed
        TasksRepository.markAllSchoolTasksCompleted()
        val completedSchoolTasks = TasksRepository.tasks.filter { it.isSchoolTask }
        assertTrue(completedSchoolTasks.all { it.isCompleted })
    }

    @Test
    fun testStarredTasksOptions() {
        // Add sample tasks
        TasksRepository.addTask(
            TaskItem(
                id = "test_my_starred",
                title = "Test My Starred",
                isStarred = true,
                isSchoolTask = false
            )
        )
        TasksRepository.addTask(
            TaskItem(
                id = "test_school_starred",
                title = "Test School Starred",
                isStarred = true,
                isSchoolTask = true
            )
        )

        // Mark all starred as completed
        TasksRepository.markAllStarredTasksCompleted()
        val starred = TasksRepository.tasks.filter { it.isStarred }
        assertTrue(starred.all { it.isCompleted })

        // Delete all starred tasks -> school tasks must NOT be deleted
        TasksRepository.deleteAllStarredTasks()
        assertFalse(TasksRepository.tasks.any { it.id == "test_my_starred" })
        assertTrue(TasksRepository.tasks.any { it.id == "test_school_starred" })

        // Unstar all
        TasksRepository.unstarAllTasks()
        assertFalse(TasksRepository.tasks.any { it.isStarred })
    }

    @Test
    fun testTasksRepositoryCrudAndOfflinePersistence() {
        val testTask = TaskItem(
            id = "custom_task_99",
            title = "Physik Referat ausarbeiten",
            description = "Folien 1 bis 10",
            isCompleted = false,
            isStarred = false,
            isSchoolTask = false
        )
        TasksRepository.addTask(testTask)
        assertTrue(TasksRepository.tasks.any { it.id == "custom_task_99" })

        // Toggle completed
        TasksRepository.toggleCompleted("custom_task_99")
        val toggled = TasksRepository.tasks.first { it.id == "custom_task_99" }
        assertTrue(toggled.isCompleted)
        assertTrue(toggled.completedDate.isNotBlank())

        // Toggle starred
        TasksRepository.toggleStarred("custom_task_99")
        assertTrue(TasksRepository.tasks.first { it.id == "custom_task_99" }.isStarred)

        // Create private list and move task
        val list = TasksRepository.createPrivateList("Referate")
        assertTrue(TasksRepository.privateLists.any { it.id == list.id })

        TasksRepository.updateTask(toggled.copy(listId = list.id))
        assertEquals(list.id, TasksRepository.tasks.first { it.id == "custom_task_99" }.listId)

        // Delete list -> must delete all tasks in list
        TasksRepository.deletePrivateList(list.id)
        assertFalse(TasksRepository.privateLists.any { it.id == list.id })
        assertFalse(TasksRepository.tasks.any { it.id == "custom_task_99" })

        // Check PlatformStorage cached content
        val cached = de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("cached_tasks")
        assertTrue(cached != null && cached.isNotBlank())
    }

    @Test
    fun testSchulportalHtmlParser() {
        val sampleHtml = """
            <table id="vtable22_10_2026" class="table">
              <thead><tr>
                <th data-field="Stunde">Stunde</th>
                <th data-field="Klasse_alt">Klasse_alt</th>
                <th data-field="Lehrkraft">Lehrkraft</th>
                <th data-field="Fach">Fach</th>
                <th data-field="Raum">Raum</th>
                <th data-field="Hinweis">Hinweis</th>
              </tr></thead>
              <tbody>
                <tr><td>2</td><td>09AR</td><td>Tho</td><td>B</td><td>C202</td><td>fällt aus</td></tr>
                <tr><td>3 - 4</td><td>09AR</td><td>Af</td><td>D</td><td></td><td>fällt aus</td></tr>
              </tbody>
            </table>
        """.trimIndent()

        val parsed = de.kjltowinkler.msapp.data.schulportal.SchulportalHtmlParser.parseHtml(sampleHtml)
        assertEquals(3, parsed.size)
        assertEquals("2. Std", parsed[0].period)
        assertEquals("09AR", parsed[0].className)
        assertEquals("Biologie", parsed[0].subject)
        assertEquals("Entfall", parsed[0].type)
        assertEquals("Hr. Thomas entfällt ersatzlos", parsed[0].details)
        assertEquals("2026-10-22", parsed[0].date)

        assertEquals("3. Std", parsed[1].period)
        assertEquals("Deutsch", parsed[1].subject)
        assertEquals("Fr. Afrasiabi entfällt ersatzlos", parsed[1].details)

        assertEquals("4. Std", parsed[2].period)
        assertEquals("Deutsch", parsed[2].subject)
        assertEquals("Fr. Afrasiabi entfällt ersatzlos", parsed[2].details)
    }

    @Test
    fun testTeacherDisplayNameAndFormatting() {
        assertEquals("Fr. Afrasiabi", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Af"))
        assertEquals("Hr. Thomas", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Tho"))
        assertEquals("Hr. Weber", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Web"))
        assertEquals("Hr. Weber", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Weber"))
        assertEquals("Hr. Schneider", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Sn"))
        assertEquals("Fr. Ridel", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Rd"))
        assertEquals("Fr. Zania", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Za"))
        assertEquals("Hr. Thomas / Fr. Zania", de.kjltowinkler.msapp.data.timetable.TimetableRepository.getTeacherDisplayName("Tho / Za"))

        assertEquals("Fr. Afrasiabi entfällt ersatzlos", de.kjltowinkler.msapp.data.timetable.TimetableRepository.formatDetailsWithTeacherNames("Af entfällt ersatzlos"))
        assertEquals("Hr. Schneider statt Fr. Afrasiabi", de.kjltowinkler.msapp.data.timetable.TimetableRepository.formatDetailsWithTeacherNames("Sn statt Af"))
        assertEquals("Vertretung durch Hr. Weber", de.kjltowinkler.msapp.data.timetable.TimetableRepository.formatDetailsWithTeacherNames("Vertretung durch Web"))
    }

    @Test
    fun testExtractLastUpdated() {
        val html = """
            <div class="footer">
                <i>Letzte Aktualisierung: 22.10.2026 um 10:25:48 Uhr</i>
            </div>
        """.trimIndent()

        val updated = de.kjltowinkler.msapp.data.schulportal.SchulportalHtmlParser.extractLastUpdated(html)
        assertEquals("Letzte Aktualisierung: 22.10.2026 um 10:25:48 Uhr", updated)
    }

    @Test
    fun testChangeDetectorNoChangesForIdenticalLists() {
        val entries = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(entries, entries)
        assertFalse(diff.hasChanges)
        assertEquals(0, diff.addedEntries.size)
        assertEquals(0, diff.modifiedEntries.size)
        assertEquals(0, diff.removedEntries.size)
    }

    @Test
    fun testChangeDetectorColdBootNoSpam() {
        val entries = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(emptyList(), entries)
        assertFalse(diff.hasChanges)
    }

    @Test
    fun testChangeDetectorNewCancellation() {
        val base = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
        val newCancellation = de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
            id = "test_new_cancellation",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "1. Std",
            subject = "Englisch",
            className = "09AR",
            type = "Entfall",
            details = "Sm entfällt ersatzlos",
            roomInfo = "Entfall",
            time = "22.10.2026"
        )
        val updatedList = base + newCancellation

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(base, updatedList)
        assertTrue(diff.hasChanges)
        assertEquals(1, diff.addedEntries.size)
        assertEquals("Englisch", diff.addedEntries[0].subject)
        assertEquals("Vertretungsplan: Unterrichtsentfall", diff.title)
        assertTrue(diff.summary.contains("Englisch entfällt"))
        assertEquals(1, diff.detailLines.size)
        assertTrue(diff.detailLines[0].contains("Englisch: Entfall"))
    }

    @Test
    fun testChangeDetectorRoomModification() {
        val base = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
        // Modify entry 0 (Biologie): room changed from C202 to D101
        val modifiedList = base.mapIndexed { index, entry ->
            if (index == 0) {
                entry.copy(roomInfo = "Raum D101")
            } else entry
        }

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(base, modifiedList)
        assertTrue(diff.hasChanges)
        assertEquals(1, diff.modifiedEntries.size)
        assertEquals("Biologie", diff.modifiedEntries[0].updated.subject)
        assertTrue(diff.detailLines[0].contains("Raum D101"))
    }

    @Test
    fun testChangeDetectorRemovedEntry() {
        val base = de.kjltowinkler.msapp.data.substitution.SubstitutionRepository.defaultEntries
        // Remove 1st entry (Biologie entfällt) -> regular lesson restored
        val shortenedList = base.drop(1)

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(base, shortenedList)
        assertTrue(diff.hasChanges)
        assertEquals(1, diff.removedEntries.size)
        assertEquals("Biologie", diff.removedEntries[0].subject)
        assertTrue(diff.summary.contains("Biologie findet wieder regulär statt"))
    }

    @Test
    fun testChangeDetectorPastDateIgnored() {
        val testToday = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 2) // Montag, 02. Nov
        val testTime = de.kjltowinkler.msapp.data.task.SimpleTime(10, 0) // 10:00 Uhr

        val pastThursdayEntry = de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
            id = "past_entry_1",
            date = "2026-10-22", // Vergangener Donnerstag
            dayLabel = "Donnerstag, 22. Oktober",
            period = "2. Std",
            subject = "Biologie",
            className = "09AR",
            type = "Entfall",
            details = "Tho entfällt ersatzlos",
            roomInfo = "Entfall"
        )

        // Bestehende Basis ohne den Eintrag
        val base = listOf(
            de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
                id = "dummy_entry",
                date = "2026-11-02",
                dayLabel = "Heute",
                period = "6. Std",
                subject = "Mathe",
                className = "09AR",
                type = "Entfall"
            )
        )
        // Neuer Plan fügt den vergangenen Eintrag hinzu
        val updated = base + pastThursdayEntry

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(
            previousEntries = base,
            newEntries = updated,
            userClass = "09AR",
            filterActiveAndRelevantOnly = true,
            today = testToday,
            currentTime = testTime
        )

        // Der vergangene Tag darf KEINE Benachrichtigung auslösen!
        assertFalse(diff.hasChanges)
        assertEquals(0, diff.addedEntries.size)
    }

    @Test
    fun testChangeDetectorTodayPastPeriodIgnored() {
        val testToday = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 2) // Montag
        val testTime = de.kjltowinkler.msapp.data.task.SimpleTime(10, 0) // 10:00 Uhr (3. Stunde)

        // 1. Stunde (07:55 - 08:40) ist um 10:00 Uhr bereits lange vorbei!
        val pastPeriodEntry = de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
            id = "today_period_1",
            date = "2026-11-02",
            dayLabel = "Heute",
            period = "1. Std",
            subject = "Deutsch",
            className = "09AR",
            type = "Entfall"
        )

        val base = listOf(
            de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
                id = "base_today",
                date = "2026-11-02",
                dayLabel = "Heute",
                period = "5. Std",
                subject = "Physik",
                className = "09AR",
                type = "Entfall"
            )
        )
        val updated = base + pastPeriodEntry

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(
            previousEntries = base,
            newEntries = updated,
            userClass = "09AR",
            filterActiveAndRelevantOnly = true,
            today = testToday,
            currentTime = testTime
        )

        // Die bereits vergangene 1. Stunde darf um 10:00 Uhr nicht benachrichtigt werden!
        assertFalse(diff.hasChanges)
    }

    @Test
    fun testChangeDetectorTodayUpcomingPeriodNotified() {
        val testToday = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 2) // Montag
        val testTime = de.kjltowinkler.msapp.data.task.SimpleTime(10, 0) // 10:00 Uhr (3. Stunde)

        // 6. Stunde (12:25 - 13:10) fällt aus -> bevorstehend!
        val upcomingEntry = de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
            id = "today_period_6",
            date = "2026-11-02",
            dayLabel = "Heute",
            period = "6. Std",
            subject = "Mathe",
            className = "09AR",
            type = "Entfall",
            details = "Kne entfällt ersatzlos"
        )

        val base = listOf(
            de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
                id = "base_today",
                date = "2026-11-02",
                dayLabel = "Heute",
                period = "4. Std",
                subject = "Physik",
                className = "09AR",
                type = "Entfall"
            )
        )
        val updated = base + upcomingEntry

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(
            previousEntries = base,
            newEntries = updated,
            userClass = "09AR",
            filterActiveAndRelevantOnly = true,
            today = testToday,
            currentTime = testTime
        )

        // Bevorstehende 6. Stunde MUSS benachrichtigt werden!
        assertTrue(diff.hasChanges)
        assertEquals(1, diff.addedEntries.size)
        assertEquals("Heute • 6. Std Mathe entfällt (Kne entfällt ersatzlos)", diff.summary)
    }

    @Test
    fun testChangeDetectorForeignClassIgnored() {
        val testToday = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 2)
        val testTime = de.kjltowinkler.msapp.data.task.SimpleTime(10, 0)

        // Entfall für fremde Klasse 05BR
        val foreignClassEntry = de.kjltowinkler.msapp.data.substitution.SubstitutionEntry(
            id = "foreign_class_entry",
            date = "2026-11-02",
            dayLabel = "Heute",
            period = "6. Std",
            subject = "Biologie",
            className = "05BR",
            type = "Entfall"
        )

        val base = emptyList<de.kjltowinkler.msapp.data.substitution.SubstitutionEntry>()
        val updated = listOf(foreignClassEntry)

        val diff = de.kjltowinkler.msapp.data.substitution.SubstitutionChangeDetector.detectChanges(
            previousEntries = base,
            newEntries = updated,
            userClass = "09AR",
            filterActiveAndRelevantOnly = true,
            today = testToday,
            currentTime = testTime
        )

        assertFalse(diff.hasChanges)
    }

    @Test
    fun testSimpleDateMinusDays() {
        val nov2 = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 2)
        assertEquals("2026-11-01", nov2.minusDays(1).toIsoString())
        assertEquals("2026-10-31", nov2.minusDays(2).toIsoString())
        assertEquals("2026-10-26", nov2.minusDays(7).toIsoString())

        val jan1 = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 1, 1)
        assertEquals("2025-12-31", jan1.minusDays(1).toIsoString())

        val leapMarch1 = de.kjltowinkler.msapp.data.task.SimpleDate(2024, 3, 1)
        assertEquals("2024-02-29", leapMarch1.minusDays(1).toIsoString())
    }

    @Test
    fun testIsDateInCurrentWeek() {
        // Montag, 02. November 2026 (Woche: 02.11.2026 - 08.11.2026)
        val mondayNov2 = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 2)
        assertEquals(1, mondayNov2.dayOfWeek) // Montag

        // Einträge dieser Woche müssen true sein
        assertTrue(mondayNov2.isDateInCurrentWeek("2026-11-02"))
        assertTrue(mondayNov2.isDateInCurrentWeek("2026-11-03"))
        assertTrue(mondayNov2.isDateInCurrentWeek("2026-11-06"))

        // Einträge der VORWOCHE (z.B. Freitag, 30. Oktober 2026) MÜSSEN FALSE sein!
        assertFalse(mondayNov2.isDateInCurrentWeek("2026-10-30"))
        assertFalse(mondayNov2.isDateInCurrentWeek("2026-10-23"))
        assertFalse(mondayNov2.isDateInCurrentWeek("2026-10-22"))

        // Nächste Woche muss ebenfalls false sein
        assertFalse(mondayNov2.isDateInCurrentWeek("2026-11-09"))

        // Donnerstag, 05. November 2026
        val thursdayNov5 = de.kjltowinkler.msapp.data.task.SimpleDate(2026, 11, 5)
        assertEquals(4, thursdayNov5.dayOfWeek)
        assertTrue(thursdayNov5.isDateInCurrentWeek("2026-11-02")) // Montag derselben Woche
        assertTrue(thursdayNov5.isDateInCurrentWeek("2026-11-05")) // Heute
        assertFalse(thursdayNov5.isDateInCurrentWeek("2026-10-30")) // Freitag letzter Woche
    }

    @Test
    fun testTaskDeadlineSortingOrdersNearestFirst() {
        val taskOverdue = TaskItem(
            id = "t_overdue",
            title = "Aufsatz abgeben (Überfällig)",
            dueDate = "2026-10-25",
            dueTime = "12:00"
        )
        val taskSoon = TaskItem(
            id = "t_soon",
            title = "Mathe Hausaufgabe",
            dueDate = "2026-11-03",
            dueTime = "08:00"
        )
        val taskLater = TaskItem(
            id = "t_later",
            title = "Biologie Projekt",
            dueDate = "2026-11-20",
            dueTime = "14:00"
        )
        val taskNoDate = TaskItem(
            id = "t_nodate",
            title = "Zimmer aufräumen",
            dueDate = "",
            dueTime = "",
            deadlineDate = ""
        )

        val unsorted = listOf(taskLater, taskNoDate, taskOverdue, taskSoon)
        val sortedByDeadline = unsorted.sortedByOption(TaskSortOption.DEADLINE)

        // Die am nächsten / dringendsten fälligen Aufgaben MÜSSEN oben stehen!
        // 1. Überfällig (25. Okt)
        // 2. Bald fällig (3. Nov)
        // 3. Später fällig (20. Nov)
        // 4. Ohne Frist ganz unten
        assertEquals("t_overdue", sortedByDeadline[0].id)
        assertEquals("t_soon", sortedByDeadline[1].id)
        assertEquals("t_later", sortedByDeadline[2].id)
        assertEquals("t_nodate", sortedByDeadline[3].id)
    }

    @Test
    fun testTaskOverdueDetectionAndDateParsing() {
        // Überfällige Aufgabe
        val overdueTask = TaskItem(
            id = "t_overdue_check",
            title = "Geschichte Hausaufgabe",
            dueDate = "2026-10-15",
            dueTime = "10:00"
        )
        assertTrue(overdueTask.isOverdue(nowMillis = de.kjltowinkler.msapp.data.task.parseDateTimeToEpochMillis(2026, 11, 2, 10, 0)))

        // Erledigte Aufgaben dürfen NIEMALS als überfällig gelten!
        val completedTask = overdueTask.copy(isCompleted = true)
        assertFalse(completedTask.isOverdue(nowMillis = de.kjltowinkler.msapp.data.task.parseDateTimeToEpochMillis(2026, 11, 2, 10, 0)))

        // Zukünftige Aufgabe
        val futureTask = TaskItem(
            id = "t_future_check",
            title = "Chemie Protokoll",
            dueDate = "2026-11-10",
            dueTime = "12:00"
        )
        assertFalse(futureTask.isOverdue(nowMillis = de.kjltowinkler.msapp.data.task.parseDateTimeToEpochMillis(2026, 11, 2, 10, 0)))
    }
}
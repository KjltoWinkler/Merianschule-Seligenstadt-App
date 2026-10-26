package de.kjltowinkler.msapp

import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.data.task.TasksRepository
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
        assertEquals(2, parsed.size)
        assertEquals("2. Std", parsed[0].period)
        assertEquals("09AR", parsed[0].className)
        assertEquals("Biologie", parsed[0].subject)
        assertEquals("Entfall", parsed[0].type)
        assertEquals("Tho entfällt ersatzlos", parsed[0].details)
        assertEquals("2026-10-22", parsed[0].date)

        assertEquals("3 - 4. Std", parsed[1].period)
        assertEquals("Deutsch", parsed[1].subject)
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
}
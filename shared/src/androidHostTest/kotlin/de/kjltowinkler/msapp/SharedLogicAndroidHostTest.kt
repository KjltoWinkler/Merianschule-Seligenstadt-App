package de.kjltowinkler.msapp

import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicAndroidHostTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun testParseJsonEntriesDirectly() {
        val sampleJson = """
        [
          {
            "id": "rep-9ar-001",
            "dayLabel": "Heute, 13. Oktober",
            "period": "1. Std",
            "subject": "Mathematik",
            "className": "9aR",
            "type": "Vertretung",
            "details": "Vertretung durch Hr. Schneider (für Fr. Weber)",
            "roomInfo": "Raum 204 | Stillarbeit: Buch S. 42 Nr. 3-6",
            "time": "07:35",
            "isNew": true,
            "isMarked": true
          }
        ]
        """.trimIndent()
        val parsed = SubstitutionRepository.parseJsonEntries(sampleJson)
        println("TEST LOG: parsed direct count = ${parsed.size}")
        assertEquals(1, parsed.size)
        val first = parsed[0]
        assertEquals("rep-9ar-001", first.id)
        assertEquals("Heute, 13. Oktober", first.dayLabel)
        assertEquals("1. Std", first.period)
        assertEquals("Mathematik", first.subject)
        assertEquals("Vertretung", first.type)
        assertEquals(true, first.isNew)
        assertEquals(true, first.isMarked)
    }

    @Test
    fun testParseFullUserJsonFromDisk() {
        val fileContent = java.io.File("src/commonMain/composeResources/files/substitutions.json").readText()
        val parsed = SubstitutionRepository.parseJsonEntries(fileContent)
        println("TEST LOG: disk JSON parsed count = ${parsed.size}")
        assertEquals(12, parsed.size)
        assertEquals("sph_2026-10-22_1", parsed[0].id)
        assertEquals("2. Std", parsed[0].period)
        assertEquals("Biologie", parsed[0].subject)
        assertEquals("09AR", parsed[0].className)
    }
}
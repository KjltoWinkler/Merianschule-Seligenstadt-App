package de.kjltowinkler.msapp.data.schulportal

import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.timetable.TimetableRepository

/**
 * Parser für das HTML des Schulportal Hessen (LANIS) Vertretungsplans.
 * Parst die Tabellen (#vtableDD_MM_YYYY) und wandelt sie in [SubstitutionEntry]-Objekte um.
 */
object SchulportalHtmlParser {

    /**
     * Extrahiert den "Letzte Aktualisierung"-String aus dem HTML des Schulportals.
     * Beispiel im HTML: "Letzte Aktualisierung: 22.10.2026 um 10:25:48 Uhr"
     */
    fun extractLastUpdated(html: String): String? {
        val regex = Regex(
            """Letzte\s+Aktualisierung:\s*(\d{2}\.\d{2}\.\d{4})\s+um\s+(\d{2}:\d{2}(?::\d{2})?)\s*Uhr""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(html) ?: return null
        val datum = match.groupValues[1]
        val uhrzeit = match.groupValues[2]
        return "Letzte Aktualisierung: $datum um $uhrzeit Uhr"
    }

    /**
     * Parst das geladene HTML von `vertretungsplan.php` und gibt eine Liste von [SubstitutionEntry] zurück.
     */
    fun parseHtml(html: String): List<SubstitutionEntry> {
        val result = mutableListOf<SubstitutionEntry>()
        if (html.isBlank()) return emptyList()

        // 1. Suche nach allen Vertretungstabellen: <table id="vtable..." ...>...</table>
        val tableRegex = Regex(
            """(?is)<table[^>]*\bid=["'](vtable\d{2}_\d{2}_\d{4})["'][^>]*>(.*?)</table>"""
        )

        val tableMatches = tableRegex.findAll(html)
        var globalIndex = 1

        for (match in tableMatches) {
            val tableId = match.groupValues[1]
            val tableContent = match.groupValues[2]

            // Datum aus tableId extrahieren (z. B. vtable22_10_2026 -> 2026-10-22)
            val dateRegex = Regex("""vtable(\d{2})_(\d{2})_(\d{4})""", RegexOption.IGNORE_CASE)
            val dateMatch = dateRegex.find(tableId)
            val (day, month, year) = if (dateMatch != null) {
                Triple(dateMatch.groupValues[1], dateMatch.groupValues[2], dateMatch.groupValues[3])
            } else {
                Triple("01", "01", "2026")
            }

            val isoDate = "$year-$month-$day"
            val displayDate = "$day.$month.$year"
            val dayLabel = formatDayLabel(day.toIntOrNull() ?: 1, month.toIntOrNull() ?: 1, year.toIntOrNull() ?: 2026)

            // 2. Tabellenkopf parsen (Spaltennamen über data-field oder Inhalt)
            val theadRegex = Regex("""(?is)<thead[^>]*>(.*?)</thead>""")
            val theadContent = theadRegex.find(tableContent)?.groupValues?.get(1) ?: ""

            val thRegex = Regex("""(?is)<th([^>]*)>(.*?)</th>""")
            val headers = mutableListOf<String>()

            for (thMatch in thRegex.findAll(theadContent)) {
                val attrs = thMatch.groupValues[1]
                val text = cleanHtmlText(thMatch.groupValues[2])

                val dataFieldMatch = Regex("""data-field=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
                val fieldName = dataFieldMatch?.groupValues?.get(1)?.trim() ?: text
                headers.add(fieldName)
            }

            // Falls Spalten nicht erkannt wurden, nutze Standard-Reihenfolge
            if (headers.isEmpty()) {
                headers.addAll(listOf("Stunde", "Klasse", "Klasse_alt", "Vertretung", "Lehrkraft", "Fach", "Fach_alt", "Raum", "Hinweis", "Hinweis2", "Lerngruppe"))
            }

            // 3. Tabellenzeilen in tbody parsen
            val tbodyRegex = Regex("""(?is)<tbody[^>]*>(.*?)</tbody>""")
            val tbodyContent = tbodyRegex.find(tableContent)?.groupValues?.get(1) ?: tableContent

            val trRegex = Regex("""(?is)<tr[^>]*>(.*?)</tr>""")

            for (trMatch in trRegex.findAll(tbodyContent)) {
                val rowHtml = trMatch.groupValues[1]

                // Zeilen mit colspan überspringen (sind oft Hinweis-/Statuszeilen)
                if (rowHtml.contains("colspan", ignoreCase = true)) continue

                val tdRegex = Regex("""(?is)<td[^>]*>(.*?)</td>""")
                val cells = tdRegex.findAll(rowHtml).map { cleanHtmlText(it.groupValues[1]) }.toList()

                if (cells.isEmpty()) continue

                // Zuordnung der Spalten
                val rowMap = mutableMapOf<String, String>()
                for (i in cells.indices) {
                    val key = if (i < headers.size) headers[i] else "col_$i"
                    rowMap[key] = cells[i]
                }

                val stunde = rowMap["Stunde"] ?: ""
                val klasse = (rowMap["Klasse_alt"] ?: "").ifBlank { rowMap["Klasse"] ?: "" }
                val lehrer = (rowMap["Lehrkraft"] ?: "").ifBlank { rowMap["Lehrer"] ?: "" }
                val vertreter = (rowMap["Vertretung"] ?: "").ifBlank { rowMap["Vertreter"] ?: "" }
                val fachRaw = (rowMap["Fach"] ?: "").ifBlank { rowMap["Fach_alt"] ?: "" }
                val raum = (rowMap["Raum"] ?: "").ifBlank { rowMap["Raum_alt"] ?: "" }
                val hinweis1 = rowMap["Hinweis"] ?: ""
                val hinweis2 = rowMap["Hinweis2"] ?: ""
                val fullHinweis = listOf(hinweis1, hinweis2).filter { it.isNotBlank() }.joinToString(" ")

                val isEntfall = fullHinweis.contains("fällt aus", ignoreCase = true) ||
                                fullHinweis.contains("entfällt", ignoreCase = true) ||
                                vertreter.equals("entfällt", ignoreCase = true)

                val type = if (isEntfall) "Entfall" else "Vertretung"
                val subject = mapSubject(fachRaw)

                val lehrerDisplay = TimetableRepository.getTeacherDisplayName(lehrer)
                val vertreterDisplay = TimetableRepository.getTeacherDisplayName(vertreter)

                val details = if (isEntfall) {
                    if (lehrerDisplay.isNotBlank()) "$lehrerDisplay entfällt ersatzlos" else "Unterricht entfällt"
                } else if (vertreterDisplay.isNotBlank() && lehrerDisplay.isNotBlank()) {
                    "$vertreterDisplay statt $lehrerDisplay"
                } else if (vertreterDisplay.isNotBlank()) {
                    "Vertretung durch $vertreterDisplay"
                } else {
                    lehrerDisplay.ifBlank { "Vertretung" }
                }

                val roomInfo = when {
                    isEntfall && raum.isNotBlank() -> "Entfall (geplant $raum) - $fullHinweis"
                    isEntfall -> "Entfall - $fullHinweis"
                    raum.isNotBlank() && fullHinweis.isNotBlank() -> "Raum $raum - $fullHinweis"
                    raum.isNotBlank() -> "Raum $raum"
                    fullHinweis.isNotBlank() -> fullHinweis
                    else -> ""
                }

                // Prüfung auf Stundenbereich (z. B. "3 - 4", "3-4", "1 - 2")
                val rangeMatch = Regex("""(\d+)\s*[-–]\s*(\d+)""").find(stunde)
                val startHour = rangeMatch?.groupValues?.get(1)?.toIntOrNull()
                val endHour = rangeMatch?.groupValues?.get(2)?.toIntOrNull()

                if (startHour != null && endHour != null && startHour < endHour && (endHour - startHour) <= 10) {
                    for (h in startHour..endHour) {
                        result.add(
                            SubstitutionEntry(
                                id = "sph_${isoDate}_${globalIndex}_h$h",
                                date = isoDate,
                                dayLabel = dayLabel,
                                period = "$h. Std",
                                subject = subject,
                                className = klasse,
                                type = type,
                                details = details,
                                roomInfo = roomInfo,
                                time = displayDate,
                                isNew = true,
                                isMarked = false
                            )
                        )
                    }
                } else {
                    val singleHour = Regex("""\b(\d+)\b""").find(stunde)?.value
                    val periodFormatted = when {
                        singleHour != null -> "$singleHour. Std"
                        stunde.endsWith(". Std") || stunde.endsWith(". Stunde") -> stunde
                        stunde.isNotBlank() -> "$stunde. Std"
                        else -> ""
                    }
                    result.add(
                        SubstitutionEntry(
                            id = "sph_${isoDate}_$globalIndex",
                            date = isoDate,
                            dayLabel = dayLabel,
                            period = periodFormatted,
                            subject = subject,
                            className = klasse,
                            type = type,
                            details = details,
                            roomInfo = roomInfo,
                            time = displayDate,
                            isNew = true,
                            isMarked = false
                        )
                    )
                }
                globalIndex++
            }
        }

        return result
    }

    private fun cleanHtmlText(text: String): String {
        return text.replace(Regex("""<[^>]+>"""), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }

    private fun formatDayLabel(day: Int, month: Int, year: Int): String {
        val monthNames = listOf(
            "Januar", "Februar", "März", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Dezember"
        )
        val monthName = if (month in 1..12) monthNames[month - 1] else "$month"
        return "$day. $monthName"
    }

    private fun mapSubject(raw: String): String {
        return when (raw.trim().uppercase()) {
            "M" -> "Mathematik"
            "D" -> "Deutsch"
            "E" -> "Englisch"
            "B", "BIO" -> "Biologie"
            "CH" -> "Chemie"
            "PH" -> "Physik"
            "G", "GESCH" -> "Geschichte"
            "POWI", "PW" -> "Politik & Wirtschaft"
            "EK", "GEO" -> "Erdkunde"
            "ETH" -> "Ethik"
            "KR", "REV", "RK" -> "Religion"
            "KU", "KUNST" -> "Kunst"
            "MU", "MUSIK" -> "Musik"
            "SP", "SPORT" -> "Sport"
            "F", "FRZ" -> "Französisch"
            "L", "LAT" -> "Latein"
            "KL" -> "Klassenlehrerstunde"
            else -> raw.trim()
        }
    }
}

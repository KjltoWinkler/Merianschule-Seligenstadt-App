package de.kjltowinkler.msapp.data.substitution

import merianschuleseligenstadtapp.shared.generated.resources.Res

/**
 * Repository zur Bereitstellung der Vertretungsplan-Einträge aus `substitutions.json`
 * bzw. mit Fallback-Daten aus dem Figma-Entwurf.
 */
object SubstitutionRepository {

    /**
     * Standard-Einträge aus `FigmaDesigns/vertretungsplan#alle.png`
     */
    val defaultEntries: List<SubstitutionEntry> = listOf(
        SubstitutionEntry(
            id = "sub_1",
            date = "2026-10-12",
            dayLabel = "Heute, 12. Oktober",
            period = "3. Std",
            subject = "Mathematik",
            className = "9aR",
            type = "Vertretung",
            details = "Hr. Müller statt Fr. Schmidt",
            roomInfo = "Raum 204 (statt 102) - Aufgaben...",
            time = "7:41",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sub_2",
            date = "2026-10-12",
            dayLabel = "Heute, 12. Oktober",
            period = "5. Std",
            subject = "Deutsch",
            className = "9aR",
            type = "Entfall",
            details = "Fr. Becker entfällt ersatzlos",
            roomInfo = "Entfall - Aufgaben im Schulportal...",
            time = "6:50",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sub_3",
            date = "2026-10-12",
            dayLabel = "Heute, 12. Oktober",
            period = "3. Std",
            subject = "Mathematik",
            className = "9aR",
            type = "Vertretung",
            details = "Hr. Müller statt Fr. Schmidt",
            roomInfo = "Raum 204 (statt 102) - Aufgaben...",
            time = "Gestern",
            isNew = false,
            isMarked = true
        ),
        SubstitutionEntry(
            id = "sub_4",
            date = "2026-10-11",
            dayLabel = "Gestern, 11. Oktober",
            period = "3. Std",
            subject = "Mathematik",
            className = "9aR",
            type = "Vertretung",
            details = "Hr. Müller statt Fr. Schmidt",
            roomInfo = "Raum 204 (statt 102) - Aufgaben...",
            time = "7:41",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sub_5",
            date = "2026-10-11",
            dayLabel = "Gestern, 11. Oktober",
            period = "5. Std",
            subject = "Deutsch",
            className = "9aR",
            type = "Entfall",
            details = "Fr. Becker entfällt ersatzlos",
            roomInfo = "Entfall - Aufgaben im Schulportal...",
            time = "6:50",
            isNew = true,
            isMarked = false
        )
    )

    /**
     * Lädt Vertretungsplan-Einträge aus `substitutions.json`.
     */
    suspend fun loadSubstitutions(): List<SubstitutionEntry> {
        return try {
            val bytes = Res.readBytes("files/substitutions.json")
            val jsonString = bytes.decodeToString()
            parseJsonEntries(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Robuster nativer JSON-Parser für das Array aus SubstitutionEntry Objekten
     */
    /**
     * Robuster nativer JSON-Parser für das Array aus SubstitutionEntry Objekten
     */
    fun parseJsonEntries(json: String): List<SubstitutionEntry> {
        val list = mutableListOf<SubstitutionEntry>()
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()

        // Zerlege das JSON-Array in einzelne Objekt-Blöcke
        var depth = 0
        var objStart = -1
        var inString = false
        var escape = false
        val objectStrings = mutableListOf<String>()

        for (i in trimmed.indices) {
            val c = trimmed[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) objStart = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        objectStrings.add(trimmed.substring(objStart + 1, i))
                        objStart = -1
                    }
                }
            }
        }

        for (content in objectStrings) {
            val map = mutableMapOf<String, String>()
            val fieldRegex = Regex(""""([^"\\]+)"\s*:\s*("(?:\\.|[^"\\])*"|true|false|\d+)""")
            fieldRegex.findAll(content).forEach { match ->
                val key = match.groupValues[1]
                val rawVal = match.groupValues[2]
                val unquotedVal = if (rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
                    rawVal.substring(1, rawVal.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\")
                } else {
                    rawVal
                }
                map[key] = unquotedVal
            }

            if (map.containsKey("id") || map.containsKey("subject") || map.containsKey("details")) {
                list.add(
                    SubstitutionEntry(
                        id = map["id"] ?: "sub_${list.size + 1}",
                        date = map["date"] ?: "",
                        dayLabel = map["dayLabel"] ?: (map["date"] ?: ""),
                        period = map["period"] ?: "",
                        subject = map["subject"] ?: "",
                        className = map["className"] ?: "",
                        type = map["type"] ?: "Vertretung",
                        details = map["details"] ?: "",
                        roomInfo = map["roomInfo"] ?: "",
                        time = map["time"] ?: "",
                        isNew = map["isNew"]?.toBooleanStrictOrNull() ?: false,
                        isMarked = map["isMarked"]?.toBooleanStrictOrNull() ?: false
                    )
                )
            }
        }

        return list
    }
}

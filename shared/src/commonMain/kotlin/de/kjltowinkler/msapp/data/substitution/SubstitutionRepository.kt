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
            id = "sph_2026-10-22_1",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "2. Std",
            subject = "Biologie",
            className = "09AR",
            type = "Entfall",
            details = "Tho entfällt ersatzlos",
            roomInfo = "Entfall (geplant C202) - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-22_2",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "3 - 4. Std",
            subject = "Deutsch",
            className = "09AR",
            type = "Entfall",
            details = "Af entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-22_3",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "5. Std",
            subject = "Klassenlehrerstunde",
            className = "09AR",
            type = "Entfall",
            details = "Tho entfällt ersatzlos",
            roomInfo = "Entfall (geplant D209) - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-22_4",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "6. Std",
            subject = "Geschichte",
            className = "09AR",
            type = "Entfall",
            details = "Za entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-22_5",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "7. Std",
            subject = "Französisch",
            className = "09AR, 09BR, 09CR",
            type = "Entfall",
            details = "Hol entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_6",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "1. Std",
            subject = "Politik & Wirtschaft",
            className = "09AR",
            type = "Entfall",
            details = "Hv entfällt ersatzlos",
            roomInfo = "Entfall (geplant D209) - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_7",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "2. Std",
            subject = "Chemie",
            className = "09AR",
            type = "Entfall",
            details = "Tho entfällt ersatzlos",
            roomInfo = "Entfall (geplant D209) - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_8",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "3. Std",
            subject = "Mathematik",
            className = "09AR",
            type = "Entfall",
            details = "Kne entfällt ersatzlos",
            roomInfo = "Entfall (geplant D209) - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_9",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "4. Std",
            subject = "Ethik",
            className = "09AR, 09BR, 09CR",
            type = "Entfall",
            details = "Car entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_10",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "4. Std",
            subject = "Ethik",
            className = "09AR, 09BR, 09CR",
            type = "Entfall",
            details = "Web entfällt ersatzlos",
            roomInfo = "Entfall (geplant D210) - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_11",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "4. Std",
            subject = "Religion",
            className = "09AR, 09BR, 09CR",
            type = "Entfall",
            details = "Sr entfällt ersatzlos",
            roomInfo = "Entfall (geplant D211) - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_12",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "5 - 6. Std",
            subject = "Kunst",
            className = "09AR",
            type = "Entfall",
            details = "Bü entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        )
    )

    private var cachedLiveEntries: List<SubstitutionEntry>? = null

    /**
     * Liefert sofort verfügbare Einträge aus dem Speicher (Cache oder Fallback-Daten),
     * ohne eine Netzwerk- oder I/O-Verzögerung.
     */
    fun getInitialOrCachedEntries(): List<SubstitutionEntry> {
        return cachedLiveEntries ?: defaultEntries
    }

    /**
     * Zuletzt vom Schulportal übermittelter Zeitstempel der Aktualisierung.
     */
    var lastUpdatedText: String = "Letzte Aktualisierung: 22.10.2026 um 10:25:48 Uhr"
        private set

    /**
     * Lädt Vertretungsplan-Einträge:
     * 1. Versucht Live-Abruf über Schulportal Hessen, falls Zugangsdaten vorliegen.
     * 2. Fallback auf `substitutions.json` (lokale Assets).
     * 3. Fallback auf `defaultEntries`.
     */
    suspend fun loadSubstitutions(forceRefresh: Boolean = false): List<SubstitutionEntry> {
        if (!forceRefresh && cachedLiveEntries != null) {
            return cachedLiveEntries!!
        }

        // 1. Versuche Abruf vom selbst gehosteten Backend-Server
        try {
            val backendResult = de.kjltowinkler.msapp.data.backend.BackendApiClient.fetchSubstitutions(forceRefresh = forceRefresh)
            if (backendResult.isSuccess) {
                val serverEntries = backendResult.getOrNull()
                if (!serverEntries.isNullOrEmpty()) {
                    cachedLiveEntries = serverEntries
                    de.kjltowinkler.msapp.data.backend.BackendApiClient.lastUpdatedFromServer?.let {
                        lastUpdatedText = it
                    }
                    return serverEntries
                }
            }
        } catch (_: Exception) {
            // Backend offline oder nicht erreichbar -> Fallback
        }

        // 2. Versucht Live-Abruf über Schulportal Hessen, falls Zugangsdaten vorliegen
        if (de.kjltowinkler.msapp.data.schulportal.SchulportalClient.hasCredentials) {
            val liveResult = de.kjltowinkler.msapp.data.schulportal.SchulportalClient.fetchSubstitutionPlan()
            if (liveResult.isSuccess) {
                de.kjltowinkler.msapp.data.schulportal.SchulportalClient.lastUpdated?.let {
                    lastUpdatedText = it
                }
                val liveEntries = liveResult.getOrNull()
                if (!liveEntries.isNullOrEmpty()) {
                    cachedLiveEntries = liveEntries
                    return liveEntries
                }
            }
        }

        // 3. Fallback auf lokale substitutions.json Assets
        return try {
            val bytes = Res.readBytes("files/substitutions.json")
            val jsonString = bytes.decodeToString()
            val parsed = parseJsonEntries(jsonString)
            if (parsed.isNotEmpty()) parsed else defaultEntries
        } catch (_: Exception) {
            defaultEntries
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

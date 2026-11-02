package de.kjltowinkler.msapp.data.substitution

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
            details = "Hr. Thomas entfällt ersatzlos",
            roomInfo = "Entfall (geplant C202) - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-22_2_h3",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "3. Std",
            subject = "Deutsch",
            className = "09AR",
            type = "Entfall",
            details = "Fr. Afrasiabi entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "22.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-22_2_h4",
            date = "2026-10-22",
            dayLabel = "Heute, 22. Oktober",
            period = "4. Std",
            subject = "Deutsch",
            className = "09AR",
            type = "Entfall",
            details = "Fr. Afrasiabi entfällt ersatzlos",
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
            details = "Hr. Thomas entfällt ersatzlos",
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
            details = "Fr. Zania entfällt ersatzlos",
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
            details = "Fr. Holstein entfällt ersatzlos",
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
            details = "Hr. Horvath-Walther entfällt ersatzlos",
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
            details = "Hr. Thomas entfällt ersatzlos",
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
            details = "Hr. Knebel entfällt ersatzlos",
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
            details = "Hr. Carvalho entfällt ersatzlos",
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
            details = "Hr. Weber entfällt ersatzlos",
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
            id = "sph_2026-10-23_12_h5",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "5. Std",
            subject = "Kunst",
            className = "09AR",
            type = "Entfall",
            details = "Fr. Bürkle entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        ),
        SubstitutionEntry(
            id = "sph_2026-10-23_12_h6",
            date = "2026-10-23",
            dayLabel = "Morgen, 23. Oktober",
            period = "6. Std",
            subject = "Kunst",
            className = "09AR",
            type = "Entfall",
            details = "Fr. Bürkle entfällt ersatzlos",
            roomInfo = "Entfall - fällt aus",
            time = "23.10.2026",
            isNew = true,
            isMarked = false
        )
    )

    private var cachedLiveEntries: List<SubstitutionEntry>? = null

    /**
     * Liefert sofort verfügbare Einträge aus dem Speicher (nur noch echter Cache, keine veralteten Mock-Daten),
     * ohne eine Netzwerk- oder I/O-Verzögerung.
     */
    fun getInitialOrCachedEntries(): List<SubstitutionEntry> {
        return cachedLiveEntries ?: emptyList()
    }

    /**
     * Ob aktuell der "Kommende Tage" (Upcoming) Filter aktiv ist.
     * Bleibt auch beim Wechseln der Screens persistent erhalten.
     */
    private var _isUpcomingFilter by mutableStateOf(
        de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("substitution_is_upcoming_filter") == "true"
    )

    var isUpcomingFilter: Boolean
        get() = _isUpcomingFilter
        set(value) {
            _isUpcomingFilter = value
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("substitution_is_upcoming_filter", value.toString())
        }

    /**
     * Zuletzt vom Backend übermittelter Zeitstempel der Aktualisierung (wann das Backend beim Schulportal nachgeschaut hat).
     */
    private var _lastUpdatedText by mutableStateOf(
        sanitizeLastUpdated(de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("substitution_last_updated_text") ?: "")
    )

    var lastUpdatedText: String
        get() = _lastUpdatedText
        private set(value) {
            _lastUpdatedText = value
        }

    fun updateLastUpdatedText(value: String) {
        val clean = sanitizeLastUpdated(value)
        lastUpdatedText = clean
        if (clean.isNotBlank()) {
            de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("substitution_last_updated_text", clean)
        }
    }

    private fun sanitizeLastUpdated(raw: String): String {
        return raw
            .removePrefix("Letzte Aktualisierung:")
            .removePrefix("Stand:")
            .trim()
    }

    var isLiveDataSource: Boolean = false
        private set

    /**
     * Spaltet Einträge mit mehreren Stunden (z. B. "3 - 4. Std" oder "3-4") in einzelne Stunden-Einträge auf
     * und sorgt dafür, dass Lehrerkürzel in den Details als "Fr. / Hr. Nachname" formatiert sind.
     */
    fun expandAndNormalizeEntries(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        val result = mutableListOf<SubstitutionEntry>()
        for (entry in entries) {
            val rangeMatch = Regex("""(\d+)\s*[-–]\s*(\d+)""").find(entry.period)
            val startHour = rangeMatch?.groupValues?.get(1)?.toIntOrNull()
            val endHour = rangeMatch?.groupValues?.get(2)?.toIntOrNull()

            val formattedDetails = de.kjltowinkler.msapp.data.timetable.TimetableRepository.formatDetailsWithTeacherNames(entry.details)

            if (startHour != null && endHour != null && startHour < endHour && (endHour - startHour) <= 10) {
                for (h in startHour..endHour) {
                    result.add(
                        entry.copy(
                            id = "${entry.id}_h$h",
                            period = "$h. Std",
                            details = formattedDetails
                        )
                    )
                }
            } else {
                val singleHour = Regex("""\b(\d+)\b""").find(entry.period)?.value
                val normalizedPeriod = when {
                    entry.period.endsWith(". Std") || entry.period.endsWith(". Stunde") -> entry.period
                    singleHour != null -> "$singleHour. Std"
                    else -> entry.period
                }
                result.add(
                    entry.copy(
                        period = normalizedPeriod,
                        details = formattedDetails
                    )
                )
            }
        }
        return result
    }

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
                    val normalized = expandAndNormalizeEntries(serverEntries)
                    cachedLiveEntries = normalized
                    isLiveDataSource = true
                    de.kjltowinkler.msapp.data.backend.BackendApiClient.lastUpdatedFromServer?.let {
                        updateLastUpdatedText(it)
                    }
                    return normalized
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
                    updateLastUpdatedText(it)
                }
                val liveEntries = liveResult.getOrNull()
                if (!liveEntries.isNullOrEmpty()) {
                    val normalized = expandAndNormalizeEntries(liveEntries)
                    cachedLiveEntries = normalized
                    isLiveDataSource = true
                    return normalized
                }
            }
        }

        // 3. Fallback auf lokale substitutions.json Assets (keine Live-Daten)
        isLiveDataSource = false
        val fallback = try {
            val bytes = Res.readBytes("files/substitutions.json")
            val jsonString = bytes.decodeToString()
            val parsed = parseJsonEntries(jsonString)
            if (parsed.isNotEmpty()) parsed else defaultEntries
        } catch (_: Exception) {
            defaultEntries
        }
        val normalizedFallback = expandAndNormalizeEntries(fallback)
        return normalizedFallback
    }

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

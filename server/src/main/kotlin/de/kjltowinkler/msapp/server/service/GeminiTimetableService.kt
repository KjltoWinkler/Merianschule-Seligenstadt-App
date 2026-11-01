package de.kjltowinkler.msapp.server.service

import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
import de.kjltowinkler.msapp.data.timetable.TimetableLesson
import de.kjltowinkler.msapp.data.timetable.TimetableRepository
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object GeminiTimetableService {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getApiKey(): String? = DatabaseFactory.query { conn ->
        val sql = "SELECT value FROM server_metadata WHERE key = 'gemini_api_key';"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) {
                    val v = rs.getString("value")
                    if (!v.isNullOrBlank()) return@query v
                }
            }
        }
        ServerConfig.geminiApiKey?.takeIf { it.isNotBlank() }
    }

    suspend fun saveApiKey(apiKey: String): Boolean = DatabaseFactory.query { conn ->
        val sql = """
            INSERT INTO server_metadata (key, value, updated_at) 
            VALUES ('gemini_api_key', ?, ?) 
            ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at;
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, apiKey.trim())
            stmt.setLong(2, System.currentTimeMillis())
            stmt.executeUpdate() > 0
        }
    }

    suspend fun hasApiKey(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    suspend fun getMaskedApiKey(): String {
        val key = getApiKey() ?: return ""
        val trimmed = key.trim()
        if (trimmed.length <= 8) return "••••••••"
        return trimmed.take(6) + "••••••••" + trimmed.takeLast(4)
    }

    suspend fun analyzeTimetableImage(
        imageBase64: String,
        mimeType: String,
        className: String,
        validFor: String,
        apiKeyOverride: String? = null
    ): ClassTimetableDto {
        val key = apiKeyOverride?.trim()?.takeIf { it.isNotBlank() } ?: getApiKey()
        if (key.isNullOrBlank()) {
            throw IllegalArgumentException("Kein Google Gemini API-Key hinterlegt. Bitte trage deinen API-Key ein.")
        }

        // Clean base64 string
        var cleanBase64 = imageBase64.trim()
        var effectiveMime = mimeType.trim().lowercase()
        if (cleanBase64.startsWith("data:")) {
            val commaIdx = cleanBase64.indexOf(",")
            if (commaIdx != -1) {
                val header = cleanBase64.substring(5, commaIdx)
                val mType = header.substringBefore(";base64")
                if (mType.isNotBlank()) effectiveMime = mType
                cleanBase64 = cleanBase64.substring(commaIdx + 1)
            }
        }
        cleanBase64 = cleanBase64.replace("\n", "").replace("\r", "").replace(" ", "")

        if (effectiveMime.isBlank() || !effectiveMime.startsWith("image/")) {
            effectiveMime = "image/jpeg"
        }

        val prompt = """
Du bist ein hochpräziser Experte für die Digitalisierung von deutschen Stundenplänen (speziell Merianschule Seligenstadt).
Analysiere das beigefügte Stundenplan-Bild für die Schulklasse "$className" (Gültigkeitszeitraum: "$validFor").

Erstelle einen strukturierten Stundenplan als valides JSON-Objekt.
Das JSON MUSS exakt folgender Struktur entsprechen:
{
  "className": "$className",
  "validFor": "$validFor",
  "days": {
    "mon": [
      {
        "id": "mon_1",
        "periodNumber": 1,
        "timeRange": "07:40 - 08:25",
        "subject": "Deutsch",
        "room": "Raum B208",
        "teacherCode": "Af",
        "teacherName": "Fr. Affeldt",
        "teacherEmail": "",
        "teacherOfficeHours": "",
        "breakAfterMinutes": 5,
        "breakLabel": "5 Min. Wechselpause",
        "note": null
      }
    ],
    "tue": [],
    "wed": [],
    "thu": [],
    "fri": []
  }
}

Regeln:
1. Die Wochentage im "days"-Map dürfen AUSSCHLIESSLICH die Keys "mon", "tue", "wed", "thu", "fri" verwenden.
2. "periodNumber" ist eine Ganzzahl von 1 bis 8 (1. bis 8. Stunde).
3. Standardzeiten & Pausen an der Merianschule Seligenstadt:
   - 1. Stunde: 07:40 - 08:25 (danach 5 Min. Wechselpause)
   - 2. Stunde: 08:30 - 09:15 (danach 20 Min. große Pause)
   - 3. Stunde: 09:35 - 10:20 (danach 5 Min. Wechselpause)
   - 4. Stunde: 10:25 - 11:10 (danach 20 Min. große Pause)
   - 5. Stunde: 11:30 - 12:15 (danach 5 Min. Wechselpause)
   - 6. Stunde: 12:20 - 13:05 (danach Mittagspause)
   - 7. Stunde: 13:30 - 14:15
   - 8. Stunde: 14:15 - 15:00
   Falls im Bild konkrete abweichende Uhrzeiten stehen, übernimm diese; ansonsten nutze die Standardzeiten oben.
4. Häufige Lehrer-Kürzel an der Merianschule:
   - Af: Fr. Affeldt (Deutsch, Ethik)
   - Ky: Hr. Kaya (Mathematik, Informatik)
   - Tho / Za: Hr. Thomas / Fr. Zander (Geschichte, PoWi)
   - Des: Fr. Desch (Englisch, Spanisch)
   - Me: Fr. Meyer (Biologie, Chemie)
   - Sn: Hr. Schneider (Physik, Mathematik)
   - Wb: Hr. Weber (Sport, Erdkunde)
   - Kr: Fr. Krause (Kunst, Musik)
   Falls das Kürzel erkannt wird, trage den vollen Lehrernamen bei "teacherName" ein.
5. Achte darauf, dass für jede erkannte Stunde eine eindeutige ID vergeben wird (z. B. "mon_1", "mon_2", "tue_1" usw.).
6. Gib AUSSCHLIESSLICH das pure JSON-Objekt zurück. Kein Markdown, keine Backticks.
        """.trimIndent()

        val requestPayload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                        add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", effectiveMime)
                                put("data", cleanBase64)
                            }
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("response_mime_type", "application/json")
                put("temperature", 0.1)
            }
        }

        val candidateModels = listOf("gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        var lastError: Exception? = null
        var responseBody: String? = null

        for (model in candidateModels) {
            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload.toString()))
                .build()

            val response = try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                lastError = e
                continue
            }

            if (response.statusCode() == 200) {
                responseBody = response.body()
                break
            } else {
                val errorBody = response.body()
                var errorMsg = "Gemini API HTTP ${response.statusCode()}"
                try {
                    val errObj = jsonParser.decodeFromString<JsonObject>(errorBody)
                    val apiErr = errObj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    if (!apiErr.isNullOrBlank()) errorMsg = apiErr
                } catch (_: Exception) {}

                val isModelUnavailable = response.statusCode() == 404 || 
                    errorMsg.contains("no longer available", ignoreCase = true) || 
                    errorMsg.contains("not found", ignoreCase = true)

                if (isModelUnavailable) {
                    lastError = IllegalStateException("Modell $model: $errorMsg")
                    continue
                } else {
                    throw IllegalStateException("Fehler bei der KI-Analyse ($model): $errorMsg")
                }
            }
        }

        if (responseBody == null) {
            throw lastError ?: IllegalStateException("Keines der Gemini-Modelle konnte erfolgreich erreicht werden.")
        }

        val parsedJson = jsonParser.decodeFromString<JsonObject>(responseBody)
        val candidates = parsedJson["candidates"]?.jsonArray
        if (candidates.isNullOrEmpty()) {
            throw IllegalStateException("Gemini hat keine Antwort zurückgegeben.")
        }

        val candidate = candidates[0].jsonObject
        val contentParts = candidate["content"]?.jsonObject?.get("parts")?.jsonArray
        val rawAiText = contentParts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Kein Textinhalt in der Gemini-Antwort gefunden.")

        // Clean markdown fences if any
        var cleanedAiText = rawAiText.trim()
        if (cleanedAiText.startsWith("```json")) {
            cleanedAiText = cleanedAiText.removePrefix("```json").trim()
        } else if (cleanedAiText.startsWith("```")) {
            cleanedAiText = cleanedAiText.removePrefix("```").trim()
        }
        if (cleanedAiText.endsWith("```")) {
            cleanedAiText = cleanedAiText.removeSuffix("```").trim()
        }

        val parsedDto = try {
            jsonParser.decodeFromString<ClassTimetableDto>(cleanedAiText)
        } catch (e: Exception) {
            throw IllegalStateException("Das von Gemini generierte Ergebnis konnte nicht als Stundenplan geparst werden: ${e.message}\nRaw: $cleanedAiText")
        }

        // Normalize and enrich
        val normalizedDays = parsedDto.days.mapValues { (dayId, lessons) ->
            lessons.mapIndexed { idx, lesson ->
                val pNum = if (lesson.periodNumber > 0) lesson.periodNumber else (idx + 1)
                val lessonId = if (lesson.id.isNotBlank()) lesson.id else "${dayId}_$pNum"
                
                // Lookup teacher if teacherName missing
                val teacherInfo = TimetableRepository.teacherDirectory[lesson.teacherCode.trim()]
                val teacherName = lesson.teacherName.ifBlank { teacherInfo?.fullName ?: "" }
                val teacherEmail = lesson.teacherEmail.ifBlank { teacherInfo?.email ?: "" }
                val teacherOfficeHours = lesson.teacherOfficeHours.ifBlank { teacherInfo?.officeHours ?: "" }

                lesson.copy(
                    id = lessonId,
                    periodNumber = pNum,
                    teacherName = teacherName,
                    teacherEmail = teacherEmail,
                    teacherOfficeHours = teacherOfficeHours
                )
            }
        }

        return parsedDto.copy(
            className = parsedDto.className.ifBlank { className.trim() },
            validFor = parsedDto.validFor.ifBlank { validFor.trim() },
            days = normalizedDays,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

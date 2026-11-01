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

    fun fetchAvailableModels(apiKey: String): List<String> {
        val listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        return try {
            val req = HttpRequest.newBuilder()
                .uri(URI.create(listUrl))
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()
            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() == 200) {
                val json = jsonParser.decodeFromString<JsonObject>(resp.body())
                val modelsArr = json["models"]?.jsonArray ?: return emptyList()
                val valid = mutableListOf<String>()
                for (elem in modelsArr) {
                    val obj = elem.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: continue
                    val methods = obj["supportedGenerationMethods"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    if (methods.contains("generateContent")) {
                        val cleanName = name.removePrefix("models/")
                        valid.add(cleanName)
                    }
                }
                valid.filter {
                    val lower = it.lowercase()
                    lower.startsWith("gemini") &&
                        !lower.contains("embedding") &&
                        !lower.contains("aqa") &&
                        !lower.contains("imagen") &&
                        !lower.contains("image") && // Excludes image-gen/preview-image models with 0 free tier quota
                        !lower.contains("tts") &&
                        !lower.contains("audio") &&
                        !lower.contains("transcribe") &&
                        !lower.contains("robotics") &&
                        !lower.contains("computer") &&
                        lower != "gemini-1.5-pro" // Explicitly avoid deprecated 1.5-pro that throws 404
                }.sortedWith(compareBy<String> { model ->
                    val lower = model.lowercase()
                    when {
                        lower == "gemini-2.5-flash" -> 0
                        lower == "gemini-2.0-flash" -> 1
                        lower == "gemini-2.0-flash-lite" -> 2
                        lower == "gemini-1.5-flash" -> 3
                        lower == "gemini-1.5-flash-8b" -> 4
                        lower == "gemini-2.5-pro" -> 5
                        lower.contains("flash") && (lower.contains("2.5") || lower.contains("2.0") || lower.contains("3.")) -> 10
                        lower.contains("flash") -> 20
                        lower.contains("2.5") -> 30
                        lower.contains("2.0") -> 40
                        else -> 50
                    }
                })
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAvailableModels(apiKeyOverride: String? = null): List<String> {
        val key = apiKeyOverride?.trim()?.takeIf { it.isNotBlank() } ?: getApiKey()
        if (key.isNullOrBlank()) return emptyList()
        val discovered = fetchAvailableModels(key)
        val defaultFallbackModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",
            "gemini-2.5-pro"
        )
        return (discovered + defaultFallbackModels).distinct()
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
        "teacherName": "Af",
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
4. Lehrer-Kürzel:
   - Lies das Lehrer-Kürzel exakt so ab, wie es im Stundenplan-Bild steht (z. B. "Af", "Reu", "Tho", "Ky", "Kne", "Bü", "Hol", "Car").
   - Trage dieses Kürzel bei "teacherCode" und bei "teacherName" ein.
   - WICHTIG: Erfinde KEINESFALLS vollständige Vor- oder Nachnamen (wie z. B. "Hr. Knecht" oder "Fr. Reuter")! Bleibe bei Lehrkräften ausnahmslos und strikt bei dem reinen Kürzel aus dem Bild.
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

        val discoveredModels = fetchAvailableModels(key)
        val defaultFallbackModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",
            "gemini-2.5-pro"
        )
        val candidateModels = (discoveredModels + defaultFallbackModels).distinct().take(6)
        var lastError: Exception? = null
        var responseBody: String? = null
        val attemptedErrors = mutableListOf<String>()

        for (model in candidateModels) {
            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", key)
                .timeout(Duration.ofSeconds(25))
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload.toString()))
                .build()

            val response = try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                lastError = e
                attemptedErrors.add("$model: ${e.message}")
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

                val isAuthError = response.statusCode() in listOf(401, 403) ||
                    errorMsg.contains("API key not valid", ignoreCase = true) ||
                    errorMsg.contains("API_KEY_INVALID", ignoreCase = true)
                if (isAuthError) {
                    throw IllegalArgumentException("Der hinterlegte Google Gemini API-Key ist ungültig: $errorMsg")
                }

                // If this model has high demand, 429, 404, or is deprecated/temporarily unavailable, try the next model!
                attemptedErrors.add("$model: $errorMsg")
                lastError = IllegalStateException("Fehler bei der KI-Analyse ($model): $errorMsg")
                continue
            }
        }

        if (responseBody == null) {
            val errorSummary = attemptedErrors.joinToString("; ")
            val anyQuotaExceeded = attemptedErrors.any { 
                it.contains("quota", ignoreCase = true) || 
                it.contains("429") || 
                it.contains("rate limit", ignoreCase = true) 
            }
            if (anyQuotaExceeded) {
                throw IllegalStateException(
                    "Google Gemini Kontingent / Rate-Limit erreicht (Free Tier Limit). Bitte warte einen Moment oder überprüfe dein Google AI Studio Kontingent: $errorSummary"
                )
            }
            throw lastError ?: IllegalStateException("Keines der Gemini-Modelle konnte erfolgreich erreicht werden ($errorSummary).")
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
                
                // Always stick strictly to the original abbreviation/code without invented names
                val cleanCode = lesson.teacherCode.trim().ifBlank { lesson.teacherName.trim() }
                val teacherName = cleanCode

                lesson.copy(
                    id = lessonId,
                    periodNumber = pNum,
                    teacherCode = cleanCode,
                    teacherName = teacherName,
                    teacherEmail = "",
                    teacherOfficeHours = ""
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

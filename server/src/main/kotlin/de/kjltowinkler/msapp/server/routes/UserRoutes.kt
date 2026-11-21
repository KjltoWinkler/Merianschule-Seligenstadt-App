package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.courses.UserCourse
import de.kjltowinkler.msapp.data.settings.UserSettings
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.repository.CourseServerRepository
import de.kjltowinkler.msapp.server.repository.SettingsServerRepository
import de.kjltowinkler.msapp.server.repository.UserRepository
import de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

private fun detectImageExtension(bytes: ByteArray): String? {
    if (bytes.size < 8) return null
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() &&
        bytes[3] == 0x47.toByte() &&
        bytes[4] == 0x0D.toByte() &&
        bytes[5] == 0x0A.toByte() &&
        bytes[6] == 0x1A.toByte() &&
        bytes[7] == 0x0A.toByte()
    ) {
        return "png"
    }
    // JPEG: FF D8 FF
    if (bytes[0] == 0xFF.toByte() &&
        bytes[1] == 0xD8.toByte() &&
        bytes[2] == 0xFF.toByte()
    ) {
        return "jpg"
    }
    // WebP: RIFF .... WEBP
    if (bytes.size >= 12 &&
        bytes[0] == 'R'.code.toByte() &&
        bytes[1] == 'I'.code.toByte() &&
        bytes[2] == 'F'.code.toByte() &&
        bytes[3] == 'F'.code.toByte() &&
        bytes[8] == 'W'.code.toByte() &&
        bytes[9] == 'E'.code.toByte() &&
        bytes[10] == 'B'.code.toByte() &&
        bytes[11] == 'P'.code.toByte()
    ) {
        return "webp"
    }
    return null
}

fun Route.userRoutes() {
    route("/api/user") {
        authenticate("auth-jwt") {
            // GET /api/user/courses
            get("/courses") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@get
                }

                val courses = CourseServerRepository.getUserCourses(userId)
                call.respond(HttpStatusCode.OK, courses)
            }

            // PUT /api/user/courses
            put("/courses") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@put
                }

                val courses = runCatching { call.receive<List<UserCourse>>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültiges Kurs-Format: ${it.message}"))
                    return@put
                }

                try {
                    CourseServerRepository.saveUserCourses(userId, courses)
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Kurse erfolgreich synchronisiert."))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Speichern: ${e.message}"))
                }
            }

            // GET /api/user/settings
            get("/settings") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@get
                }

                val settingsJson = SettingsServerRepository.getSettingsJson(userId)
                if (settingsJson != null) {
                    call.respondText(settingsJson, ContentType.Application.Json)
                } else {
                    call.respond(HttpStatusCode.OK, UserSettings())
                }
            }

            // PUT /api/user/settings
            put("/settings") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@put
                }

                val originInstance = call.request.headers["X-Client-Instance"]
                val settingsJson = call.receiveText()
                try {
                    val parsed = json.decodeFromString<UserSettings>(settingsJson)
                    val canonicalJson = json.encodeToString(parsed)
                    SettingsServerRepository.saveSettingsJson(userId, canonicalJson)

                    // Echtzeit-Broadcast an alle anderen Verbindungen dieses Nutzers
                    TaskWebSocketManager.notifySettingsUpdate(userId, originInstanceId = originInstance)

                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Einstellungen erfolgreich synchronisiert."))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültiges Settings-Format: ${e.message}"))
                }
            }

            // GET /api/user/search?q=...
            get("/search") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                val query = call.request.queryParameters["q"] ?: ""
                val users = de.kjltowinkler.msapp.server.repository.UserRepository.searchUsers(query, userId)
                call.respond(HttpStatusCode.OK, users)
            }

            // GET /api/user/suggestions
            get("/suggestions") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                val users = de.kjltowinkler.msapp.server.repository.UserRepository.getSuggestedUsers(userId)
                call.respond(HttpStatusCode.OK, users)
            }

            // POST /api/user/avatar
            post("/avatar") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@post
                }

                val maxSizeBytes = 5 * 1024 * 1024 // 5 MB
                val contentType = call.request.contentType()

                val (bytes, detectedExt) = if (contentType.match(ContentType.MultiPart.FormData)) {
                    var uploadedBytes: ByteArray? = null
                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem && uploadedBytes == null) {
                            val partBytes = part.streamProvider().readBytes()
                            if (partBytes.isNotEmpty()) {
                                uploadedBytes = partBytes
                            }
                        }
                        part.dispose()
                    }
                    val nonNullBytes = uploadedBytes ?: run {
                        call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Keine Bilddatei übermittelt."))
                        return@post
                    }
                    val detected = detectImageExtension(nonNullBytes)
                    if (detected == null) {
                        call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültiges Bildformat. Erlaubt sind JPG, PNG und WebP."))
                        return@post
                    }
                    nonNullBytes to detected
                } else {
                    val rawBytes = runCatching { call.receive<ByteArray>() }.getOrNull()
                    if (rawBytes == null || rawBytes.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Keine Bilddaten empfangen."))
                        return@post
                    }
                    val detected = detectImageExtension(rawBytes)
                    if (detected == null) {
                        call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültiges Bildformat. Erlaubt sind JPG, PNG und WebP."))
                        return@post
                    }
                    rawBytes to detected
                }

                if (bytes.size > maxSizeBytes) {
                    call.respond(HttpStatusCode.PayloadTooLarge, SimpleApiResponse(false, "Das Profilbild darf maximal 5 MB groß sein."))
                    return@post
                }

                try {
                    ServerConfig.ensureAvatarsDirectory()
                    val avatarsDir = File(ServerConfig.avatarsDir)

                    // Alte Avatar-Dateien dieses Benutzers löschen
                    val existing = avatarsDir.listFiles { _, name ->
                        name.startsWith("${userId}.")
                    }
                    existing?.forEach { it.delete() }

                    val targetFile = File(avatarsDir, "${userId}.${detectedExt}")
                    targetFile.writeBytes(bytes)

                    val timestamp = System.currentTimeMillis()
                    val avatarUrl = "/api/user/avatar/$userId?v=$timestamp"

                    val updatedUser = UserRepository.updateAvatar(userId, avatarType = "custom", avatarUrl = avatarUrl)
                    if (updatedUser != null) {
                        val originInstance = call.request.headers["X-Client-Instance"]
                        TaskWebSocketManager.notifySettingsUpdate(userId, originInstanceId = originInstance)
                        call.respond(HttpStatusCode.OK, updatedUser)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Aktualisieren des Profils."))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Speichern des Profilbilds: ${e.message}"))
                }
            }

            // DELETE /api/user/avatar
            delete("/avatar") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@delete
                }

                try {
                    val avatarsDir = File(ServerConfig.avatarsDir)
                    val existing = avatarsDir.listFiles { _, name ->
                        name.startsWith("${userId}.")
                    }
                    existing?.forEach { it.delete() }

                    val updatedUser = UserRepository.updateAvatar(userId, avatarType = "initials", avatarUrl = null)
                    if (updatedUser != null) {
                        val originInstance = call.request.headers["X-Client-Instance"]
                        TaskWebSocketManager.notifySettingsUpdate(userId, originInstanceId = originInstance)
                        call.respond(HttpStatusCode.OK, updatedUser)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Aktualisieren des Profils."))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Löschen des Profilbilds: ${e.message}"))
                }
            }
        }

        // GET /api/user/avatar/{userId} (Öffentlich abrufbar für Profilanzeige & geteilte Aufgabenlisten)
        get("/avatar/{userId}") {
            val targetUserId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "User-ID fehlt.")
            // Pfad-Traversal-Schutz: Nur Alphanumerik, Unterstrich und Bindestrich erlauben
            if (!targetUserId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                call.respond(HttpStatusCode.BadRequest, "Ungültige User-ID.")
                return@get
            }

            val avatarsDir = File(ServerConfig.avatarsDir)
            val candidateFiles = listOf(
                File(avatarsDir, "$targetUserId.jpg"),
                File(avatarsDir, "$targetUserId.jpeg"),
                File(avatarsDir, "$targetUserId.png"),
                File(avatarsDir, "$targetUserId.webp")
            )
            val file = candidateFiles.firstOrNull { it.exists() && it.isFile }
            if (file == null) {
                call.respond(HttpStatusCode.NotFound, "Profilbild nicht vorhanden.")
                return@get
            }

            val contentType = when (file.extension.lowercase()) {
                "png" -> ContentType.Image.PNG
                "webp" -> ContentType("image", "webp")
                else -> ContentType.Image.JPEG
            }

            call.response.header(HttpHeaders.CacheControl, "public, max-age=86400, must-revalidate")
            call.response.header(HttpHeaders.ETag, "\"${file.lastModified()}\"")
            call.response.header("X-Content-Type-Options", "nosniff")
            call.respondFile(file)
        }
    }

    route("/api/users") {
        authenticate("auth-jwt") {
            // GET /api/users/search?q=...
            get("/search") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                val query = call.request.queryParameters["q"] ?: ""
                val users = de.kjltowinkler.msapp.server.repository.UserRepository.searchUsers(query, userId)
                call.respond(HttpStatusCode.OK, users)
            }

            // GET /api/users/suggestions
            get("/suggestions") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                val users = de.kjltowinkler.msapp.server.repository.UserRepository.getSuggestedUsers(userId)
                call.respond(HttpStatusCode.OK, users)
            }
        }
    }
}

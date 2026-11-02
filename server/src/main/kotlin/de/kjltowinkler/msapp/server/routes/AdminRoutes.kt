package de.kjltowinkler.msapp.server.routes

import com.auth0.jwt.JWT
import de.kjltowinkler.msapp.data.auth.RegisterRequest
import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.sync.AdminStatusResponse
import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
import de.kjltowinkler.msapp.data.task.SchoolTaskItem
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.repository.SchoolTaskRepository
import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import de.kjltowinkler.msapp.server.repository.TaskRepository
import de.kjltowinkler.msapp.server.repository.TimetableServerRepository
import de.kjltowinkler.msapp.server.repository.UserRepository
import de.kjltowinkler.msapp.server.security.JwtService
import de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdminCreateUserRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String = "",
    val className: String = "9aR",
    val schoolType: String = "Realschule",
    val classTeacher: String = "Hr. Thomas",
    val studentId: String = "",
    val role: String = "USER"
)

@Serializable
data class AdminResetPasswordRequest(
    val newPassword: String
)

@Serializable
data class ServerStatsResponse(
    val status: String,
    val totalUsers: Int,
    val totalTasks: Int,
    val totalSubstitutions: Int,
    val substitutionsLastUpdated: Long,
    val uptimeMillis: Long,
    val freeMemoryMb: Long,
    val totalMemoryMb: Long
)

private val serverStartTime = System.currentTimeMillis()

private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private fun verifyAdminAccess(call: ApplicationCall): Boolean {
    // 1. Check Header X-Admin-Key
    val adminKeyHeader = call.request.header("X-Admin-Key")
    if (adminKeyHeader != null && adminKeyHeader == ServerConfig.adminKey) {
        return true
    }

    // 2. Check Bearer Token with role == "ADMIN"
    val authHeader = call.request.header("Authorization")
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        val token = authHeader.removePrefix("Bearer ").trim()
        return try {
            val decoded = JwtService.verifier.verify(token)
            val role = decoded.getClaim("role").asString()
            role.equals("ADMIN", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    return false
}

fun Route.adminRoutes() {
    route("/api/admin") {

        // User Management
        route("/users") {
            get {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich (X-Admin-Key Header oder Admin-JWT)."))
                    return@get
                }
                val users = UserRepository.getAllUsers()
                call.respond(HttpStatusCode.OK, users)
            }

            post {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich (X-Admin-Key Header oder Admin-JWT)."))
                    return@post
                }
                val req = runCatching { call.receive<AdminCreateUserRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Benutzerdaten."))
                    return@post
                }

                if (req.username.isBlank() || req.password.isBlank() || req.email.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Benutzername, E-Mail und Passwort sind erforderlich."))
                    return@post
                }

                try {
                    val user = UserRepository.createUser(
                        RegisterRequest(
                            username = req.username,
                            email = req.email,
                            password = req.password,
                            fullName = req.fullName,
                            className = req.className,
                            schoolType = req.schoolType,
                            classTeacher = req.classTeacher,
                            avatarType = "initials",
                            avatarColorHex = 0xFF1B65A4L
                        ),
                        role = req.role.ifBlank { "USER" }.uppercase(),
                        hasCustomPassword = false,
                        studentId = req.studentId
                    )
                    call.respond(HttpStatusCode.Created, user.toDto())
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.Conflict, SimpleApiResponse(false, e.message ?: "Benutzer existiert bereits."))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Erstellen des Benutzers: ${e.message}"))
                }
            }

            delete("/{id}") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@delete
                }
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt."))
                    return@delete
                }
                val success = UserRepository.deleteUser(id)
                if (success) {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Benutzer erfolgreich gelöscht."))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                }
            }

            post("/{id}/password") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt."))
                    return@post
                }
                val req = runCatching { call.receive<AdminResetPasswordRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Neues Passwort fehlt."))
                    return@post
                }
                if (req.newPassword.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Neues Passwort darf nicht leer sein."))
                    return@post
                }
                val success = UserRepository.resetPassword(id, req.newPassword)
                if (success) {
                    UserRepository.setHasCustomPassword(id, false)
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Passwort erfolgreich zurückgesetzt."))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                }
            }

            post("/{id}/reset-2fa") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }
                val id = call.parameters["id"]
                if (id.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt."))
                    return@post
                }
                val success = UserRepository.disableTwoFactor(id)
                if (success) {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "2FA wurde erfolgreich zurückgesetzt."))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                }
            }
        }

        // Check stats
        get("/stats") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich (X-Admin-Key Header oder Admin-JWT)."))
                return@get
            }

            val runtime = Runtime.getRuntime()
            val stats = ServerStatsResponse(
                status = "ONLINE",
                totalUsers = UserRepository.countUsers(),
                totalTasks = TaskRepository.countTasks(),
                totalSubstitutions = SubstitutionServerRepository.getCount(),
                substitutionsLastUpdated = SubstitutionServerRepository.getLastUpdated(),
                uptimeMillis = System.currentTimeMillis() - serverStartTime,
                freeMemoryMb = runtime.freeMemory() / (1024 * 1024),
                totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
            )
            call.respond(HttpStatusCode.OK, stats)
        }

        // Vertretungsplan (Substitutions) Management
        get("/substitutions") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich (X-Admin-Key Header oder Admin-JWT)."))
                return@get
            }
            val entries = SubstitutionServerRepository.getAll()
            call.respond(HttpStatusCode.OK, entries)
        }

        post("/substitutions/entry") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich (X-Admin-Key Header oder Admin-JWT)."))
                return@post
            }
            val entry = runCatching { call.receive<SubstitutionEntry>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültiger Vertretungseintrag."))
                return@post
            }
            val entryToSave = if (entry.id.isBlank()) {
                entry.copy(id = "sub_${System.currentTimeMillis()}_${(1000..9999).random()}")
            } else {
                entry
            }
            val success = SubstitutionServerRepository.addOrUpdateEntry(entryToSave)
            if (success) {
                call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Eintrag erfolgreich gespeichert."))
            } else {
                call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Speichern des Eintrags."))
            }
        }

        delete("/substitutions/{id}") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                return@delete
            }
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt."))
                return@delete
            }
            val success = SubstitutionServerRepository.deleteById(id)
            if (success) {
                call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Eintrag erfolgreich gelöscht."))
            } else {
                call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Eintrag nicht gefunden."))
            }
        }

        // Upload / Update Substitutions (Vertretungsplan)
        post("/substitutions") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich (X-Admin-Key Header oder Admin-JWT)."))
                return@post
            }

            val rawText = call.receiveText()
            if (rawText.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Anfrage-Body ist leer."))
                return@post
            }

            try {
                val entries = jsonParser.decodeFromString<List<SubstitutionEntry>>(rawText)
                val count = SubstitutionServerRepository.replaceEntries(entries, rawText)
                call.respond(HttpStatusCode.OK, AdminStatusResponse(
                    success = true,
                    message = "Vertretungsplan erfolgreich aktualisiert ($count Einträge gespeichert).",
                    entriesCount = count,
                    timestamp = System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Fehler beim Parsen der JSON-Daten: ${e.message}"))
            }
        }

        // Clear / Delete old Substitutions
        delete("/substitutions") {
            if (!verifyAdminAccess(call)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                return@delete
            }

            val olderThan = call.request.queryParameters["olderThan"]
            val deleted = if (!olderThan.isNullOrBlank()) {
                SubstitutionServerRepository.deleteOlderThan(olderThan)
            } else {
                SubstitutionServerRepository.clearAll()
            }

            call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "$deleted Einträge gelöscht."))
        }

        // Timetable (Stundenplan) & Gemini AI Scan
        route("/timetable") {
            get("/classes") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@get
                }
                val list = TimetableServerRepository.getAllClassesWithTimetables()
                call.respond(HttpStatusCode.OK, list)
            }

            get("/{className}") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@get
                }
                val className = call.parameters["className"]
                if (className.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Klassenname fehlt."))
                    return@get
                }
                val dto = TimetableServerRepository.getClassTimetable(className)
                call.respond(HttpStatusCode.OK, dto)
            }

            delete("/{className}") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@delete
                }
                val className = call.parameters["className"]
                if (className.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Klassenname fehlt."))
                    return@delete
                }
                val success = TimetableServerRepository.deleteClassTimetable(className)
                if (success) {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Stundenplan für Klasse $className gelöscht."))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Stundenplan nicht gefunden."))
                }
            }

            post {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }

                val rawText = call.receiveText()
                if (rawText.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Anfrage-Body ist leer."))
                    return@post
                }

                try {
                    val timetableDto = jsonParser.decodeFromString<ClassTimetableDto>(rawText)
                    val count = TimetableServerRepository.saveClassTimetable(
                        className = timetableDto.className,
                        days = timetableDto.days,
                        validFor = timetableDto.validFor,
                        rawJsonContent = rawText
                    )
                    call.respond(HttpStatusCode.OK, AdminStatusResponse(
                        success = true,
                        message = "Stundenplan für Klasse ${timetableDto.className} erfolgreich gespeichert ($count Stunden hinterlegt).",
                        entriesCount = count,
                        timestamp = System.currentTimeMillis()
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Fehler beim Parsen des Stundenplans: ${e.message}"))
                }
            }

            post("/gemini-scan") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }

                val req = runCatching { call.receive<de.kjltowinkler.msapp.data.sync.GeminiTimetableScanRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, de.kjltowinkler.msapp.data.sync.GeminiTimetableScanResponse(
                        success = false,
                        message = "Ungültige Anfrage-Daten für den KI-Scan: ${it.message}"
                    ))
                    return@post
                }

                if (req.imageBase64.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, de.kjltowinkler.msapp.data.sync.GeminiTimetableScanResponse(
                        success = false,
                        message = "Kein Bild übergeben."
                    ))
                    return@post
                }

                if (req.className.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, de.kjltowinkler.msapp.data.sync.GeminiTimetableScanResponse(
                        success = false,
                        message = "Bitte gib eine Klasse an (z. B. '09AR')."
                    ))
                    return@post
                }

                try {
                    val timetable = de.kjltowinkler.msapp.server.service.GeminiTimetableService.analyzeTimetableImage(
                        imageBase64 = req.imageBase64,
                        mimeType = req.mimeType,
                        className = req.className,
                        validFor = req.validFor,
                        apiKeyOverride = req.apiKey
                    )

                    if (req.autoSave) {
                        TimetableServerRepository.saveClassTimetable(
                            className = timetable.className,
                            days = timetable.days,
                            validFor = timetable.validFor
                        )
                    }

                    call.respond(HttpStatusCode.OK, de.kjltowinkler.msapp.data.sync.GeminiTimetableScanResponse(
                        success = true,
                        message = "Stundenplan für Klasse ${timetable.className} erfolgreich per Gemini KI analysiert.",
                        timetable = timetable
                    ))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, de.kjltowinkler.msapp.data.sync.GeminiTimetableScanResponse(
                        success = false,
                        message = e.message ?: "Ungültige Anfrage."
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, de.kjltowinkler.msapp.data.sync.GeminiTimetableScanResponse(
                        success = false,
                        message = "KI-Scan fehlgeschlagen: ${e.message}"
                    ))
                }
            }
        }

        // Configuration & Gemini API Key
        route("/config") {
            get("/gemini-key") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@get
                }
                val hasKey = de.kjltowinkler.msapp.server.service.GeminiTimetableService.hasApiKey()
                val masked = de.kjltowinkler.msapp.server.service.GeminiTimetableService.getMaskedApiKey()
                call.respond(HttpStatusCode.OK, de.kjltowinkler.msapp.data.sync.GeminiKeyConfig(hasKey, masked))
            }

            get("/gemini-models") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@get
                }
                val models = de.kjltowinkler.msapp.server.service.GeminiTimetableService.getAvailableModels()
                call.respond(HttpStatusCode.OK, models)
            }

            post("/gemini-key") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }
                val req = runCatching { call.receive<de.kjltowinkler.msapp.data.sync.SetGeminiKeyRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültiger API-Key Request."))
                    return@post
                }
                if (req.apiKey.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Der API-Key darf nicht leer sein."))
                    return@post
                }
                de.kjltowinkler.msapp.server.service.GeminiTimetableService.saveApiKey(req.apiKey)
                call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Gemini API-Key erfolgreich gespeichert."))
            }
        }

        // Schulportal Sync & Status
        route("/schulportal") {
            get("/status") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@get
                }
                call.respond(HttpStatusCode.OK, de.kjltowinkler.msapp.server.service.SchulportalSyncService.getStatus())
            }

            post("/credentials") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }
                val req = runCatching { call.receive<de.kjltowinkler.msapp.server.service.SetSchulportalCredentialsRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anmeldedaten."))
                    return@post
                }
                de.kjltowinkler.msapp.server.service.SchulportalSyncService.saveCredentials(req)
                val syncResult = de.kjltowinkler.msapp.server.service.SchulportalSyncService.syncNow()
                if (syncResult.isSuccess) {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Schulportal-Zugangsdaten gespeichert und ${syncResult.getOrNull()} Einträge synchronisiert."))
                } else {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Zugangsdaten gespeichert. Erste Synchronisation: ${syncResult.exceptionOrNull()?.message}"))
                }
            }

            post("/sync") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }
                val result = de.kjltowinkler.msapp.server.service.SchulportalSyncService.syncNow(force = true)
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Synchronisation erfolgreich ($count Einträge)."))
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Unbekannter Fehler"
                    call.respond(HttpStatusCode.BadGateway, SimpleApiResponse(false, "Synchronisation fehlgeschlagen: $err"))
                }
            }
        }

        // Schulaufgaben (School Tasks) Management
        route("/school-tasks") {
            get {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@get
                }
                val tasks = SchoolTaskRepository.getAllSchoolTasks()
                call.respond(HttpStatusCode.OK, tasks)
            }

            post {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@post
                }
                val task = runCatching { call.receive<SchoolTaskItem>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Schulaufgabendaten."))
                    return@post
                }
                if (task.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Titel der Schulaufgabe darf nicht leer sein."))
                    return@post
                }
                val saved = SchoolTaskRepository.upsertSchoolTask(task)
                TaskWebSocketManager.broadcastTaskUpdate(reason = "school_task_updated")
                call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Schulaufgabe '${saved.title}' erfolgreich gespeichert."))
            }

            delete("/{id}") {
                if (!verifyAdminAccess(call)) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Authentifizierung erforderlich."))
                    return@delete
                }
                val taskId = call.parameters["id"]
                if (taskId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Aufgaben-ID fehlt."))
                    return@delete
                }
                val deleted = SchoolTaskRepository.deleteSchoolTask(taskId)
                if (deleted) {
                    TaskWebSocketManager.broadcastTaskUpdate(reason = "school_task_deleted")
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Schulaufgabe erfolgreich gelöscht."))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Schulaufgabe nicht gefunden."))
                }
            }
        }

        // Schüleraufgaben unterliegen dem Datenschutz (DSGVO) und sind für Administratoren nicht einsehbar.
        route("/user-tasks") {
            get {
                call.respond(
                    HttpStatusCode.Forbidden,
                    SimpleApiResponse(false, "Schüleraufgaben unterliegen dem Datenschutz (DSGVO) und sind für Administratoren nicht einsehbar.")
                )
            }

            delete("/{id}") {
                call.respond(
                    HttpStatusCode.Forbidden,
                    SimpleApiResponse(false, "Schüleraufgaben unterliegen dem Datenschutz (DSGVO) und sind für Administratoren nicht einsehbar.")
                )
            }
        }
    }
}

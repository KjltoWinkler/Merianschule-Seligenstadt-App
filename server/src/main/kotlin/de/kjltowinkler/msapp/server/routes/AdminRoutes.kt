package de.kjltowinkler.msapp.server.routes

import com.auth0.jwt.JWT
import de.kjltowinkler.msapp.data.auth.RegisterRequest
import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.sync.AdminStatusResponse
import de.kjltowinkler.msapp.data.sync.ClassTimetableDto
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import de.kjltowinkler.msapp.server.repository.TaskRepository
import de.kjltowinkler.msapp.server.repository.TimetableServerRepository
import de.kjltowinkler.msapp.server.repository.UserRepository
import de.kjltowinkler.msapp.server.security.JwtService
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

        // Upload / Update Timetable (Stundenplan)

        post("/timetable") {
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
                val timetableDto = jsonParser.decodeFromString<ClassTimetableDto>(rawText)
                val count = TimetableServerRepository.saveClassTimetable(
                    className = timetableDto.className,
                    days = timetableDto.days,
                    rawJsonContent = rawText
                )
                call.respond(HttpStatusCode.OK, AdminStatusResponse(
                    success = true,
                    message = "Stundenplan für Klasse ${timetableDto.className} erfolgreich aktualisiert ($count Stunden hinterlegt).",
                    entriesCount = count,
                    timestamp = System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Fehler beim Parsen des Stundenplans: ${e.message}"))
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
    }
}

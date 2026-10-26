package de.kjltowinkler.msapp.server.routes

import com.auth0.jwt.JWT
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.courses.UserCourse
import de.kjltowinkler.msapp.data.settings.UserSettings
import de.kjltowinkler.msapp.server.repository.CourseServerRepository
import de.kjltowinkler.msapp.server.repository.SettingsServerRepository
import de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

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
        }
    }
}

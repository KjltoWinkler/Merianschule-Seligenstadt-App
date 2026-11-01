package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.courses.UserCourse
import de.kjltowinkler.msapp.server.repository.CourseServerRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

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
        }
    }
}

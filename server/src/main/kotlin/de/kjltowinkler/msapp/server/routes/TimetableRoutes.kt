package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.server.repository.TimetableServerRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.timetableRoutes() {
    route("/api/timetable") {
        get {
            call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
            call.response.header(HttpHeaders.Pragma, "no-cache")
            call.response.header(HttpHeaders.Expires, "0")

            val className = call.request.queryParameters["class"] ?: "09AR"
            val timetable = TimetableServerRepository.getClassTimetable(className)
            call.respond(HttpStatusCode.OK, timetable)
        }

        get("/teachers") {
            call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
            call.response.header(HttpHeaders.Pragma, "no-cache")
            call.response.header(HttpHeaders.Expires, "0")

            val directory = TimetableServerRepository.getTeacherDirectory()
            call.respond(HttpStatusCode.OK, directory)
        }
    }
}

package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.server.repository.TimetableServerRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.timetableRoutes() {
    route("/api/timetable") {
        get {
            val className = call.request.queryParameters["class"] ?: "09AR"
            val timetable = TimetableServerRepository.getClassTimetable(className)
            call.respond(HttpStatusCode.OK, timetable)
        }

        get("/teachers") {
            val directory = TimetableServerRepository.getTeacherDirectory()
            call.respond(HttpStatusCode.OK, directory)
        }
    }
}

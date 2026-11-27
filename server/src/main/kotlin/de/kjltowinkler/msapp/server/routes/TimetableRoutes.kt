package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.server.repository.TimetableServerRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
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

        get("/teachers/avatar/{code}") {
            val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Lehrerkürzel fehlt.")
            if (!code.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                call.respond(HttpStatusCode.BadRequest, "Ungültiges Lehrerkürzel.")
                return@get
            }
            val file = TimetableServerRepository.getTeacherAvatarFile(code)
            if (file != null) {
                call.response.header(HttpHeaders.CacheControl, "public, max-age=86400, must-revalidate")
                call.response.header(HttpHeaders.ETag, "\"${file.lastModified()}\"")
                call.response.header("X-Content-Type-Options", "nosniff")
                call.respondFile(file)
                return@get
            }

            val bundled = TimetableServerRepository.getBundledTeacherAvatar(code)
            if (bundled != null) {
                val (bytes, ext) = bundled
                val contentType = when (ext) {
                    "png" -> ContentType.Image.PNG
                    "webp" -> ContentType("image", "webp")
                    else -> ContentType.Image.JPEG
                }
                call.response.header(HttpHeaders.CacheControl, "public, max-age=86400, must-revalidate")
                call.response.header("X-Content-Type-Options", "nosniff")
                call.respondBytes(bytes, contentType)
                return@get
            }

            call.respond(HttpStatusCode.NotFound, "Lehrer-Profilbild nicht vorhanden.")
        }
    }
}

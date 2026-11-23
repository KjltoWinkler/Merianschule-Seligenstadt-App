package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.news.CreateNewsRequest
import de.kjltowinkler.msapp.data.news.NewsItem
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.repository.NewsServerRepository
import de.kjltowinkler.msapp.server.security.JwtService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.io.File
import java.util.UUID

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

fun Route.newsRoutes() {
    route("/api/news") {
        // Public/Client: get all news & events
        get {
            val news = NewsServerRepository.getAll()
            call.respond(news)
        }

        // Static serve news images
        get("/images/{filename}") {
            val filename = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Dateiname fehlt.")
            val safeName = File(filename).name
            val file = File(ServerConfig.newsImagesDir, safeName)
            if (file.exists() && file.isFile) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "Bild nicht gefunden.")
            }
        }
    }

    // Admin endpoints for News & Events
    route("/api/admin/news") {
        get {
            if (!verifyAdminAccess(call)) {
                return@get call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Zugriff erforderlich."))
            }
            val news = NewsServerRepository.getAll()
            call.respond(news)
        }

        post {
            if (!verifyAdminAccess(call)) {
                return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Zugriff erforderlich."))
            }
            try {
                val req = call.receive<CreateNewsRequest>()
                val now = System.currentTimeMillis()
                val newItem = NewsItem(
                    id = UUID.randomUUID().toString(),
                    title = req.title.trim(),
                    description = req.description.trim(),
                    imageUrl = req.imageUrl?.takeIf { it.isNotBlank() },
                    type = req.type,
                    dateText = req.dateText.trim(),
                    timestamp = if (req.timestamp > 0) req.timestamp else now,
                    author = req.author.ifBlank { "Schulleitung" },
                    isFeatured = req.isFeatured,
                    createdAt = now,
                    updatedAt = now
                )
                val created = NewsServerRepository.create(newItem)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Fehler beim Erstellen der Neuigkeit: ${e.message}"))
            }
        }

        put("/{id}") {
            if (!verifyAdminAccess(call)) {
                return@put call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Zugriff erforderlich."))
            }
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt."))
            try {
                val item = call.receive<NewsItem>()
                val updated = NewsServerRepository.update(item.copy(id = id, updatedAt = System.currentTimeMillis()))
                if (updated) {
                    call.respond(SimpleApiResponse(true, "Neuigkeit erfolgreich aktualisiert."))
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Neuigkeit nicht gefunden."))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Fehler beim Aktualisieren: ${e.message}"))
            }
        }

        delete("/{id}") {
            if (!verifyAdminAccess(call)) {
                return@delete call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Zugriff erforderlich."))
            }
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "ID fehlt."))
            val deleted = NewsServerRepository.delete(id)
            if (deleted) {
                call.respond(SimpleApiResponse(true, "Neuigkeit erfolgreich gelöscht."))
            } else {
                call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Neuigkeit nicht gefunden."))
            }
        }

        post("/upload-image") {
            if (!verifyAdminAccess(call)) {
                return@post call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Admin-Zugriff erforderlich."))
            }
            try {
                ServerConfig.ensureNewsImagesDirectory()
                var savedFileName: String? = null
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val ext = part.originalFileName?.substringAfterLast('.', "png") ?: "png"
                        val fileName = "news_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
                        val file = File(ServerConfig.newsImagesDir, fileName)
                        part.streamProvider().use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        savedFileName = fileName
                    }
                    part.dispose()
                }

                if (savedFileName != null) {
                    call.respond(mapOf("imageUrl" to "/api/news/images/$savedFileName"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Keine Bilddatei empfangen."))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Upload-Fehler: ${e.message}"))
            }
        }
    }
}

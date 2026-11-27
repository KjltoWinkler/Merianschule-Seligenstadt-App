package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.data.news.CreateNewsRequest
import de.kjltowinkler.msapp.data.news.NewsItem
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.repository.NewsServerRepository
import de.kjltowinkler.msapp.server.security.JwtService
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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

        // CORS-Proxy für WordPress-Beiträge (z. B. für lokale WebApp-Tests)
        get("/wp-posts") {
            val perPage = call.request.queryParameters["per_page"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
            try {
                val client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                val req = HttpRequest.newBuilder()
                    .uri(URI.create("https://merianschule-seligenstadt.de/wp-json/wp/v2/posts?_embed&per_page=$perPage"))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build()
                val res = client.send(req, HttpResponse.BodyHandlers.ofString())
                call.respondText(
                    text = res.body(),
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.fromValue(res.statusCode())
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadGateway, SimpleApiResponse(false, "WordPress-API nicht erreichbar: ${e.message}"))
            }
        }

        // CORS-Proxy für Artikel-Bilder der Merianschule-Website
        get("/proxy-image") {
            val targetUrl = call.request.queryParameters["url"]?.trim()
            if (targetUrl.isNullOrBlank() || !targetUrl.startsWith("https://merianschule-seligenstadt.de/")) {
                return@get call.respond(HttpStatusCode.BadRequest, "Ungültige Bild-URL.")
            }
            try {
                val client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                val req = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build()
                val res = client.send(req, HttpResponse.BodyHandlers.ofByteArray())
                if (res.statusCode() !in 200..299) {
                    return@get call.respond(HttpStatusCode.fromValue(res.statusCode()), "Bild konnte nicht geladen werden.")
                }
                val contentTypeHeader = res.headers().firstValue("Content-Type").orElse("image/jpeg")
                val contentType = runCatching { ContentType.parse(contentTypeHeader) }.getOrDefault(ContentType.Image.JPEG)
                call.response.header(HttpHeaders.CacheControl, "private, max-age=3600")
                call.respondBytes(res.body(), contentType = contentType)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadGateway, "Bild-Proxy-Fehler: ${e.message}")
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

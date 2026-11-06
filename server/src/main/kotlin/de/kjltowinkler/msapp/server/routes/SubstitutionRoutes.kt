package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import de.kjltowinkler.msapp.server.security.JwtService
import de.kjltowinkler.msapp.server.websocket.TaskWebSocketManager
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable

@Serializable
data class SubstitutionMetaResponse(
    val count: Int,
    val lastUpdated: Long,
    val lastUpdatedIso: String,
    val lastUpdatedText: String
)

fun Route.substitutionRoutes() {
    route("/api/substitutions") {
        webSocket("/ws") {
            val token = call.request.queryParameters["token"]
                ?: call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
            val userId = if (!token.isNullOrBlank()) JwtService.verifyToken(token) else null
            val effectiveUserId = userId ?: "anon_${java.util.UUID.randomUUID()}"

            TaskWebSocketManager.register(effectiveUserId, this)
            try {
                send(Frame.Text("""{"event":"connected","userId":"$effectiveUserId"}"""))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.contains("ping", ignoreCase = true)) {
                            send(Frame.Text("""{"event":"pong"}"""))
                        }
                    }
                }
            } finally {
                TaskWebSocketManager.unregister(effectiveUserId, this)
            }
        }
        get {
            val forceRefresh = call.request.queryParameters["forceRefresh"] == "true"
            val classFilter = call.request.queryParameters["class"]

            // Wenn Anmeldedaten vorhanden sind und die Daten älter als 5 Minuten sind (oder forceRefresh verlangt wird):
            // Synchronisiere automatisch mit Schulportal Hessen
            val syncService = de.kjltowinkler.msapp.server.service.SchulportalSyncService
            if (syncService.hasCredentials && (forceRefresh || System.currentTimeMillis() - syncService.lastSyncTimestamp > 5 * 60 * 1000L)) {
                try {
                    syncService.syncNow(force = forceRefresh)
                } catch (e: Exception) {
                    println("Schulportal-Abruf während GET /substitutions fehlgeschlagen: ${e.message}")
                }
            }

            val entries = if (!classFilter.isNullOrBlank()) {
                SubstitutionServerRepository.getByClass(classFilter)
            } else {
                SubstitutionServerRepository.getAll()
            }

            val syncTs = if (syncService.lastSyncTimestamp > 0) {
                syncService.lastSyncTimestamp
            } else {
                SubstitutionServerRepository.getLastUpdated()
            }

            val lastUpdatedText = if (syncTs > 0) {
                val zdt = java.time.Instant.ofEpochMilli(syncTs).atZone(java.time.ZoneId.of("Europe/Berlin"))
                java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm:ss").format(zdt)
            } else {
                SubstitutionServerRepository.getLastUpdatedText()
                    .removePrefix("Letzte Aktualisierung:")
                    .removePrefix("Stand:")
                    .trim()
            }

            if (lastUpdatedText.isNotBlank()) {
                call.response.headers.append("X-Last-Updated", lastUpdatedText)
            }
            if (syncTs > 0) {
                call.response.headers.append("X-Last-Sync-Timestamp", syncTs.toString())
            }
            call.respond(HttpStatusCode.OK, entries)
        }

        get("/raw") {
            val raw = SubstitutionServerRepository.getRawJson()
            call.respondText(raw, ContentType.Application.Json, HttpStatusCode.OK)
        }

        get("/meta") {
            val count = SubstitutionServerRepository.getCount()
            val lastUpdated = SubstitutionServerRepository.getLastUpdated()
            val iso = if (lastUpdated > 0) java.time.Instant.ofEpochMilli(lastUpdated).toString() else ""
            val text = SubstitutionServerRepository.getLastUpdatedText().ifBlank {
                de.kjltowinkler.msapp.server.service.SchulportalSyncService.lastSyncText
            }
            call.respond(HttpStatusCode.OK, SubstitutionMetaResponse(count, lastUpdated, iso, text))
        }
    }
}

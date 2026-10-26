package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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

            val lastUpdatedText = SubstitutionServerRepository.getLastUpdatedText().ifBlank {
                syncService.lastSyncText.ifBlank { "Letzte Aktualisierung: " + java.time.LocalDate.now() }
            }

            call.response.headers.append("X-Last-Updated", lastUpdatedText)
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

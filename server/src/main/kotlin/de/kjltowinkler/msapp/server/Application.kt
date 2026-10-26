package de.kjltowinkler.msapp.server

import de.kjltowinkler.msapp.data.auth.RegisterRequest
import de.kjltowinkler.msapp.data.auth.SimpleApiResponse
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import de.kjltowinkler.msapp.server.repository.SubstitutionServerRepository
import de.kjltowinkler.msapp.server.repository.TimetableServerRepository
import de.kjltowinkler.msapp.server.repository.UserRepository
import de.kjltowinkler.msapp.server.routes.*
import de.kjltowinkler.msapp.server.security.JwtService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    println("==================================================")
    println("  Merianschule Seligenstadt App - Backend Server  ")
    println("==================================================")
    println("Port: ${ServerConfig.port}")
    println("Data Directory: ${ServerConfig.dataDir}")
    println("DB Path: ${ServerConfig.dbPath}")
    println("Admin Key set: ${ServerConfig.adminKey.isNotBlank()}")
    println("==================================================")

    DatabaseFactory.init()

    runBlocking {
        // Initialize default data if tables are empty
        SubstitutionServerRepository.initDefaultsIfEmpty()
        TimetableServerRepository.initDefaultsIfEmpty()
        de.kjltowinkler.msapp.server.service.SchulportalSyncService.init()

        // Ensure default admin user exists
        val existingAdmin = UserRepository.findByUsernameOrEmail("admin")
        if (existingAdmin == null) {
            val adminPassword = System.getenv("ADMIN_DEFAULT_PASSWORD") ?: "MerianAdmin2026!"
            try {
                UserRepository.createUser(
                    RegisterRequest(
                        username = "admin",
                        email = "admin@merianschule-seligenstadt.de",
                        password = adminPassword,
                        fullName = "Administrator",
                        className = "Schulleitung",
                        schoolType = "Verwaltung",
                        classTeacher = "Schulleitung"
                    ),
                    role = "ADMIN"
                )
                println("Standard-Admin-Benutzer initialisiert: admin (Passwort: $adminPassword)")
            } catch (e: Exception) {
                println("Admin-User existiert bereits oder Initialisierungsfehler: ${e.message}")
            }
        }
    }

    embeddedServer(Netty, port = ServerConfig.port, host = ServerConfig.host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    de.kjltowinkler.msapp.server.service.SchulportalSyncService.startBackgroundScheduler(this)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Admin-Key")
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled Exception: ", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                SimpleApiResponse(false, "Interner Serverfehler: ${cause.localizedMessage ?: "Unbekannter Fehler"}")
            )
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = ServerConfig.jwtRealm
            verifier(JwtService.verifier)
            validate { credential ->
                JwtService.validateCredential(credential)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Token ist ungültig oder abgelaufen."))
            }
        }
    }

@kotlinx.serialization.Serializable
data class RootInfoResponse(
    val service: String,
    val status: String,
    val version: String,
    val docs: String
)

@kotlinx.serialization.Serializable
data class HealthStatusResponse(
    val status: String,
    val timestamp: Long
)

    routing {
        get("/") {
            call.respond(RootInfoResponse(
                service = "Merianschule Seligenstadt App Backend",
                status = "UP",
                version = "1.0.0",
                docs = "/api/admin/stats"
            ))
        }

        get("/health") {
            call.respond(HealthStatusResponse(status = "OK", timestamp = System.currentTimeMillis()))
        }

        authRoutes()
        taskRoutes()
        substitutionRoutes()
        timetableRoutes()
        adminRoutes()
    }
}

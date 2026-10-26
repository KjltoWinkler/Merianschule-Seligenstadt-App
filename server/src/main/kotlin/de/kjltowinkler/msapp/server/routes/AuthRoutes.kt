package de.kjltowinkler.msapp.server.routes

import de.kjltowinkler.msapp.data.auth.*
import de.kjltowinkler.msapp.server.repository.UserRepository
import de.kjltowinkler.msapp.server.security.JwtService
import de.kjltowinkler.msapp.server.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val request = runCatching { call.receive<RegisterRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Registrierungsdaten."))
                return@post
            }

            if (request.username.isBlank() || request.password.isBlank() || request.email.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Benutzername, E-Mail und Passwort sind erforderlich."))
                return@post
            }

            try {
                val user = UserRepository.createUser(request)
                val token = JwtService.generateToken(user.id, user.username, user.role)
                call.respond(HttpStatusCode.Created, AuthResponse(token, user.toDto()))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Conflict, SimpleApiResponse(false, e.message ?: "Benutzer existiert bereits."))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler bei der Registrierung: ${e.message}"))
            }
        }

        post("/login") {
            val request = runCatching { call.receive<LoginRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anmeldedaten."))
                return@post
            }

            val user = UserRepository.findByUsernameOrEmail(request.usernameOrEmail.trim())
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Benutzername/E-Mail oder Passwort ist falsch."))
                return@post
            }

            val passwordValid = PasswordHasher.verifyPassword(request.password, user.passwordSalt, user.passwordHash)
            if (!passwordValid) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Benutzername/E-Mail oder Passwort ist falsch."))
                return@post
            }

            val token = JwtService.generateToken(user.id, user.username, user.role)
            call.respond(HttpStatusCode.OK, AuthResponse(token, user.toDto()))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@get
                }

                val user = UserRepository.findById(userId)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user.toDto())
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                }
            }

            put("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@put
                }

                val updateReq = runCatching { call.receive<UpdateProfileRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Profildaten."))
                    return@put
                }

                val updatedUser = UserRepository.updateProfile(userId, updateReq)
                if (updatedUser != null) {
                    call.respond(HttpStatusCode.OK, updatedUser)
                } else {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                }
            }

            post("/change-password") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@post
                }

                val req = runCatching { call.receive<ChangePasswordRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anfrage."))
                    return@post
                }

                val user = UserRepository.findById(userId)
                if (user == null || !PasswordHasher.verifyPassword(req.oldPassword, user.passwordSalt, user.passwordHash)) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Das alte Passwort ist nicht korrekt."))
                    return@post
                }

                val newSalt = PasswordHasher.generateSalt()
                val newHash = PasswordHasher.hashPassword(req.newPassword, newSalt)
                val success = UserRepository.updatePassword(userId, newHash, newSalt)

                if (success) {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Passwort erfolgreich geändert."))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Ändern des Passworts."))
                }
            }
        }
    }
}

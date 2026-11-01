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
        post("/check") {
            val request = runCatching { call.receive<CheckAccountRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anfrage."))
                return@post
            }

            val identifier = request.usernameOrEmail.trim()
            if (identifier.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Benutzername oder E-Mail ist erforderlich."))
                return@post
            }

            val user = UserRepository.findByUsernameOrEmail(identifier)
            if (user != null) {
                call.respond(HttpStatusCode.OK, CheckAccountResponse(
                    exists = true,
                    username = user.username,
                    email = user.email,
                    fullName = user.fullName,
                    className = user.className,
                    schoolType = user.schoolType,
                    classTeacher = user.classTeacher,
                    avatarType = user.avatarType,
                    avatarColorHex = user.avatarColorHex,
                    studentId = user.studentId,
                    hasCustomPassword = user.hasCustomPassword,
                    isTwoFactorEnabled = user.isTwoFactorEnabled
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, CheckAccountResponse(
                    exists = false,
                    message = "Dieser Account existiert nicht."
                ))
            }
        }

        get("/check") {
            val identifier = call.request.queryParameters["usernameOrEmail"]?.trim()
            if (identifier.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Benutzername oder E-Mail ist erforderlich."))
                return@get
            }

            val user = UserRepository.findByUsernameOrEmail(identifier)
            if (user != null) {
                call.respond(HttpStatusCode.OK, CheckAccountResponse(
                    exists = true,
                    username = user.username,
                    email = user.email,
                    fullName = user.fullName,
                    className = user.className,
                    schoolType = user.schoolType,
                    classTeacher = user.classTeacher,
                    avatarType = user.avatarType,
                    avatarColorHex = user.avatarColorHex,
                    studentId = user.studentId,
                    hasCustomPassword = user.hasCustomPassword,
                    isTwoFactorEnabled = user.isTwoFactorEnabled
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, CheckAccountResponse(
                    exists = false,
                    message = "Dieser Account existiert nicht."
                ))
            }
        }

        post("/set-initial-password") {
            val req = runCatching { call.receive<SetInitialPasswordRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anfrage."))
                return@post
            }

            val user = UserRepository.findByUsernameOrEmail(req.usernameOrEmail.trim())
            if (user == null || !PasswordHasher.verifyPassword(req.initialPassword, user.passwordSalt, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Das Einmal-Passwort ist nicht korrekt."))
                return@post
            }

            if (req.newPassword.trim().length < 6) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Das neue Passwort muss mindestens 6 Zeichen lang sein."))
                return@post
            }

            val newSalt = PasswordHasher.generateSalt()
            val newHash = PasswordHasher.hashPassword(req.newPassword.trim(), newSalt)
            UserRepository.updatePassword(user.id, newHash, newSalt)
            UserRepository.setHasCustomPassword(user.id, true)

            val updatedUser = UserRepository.findById(user.id) ?: user
            val token = JwtService.generateToken(user.id, user.username, user.role)
            call.respond(HttpStatusCode.OK, AuthResponse(token, updatedUser.toDto()))
        }

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

            // Prüfe, ob 2FA für diesen Account aktiv ist
            if (user.isTwoFactorEnabled && !user.twoFactorSecret.isNullOrBlank()) {
                val tempToken = JwtService.generate2FaTempToken(user.id, user.username)
                call.respond(HttpStatusCode.OK, AuthResponse(
                    requiresTwoFactor = true,
                    tempToken = tempToken,
                    user = user.toDto()
                ))
                return@post
            }

            val token = JwtService.generateToken(user.id, user.username, user.role)
            call.respond(HttpStatusCode.OK, AuthResponse(token, user.toDto()))
        }

        post("/2fa/verify-login") {
            val req = runCatching { call.receive<TwoFactorLoginRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anfrage."))
                return@post
            }

            val userClaims = JwtService.verify2FaTempToken(req.tempToken)
            if (userClaims == null) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Deine 2FA-Sitzung ist abgelaufen. Bitte melde dich erneut an."))
                return@post
            }

            val (userId, _) = userClaims
            val user = UserRepository.findById(userId)
            if (user == null || !user.isTwoFactorEnabled || user.twoFactorSecret.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "2FA ist für diesen Account nicht aktiv."))
                return@post
            }

            val isValid = de.kjltowinkler.msapp.server.security.TotpService.verifyCode(user.twoFactorSecret, req.code)
            if (!isValid) {
                call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Der Bestätigungscode ist ungültig oder abgelaufen."))
                return@post
            }

            val token = JwtService.generateToken(user.id, user.username, user.role)
            call.respond(HttpStatusCode.OK, AuthResponse(token = token, user = user.toDto()))
        }

        authenticate("auth-jwt") {
            post("/2fa/setup") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@post
                }

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                    return@post
                }

                val secret = de.kjltowinkler.msapp.server.security.TotpService.generateSecret()
                val otpauthUri = de.kjltowinkler.msapp.server.security.TotpService.generateOtpAuthUri(
                    issuer = "Merianschule",
                    accountName = user.username,
                    secret = secret
                )
                val formattedSecret = de.kjltowinkler.msapp.server.security.TotpService.formatSecret(secret)

                UserRepository.setTwoFactorTempSecret(userId, secret)

                call.respond(HttpStatusCode.OK, TwoFactorSetupResponse(
                    secret = formattedSecret,
                    otpauthUri = otpauthUri
                ))
            }

            post("/2fa/confirm") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@post
                }

                val req = runCatching { call.receive<ConfirmTwoFactorRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anfrage."))
                    return@post
                }

                val user = UserRepository.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, SimpleApiResponse(false, "Benutzer nicht gefunden."))
                    return@post
                }

                val secretToVerify = user.twoFactorTempSecret ?: user.twoFactorSecret
                if (secretToVerify.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Keine 2FA-Einrichtung gestartet. Bitte starte das Setup erneut."))
                    return@post
                }

                val isValid = de.kjltowinkler.msapp.server.security.TotpService.verifyCode(secretToVerify, req.code)
                if (!isValid) {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Der Bestätigungscode ist falsch oder abgelaufen. Bitte prüfe deine Authenticator-App."))
                    return@post
                }

                UserRepository.activateTwoFactor(userId, secretToVerify)
                call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "Zwei-Faktor-Authentifizierung wurde erfolgreich aktiviert!"))
            }

            post("/2fa/disable") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@post
                }

                UserRepository.disableTwoFactor(userId)
                call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "2FA wurde deaktiviert."))
            }

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

            post("/2fa") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, SimpleApiResponse(false, "Nicht autorisiert."))
                    return@post
                }

                val req = runCatching { call.receive<Update2FaRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, SimpleApiResponse(false, "Ungültige Anfrage."))
                    return@post
                }

                val success = UserRepository.setTwoFactorEnabled(userId, req.enabled)
                if (success) {
                    call.respond(HttpStatusCode.OK, SimpleApiResponse(true, "2FA-Status erfolgreich aktualisiert."))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, SimpleApiResponse(false, "Fehler beim Aktualisieren des 2FA-Status."))
                }
            }
        }
    }
}

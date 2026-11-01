package de.kjltowinkler.msapp.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import de.kjltowinkler.msapp.server.config.ServerConfig
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.Date

object JwtService {
    private val algorithm: Algorithm by lazy {
        Algorithm.HMAC256(ServerConfig.jwtSecret)
    }

    val verifier: JWTVerifier by lazy {
        JWT.require(algorithm)
            .withAudience(ServerConfig.jwtAudience)
            .withIssuer(ServerConfig.jwtIssuer)
            .build()
    }

    // 30 days validity for smooth mobile app experience
    private const val VALIDITY_MS = 30L * 24 * 60 * 60 * 1000

    fun generateToken(userId: String, username: String, role: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withAudience(ServerConfig.jwtAudience)
            .withIssuer(ServerConfig.jwtIssuer)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withClaim("role", role)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + VALIDITY_MS))
            .sign(algorithm)
    }

    fun generate2FaTempToken(userId: String, username: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withAudience(ServerConfig.jwtAudience)
            .withIssuer(ServerConfig.jwtIssuer)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withClaim("purpose", "2fa_challenge")
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 5 * 60 * 1000)) // 5 Minuten gültig
            .sign(algorithm)
    }

    fun verify2FaTempToken(token: String): Pair<String, String>? {
        return try {
            val decoded = verifier.verify(token)
            val purpose = decoded.getClaim("purpose").asString()
            if (purpose != "2fa_challenge") return null
            val userId = decoded.getClaim("userId").asString() ?: return null
            val username = decoded.getClaim("username").asString() ?: return null
            userId to username
        } catch (_: Exception) {
            null
        }
    }

    fun validateCredential(credential: JWTCredential): JWTPrincipal? {
        val userId = credential.payload.getClaim("userId").asString()
        val username = credential.payload.getClaim("username").asString()
        val purpose = credential.payload.getClaim("purpose").asString()
        // Temp tokens for 2FA challenges cannot be used as regular authentication tokens
        if (purpose == "2fa_challenge") return null
        return if (!userId.isNullOrBlank() && !username.isNullOrBlank()) {
            JWTPrincipal(credential.payload)
        } else {
            null
        }
    }
}

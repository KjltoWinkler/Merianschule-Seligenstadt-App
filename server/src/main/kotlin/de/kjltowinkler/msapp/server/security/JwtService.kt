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

    fun validateCredential(credential: JWTCredential): JWTPrincipal? {
        val userId = credential.payload.getClaim("userId").asString()
        val username = credential.payload.getClaim("username").asString()
        return if (!userId.isNullOrBlank() && !username.isNullOrBlank()) {
            JWTPrincipal(credential.payload)
        } else {
            null
        }
    }
}

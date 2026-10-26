package de.kjltowinkler.msapp.server.config

import java.io.File

object ServerConfig {
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host: String = System.getenv("HOST") ?: "0.0.0.0"

    val jwtSecret: String = System.getenv("JWT_SECRET") ?: "merianschule-seligenstadt-secret-key-2026-safe-production"
    val jwtIssuer: String = System.getenv("JWT_ISSUER") ?: "https://merianschule-app.de"
    val jwtAudience: String = System.getenv("JWT_AUDIENCE") ?: "merianschule-app-users"
    val jwtRealm: String = "Merianschule App Access"

    val adminKey: String = System.getProperty("ADMIN_SECRET_KEY") ?: System.getenv("ADMIN_SECRET_KEY") ?: "merian-admin-secret-2026"

    val dataDir: String = System.getProperty("DATA_DIR") ?: System.getenv("DATA_DIR") ?: "data"
    val dbPath: String = System.getProperty("DB_PATH") ?: System.getenv("DB_PATH") ?: "$dataDir/merianschule.db"

    fun ensureDataDirectory() {
        val dir = File(dataDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
}

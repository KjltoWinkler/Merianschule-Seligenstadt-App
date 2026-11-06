package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.server.database.DatabaseFactory

object SettingsServerRepository {

    suspend fun getSettingsJson(userId: String): String? {
        return DatabaseFactory.query { conn ->
            val sql = "SELECT settings_json FROM user_settings WHERE user_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("settings_json") else null
                }
            }
        }
    }

    suspend fun saveSettingsJson(userId: String, settingsJson: String) {
        DatabaseFactory.query { conn ->
            val sql = """
                INSERT INTO user_settings (user_id, settings_json, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    settings_json = excluded.settings_json,
                    updated_at = excluded.updated_at
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.setString(2, settingsJson)
                stmt.setLong(3, System.currentTimeMillis())
                stmt.executeUpdate()
            }
        }
    }
}

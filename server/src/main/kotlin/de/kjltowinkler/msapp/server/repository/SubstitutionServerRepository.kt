package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.substitution.SubstitutionEntry
import de.kjltowinkler.msapp.data.substitution.SubstitutionRepository
import de.kjltowinkler.msapp.server.config.ServerConfig
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.ResultSet

object SubstitutionServerRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getJsonFile(): File = File(ServerConfig.dataDir, "substitutions.json")

    private fun mapEntry(rs: ResultSet): SubstitutionEntry = SubstitutionEntry(
        id = rs.getString("id"),
        date = rs.getString("date"),
        dayLabel = rs.getString("day_label"),
        period = rs.getString("period"),
        subject = rs.getString("subject"),
        className = rs.getString("class_name"),
        type = rs.getString("type"),
        details = rs.getString("details"),
        roomInfo = rs.getString("room_info"),
        time = rs.getString("time"),
        isNew = rs.getInt("is_new") == 1,
        isMarked = rs.getInt("is_marked") == 1
    )

    suspend fun initDefaultsIfEmpty() {
        val count = getCount()
        if (count == 0) {
            val file = getJsonFile()
            if (file.exists() && file.length() > 0) {
                try {
                    val raw = file.readText()
                    val parsed = json.decodeFromString<List<SubstitutionEntry>>(raw)
                    if (parsed.isNotEmpty()) {
                        replaceEntries(parsed, raw)
                        return
                    }
                } catch (e: Exception) {
                    println("Warnung beim Laden von ${file.absolutePath}: ${e.message}")
                }
            }
            // Seed with default entries
            replaceEntries(SubstitutionRepository.defaultEntries)
        }
    }

    suspend fun getAll(): List<SubstitutionEntry> = DatabaseFactory.query { conn ->
        val sql = "SELECT * FROM substitutions ORDER BY date ASC, period ASC;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val list = mutableListOf<SubstitutionEntry>()
                while (rs.next()) list.add(mapEntry(rs))
                list
            }
        }
    }

    suspend fun getByClass(className: String): List<SubstitutionEntry> = DatabaseFactory.query { conn ->
        val normalized = className.trim().lowercase()
        val sql = "SELECT * FROM substitutions WHERE lower(class_name) LIKE ? ORDER BY date ASC, period ASC;"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, "%$normalized%")
            stmt.executeQuery().use { rs ->
                val list = mutableListOf<SubstitutionEntry>()
                while (rs.next()) list.add(mapEntry(rs))
                list
            }
        }
    }

    suspend fun getRawJson(): String {
        val file = getJsonFile()
        return if (file.exists() && file.length() > 0) {
            file.readText()
        } else {
            val all = getAll()
            json.encodeToString(all)
        }
    }

    suspend fun replaceEntries(entries: List<SubstitutionEntry>, rawJsonContent: String? = null): Int {
        val now = System.currentTimeMillis()

        DatabaseFactory.query { conn ->
            conn.autoCommit = false
            try {
                val incomingDates = entries.map { it.date }.filter { it.isNotBlank() }.toSet()
                if (incomingDates.isNotEmpty()) {
                    val placeholders = incomingDates.joinToString(",") { "?" }
                    conn.prepareStatement("DELETE FROM substitutions WHERE date IN ($placeholders);").use { stmt ->
                        incomingDates.forEachIndexed { idx, d -> stmt.setString(idx + 1, d) }
                        stmt.executeUpdate()
                    }
                } else {
                    conn.createStatement().use { it.execute("DELETE FROM substitutions;") }
                }

                val sql = """
                    INSERT INTO substitutions (
                        id, date, day_label, period, subject, class_name, 
                        type, details, room_info, time, is_new, is_marked, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    for (entry in entries) {
                        stmt.setString(1, entry.id)
                        stmt.setString(2, entry.date)
                        stmt.setString(3, entry.dayLabel)
                        stmt.setString(4, entry.period)
                        stmt.setString(5, entry.subject)
                        stmt.setString(6, entry.className)
                        stmt.setString(7, entry.type)
                        stmt.setString(8, entry.details)
                        stmt.setString(9, entry.roomInfo)
                        stmt.setString(10, entry.time)
                        stmt.setInt(11, if (entry.isNew) 1 else 0)
                        stmt.setInt(12, if (entry.isMarked) 1 else 0)
                        stmt.setLong(13, now)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                // Update metadata
                val metaSql = """
                    INSERT INTO server_metadata (key, value, updated_at) 
                    VALUES ('substitutions_last_updated', ?, ?)
                    ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at;
                """.trimIndent()
                conn.prepareStatement(metaSql).use { stmt ->
                    stmt.setString(1, now.toString())
                    stmt.setLong(2, now)
                    stmt.executeUpdate()
                }

                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }

        // Also persist to disk as backup / easy manual access
        try {
            ServerConfig.ensureDataDirectory()
            val allEntries = getAll()
            getJsonFile().writeText(json.encodeToString(allEntries))
        } catch (e: Exception) {
            println("Fehler beim Speichern der substitutions.json: ${e.message}")
        }

        return entries.size
    }

    suspend fun getLastUpdated(): Long = DatabaseFactory.query { conn ->
        val sql = "SELECT value FROM server_metadata WHERE key = 'substitutions_last_updated';"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getString("value").toLongOrNull() ?: 0L else 0L
            }
        }
    }

    suspend fun getLastUpdatedText(): String = DatabaseFactory.query { conn ->
        val sql = "SELECT value FROM server_metadata WHERE key = 'substitutions_last_updated_text';"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getString("value") ?: "" else ""
            }
        }
    }

    suspend fun getCount(): Int = DatabaseFactory.query { conn ->
        val sql = "SELECT COUNT(*) FROM substitutions;"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}

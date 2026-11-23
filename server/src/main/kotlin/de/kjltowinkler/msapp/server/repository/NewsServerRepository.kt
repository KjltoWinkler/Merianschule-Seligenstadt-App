package de.kjltowinkler.msapp.server.repository

import de.kjltowinkler.msapp.data.news.NewsItem
import de.kjltowinkler.msapp.data.news.NewsType
import de.kjltowinkler.msapp.server.database.DatabaseFactory
import java.util.UUID

object NewsServerRepository {

    suspend fun getAll(): List<NewsItem> = DatabaseFactory.query { conn ->
        val sql = "SELECT id, title, description, image_url, type, date_text, timestamp, author, is_featured, created_at, updated_at FROM news_items ORDER BY is_featured DESC, timestamp DESC, created_at DESC"
        conn.prepareStatement(sql).use { stmt ->
            val rs = stmt.executeQuery()
            val list = mutableListOf<NewsItem>()
            while (rs.next()) {
                list.add(
                    NewsItem(
                        id = rs.getString("id"),
                        title = rs.getString("title"),
                        description = rs.getString("description"),
                        imageUrl = rs.getString("image_url"),
                        type = try { NewsType.valueOf(rs.getString("type")) } catch (_: Exception) { NewsType.NEWS },
                        dateText = rs.getString("date_text") ?: "",
                        timestamp = rs.getLong("timestamp"),
                        author = rs.getString("author") ?: "Schulleitung",
                        isFeatured = rs.getInt("is_featured") == 1,
                        createdAt = rs.getLong("created_at"),
                        updatedAt = rs.getLong("updated_at")
                    )
                )
            }
            list
        }
    }

    suspend fun getById(id: String): NewsItem? = DatabaseFactory.query { conn ->
        val sql = "SELECT id, title, description, image_url, type, date_text, timestamp, author, is_featured, created_at, updated_at FROM news_items WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                NewsItem(
                    id = rs.getString("id"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    imageUrl = rs.getString("image_url"),
                    type = try { NewsType.valueOf(rs.getString("type")) } catch (_: Exception) { NewsType.NEWS },
                    dateText = rs.getString("date_text") ?: "",
                    timestamp = rs.getLong("timestamp"),
                    author = rs.getString("author") ?: "Schulleitung",
                    isFeatured = rs.getInt("is_featured") == 1,
                    createdAt = rs.getLong("created_at"),
                    updatedAt = rs.getLong("updated_at")
                )
            } else null
        }
    }

    suspend fun create(item: NewsItem): NewsItem = DatabaseFactory.query { conn ->
        val now = System.currentTimeMillis()
        val generatedId = if (item.id.isNotBlank()) item.id else UUID.randomUUID().toString()
        val sql = """
            INSERT INTO news_items (id, title, description, image_url, type, date_text, timestamp, author, is_featured, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, generatedId)
            stmt.setString(2, item.title)
            stmt.setString(3, item.description)
            stmt.setString(4, item.imageUrl)
            stmt.setString(5, item.type.name)
            stmt.setString(6, item.dateText)
            stmt.setLong(7, if (item.timestamp > 0) item.timestamp else now)
            stmt.setString(8, item.author)
            stmt.setInt(9, if (item.isFeatured) 1 else 0)
            stmt.setLong(10, if (item.createdAt > 0) item.createdAt else now)
            stmt.setLong(11, now)
            stmt.executeUpdate()
        }
        item.copy(id = generatedId, createdAt = if (item.createdAt > 0) item.createdAt else now, updatedAt = now)
    }

    suspend fun update(item: NewsItem): Boolean = DatabaseFactory.query { conn ->
        val now = System.currentTimeMillis()
        val sql = """
            UPDATE news_items
            SET title = ?, description = ?, image_url = ?, type = ?, date_text = ?, timestamp = ?, author = ?, is_featured = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, item.title)
            stmt.setString(2, item.description)
            stmt.setString(3, item.imageUrl)
            stmt.setString(4, item.type.name)
            stmt.setString(5, item.dateText)
            stmt.setLong(6, item.timestamp)
            stmt.setString(7, item.author)
            stmt.setInt(8, if (item.isFeatured) 1 else 0)
            stmt.setLong(9, now)
            stmt.setString(10, item.id)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun delete(id: String): Boolean = DatabaseFactory.query { conn ->
        val sql = "DELETE FROM news_items WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun seedInitialNewsIfEmpty() {
        val existing = getAll()
        if (existing.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val sampleNews = listOf(
            NewsItem(
                id = "news_sample_1",
                title = "Projektwoche 2026: Kreative Ergebnisse",
                description = "Unsere Schülerinnen und Schüler präsentierten stolz ihre herausragenden Arbeiten und Kunstprojekte auf der Freitreppe.",
                imageUrl = "res:news_stairs",
                type = NewsType.NEWS,
                dateText = "21.11.2026",
                timestamp = now,
                author = "Schulleitung",
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            NewsItem(
                id = "news_sample_2",
                title = "Tag der offenen Tür",
                description = "Spannende Mitmach-Experimente, Schulführungen und lebendige Aktionen für alle neuen Fünftklässler und Eltern.",
                imageUrl = "res:news_cups",
                type = NewsType.EVENT,
                dateText = "28.11.2026",
                timestamp = now - 86400000L,
                author = "Schulleitung",
                isFeatured = true,
                createdAt = now - 86400000L,
                updatedAt = now
            ),
            NewsItem(
                id = "news_sample_3",
                title = "Zertifikatsverleihung: Sprachdiplome",
                description = "Feierliche Übergabe der Cambridge English & DELF-Sprachzertifikate im festlich geschmückten Foyer.",
                imageUrl = "res:news_certificates",
                type = NewsType.NEWS,
                dateText = "05.12.2026",
                timestamp = now - 172800000L,
                author = "Fachschaft Sprachen",
                isFeatured = true,
                createdAt = now - 172800000L,
                updatedAt = now
            ),
            NewsItem(
                id = "news_sample_4",
                title = "Winterbasar der Merianschule",
                description = "Am Freitag ab 15 Uhr auf dem Schulhof. Schüler backen Waffeln und verkaufen Bastelarbeiten. Der Erlös geht an den Förderverein.",
                imageUrl = null,
                type = NewsType.EVENT,
                dateText = "12.12.2026",
                timestamp = now - 259200000L,
                author = "Förderverein",
                isFeatured = false,
                createdAt = now - 259200000L,
                updatedAt = now
            ),
            NewsItem(
                id = "news_sample_5",
                title = "Elternsprechtag im Dezember",
                description = "Die Terminbuchung für Eltern ist ab sofort über das Schulportal freigeschaltet. Gespräche finden wahlweise vor Ort oder digital statt.",
                imageUrl = null,
                type = NewsType.ANNOUNCEMENT,
                dateText = "18.12.2026",
                timestamp = now - 345600000L,
                author = "Schulleitung",
                isFeatured = false,
                createdAt = now - 345600000L,
                updatedAt = now
            ),
            NewsItem(
                id = "news_sample_6",
                title = "Neuer Mensa-Speiseplan online",
                description = "Der Küchenbetrieb stellt ab kommenden Montag auf den erweiterten Winter-Speiseplan mit zusätzlichen frischen Bowls und Suppen um.",
                imageUrl = null,
                type = NewsType.NEWS,
                dateText = "01.12.2026",
                timestamp = now - 432000000L,
                author = "Mensa-Team",
                isFeatured = false,
                createdAt = now - 432000000L,
                updatedAt = now
            )
        )

        for (item in sampleNews) {
            create(item)
        }
    }
}

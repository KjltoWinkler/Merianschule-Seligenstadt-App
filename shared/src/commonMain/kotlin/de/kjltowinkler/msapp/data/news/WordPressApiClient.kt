package de.kjltowinkler.msapp.data.news

import de.kjltowinkler.msapp.data.backend.BackendApiClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class WpPost(
    val id: Long,
    val date: String = "",
    val link: String = "",
    val sticky: Boolean = false,
    val title: WpRendered = WpRendered(),
    val content: WpRendered = WpRendered(),
    val excerpt: WpRendered = WpRendered(),
    @SerialName("_embedded") val embedded: WpEmbedded? = null
)

@Serializable
internal data class WpRendered(
    val rendered: String = ""
)

@Serializable
internal data class WpEmbedded(
    val author: List<WpAuthor>? = null,
    @SerialName("wp:featuredmedia") val featuredMedia: List<WpFeaturedMedia>? = null
)

@Serializable
internal data class WpAuthor(
    val id: Long = 0,
    val name: String = ""
)

@Serializable
internal data class WpFeaturedMedia(
    val id: Long? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("media_details") val mediaDetails: WpMediaDetails? = null
)

@Serializable
internal data class WpMediaDetails(
    val sizes: Map<String, WpMediaSize>? = null
)

@Serializable
internal data class WpMediaSize(
    @SerialName("source_url") val sourceUrl: String? = null
)

/**
 * Ruft die News und Beiträge der Merianschule Seligenstadt direkt
 * über die öffentliche WordPress REST API ab.
 */
object WordPressApiClient {
    private const val BASE_URL = "https://merianschule-seligenstadt.de/wp-json/wp/v2/posts"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchPosts(perPage: Int = 20): List<NewsItem> {
        val response = BackendApiClient.httpClient.get("$BASE_URL?_embed&per_page=$perPage")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("WordPress API returned ${response.status}")
        }
        val body = response.bodyAsText()
        val posts = json.decodeFromString<List<WpPost>>(body)

        return posts.mapIndexed { index, post ->
            val cleanTitle = cleanHtml(post.title.rendered)
            val fullContent = cleanHtml(post.content.rendered)
            val excerptContent = cleanHtml(post.excerpt.rendered)
            val description = if (fullContent.isNotBlank()) fullContent else excerptContent

            val embeddedMediaUrl = post.embedded?.featuredMedia?.firstOrNull()?.let { media ->
                media.mediaDetails?.sizes?.get("large")?.sourceUrl
                    ?: media.mediaDetails?.sizes?.get("medium_large")?.sourceUrl
                    ?: media.sourceUrl
            }
            val contentImages = extractImageUrls(post.content.rendered)
            val allImages = (listOfNotNull(embeddedMediaUrl) + contentImages).distinct()
            val primaryImageUrl = allImages.firstOrNull()

            val authorName = post.embedded?.author?.firstOrNull()?.name?.trim()?.ifBlank { null } ?: "Schulgemeinschaft"
            val formattedDate = formatIsoDate(post.date)
            val timestamp = parseIsoToTimestamp(post.date)

            NewsItem(
                id = "wp_${post.id}",
                title = cleanTitle,
                description = description,
                imageUrl = primaryImageUrl,
                imageUrls = allImages,
                type = NewsType.NEWS,
                dateText = formattedDate,
                timestamp = timestamp,
                author = authorName,
                isFeatured = post.sticky || index < 3,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }

    private fun extractImageUrls(html: String): List<String> {
        if (html.isBlank()) return emptyList()
        val regex = Regex("""<img[^>]+src=["'](https?://[^"'\s]+)["']""", RegexOption.IGNORE_CASE)
        return regex.findAll(html).map { it.groupValues[1] }.toList()
    }

    private fun formatIsoDate(iso: String): String {
        val datePart = iso.substringBefore("T")
        val parts = datePart.split("-")
        return if (parts.size == 3) {
            "${parts[2]}.${parts[1]}.${parts[0]}"
        } else {
            iso
        }
    }

    private fun parseIsoToTimestamp(iso: String): Long {
        return try {
            val datePart = iso.substringBefore("T")
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val year = parts[0].toLongOrNull() ?: 2026L
                val month = parts[1].toLongOrNull() ?: 1L
                val day = parts[2].toLongOrNull() ?: 1L
                val daysSince1970 = (year - 1970) * 365 + ((year - 1969) / 4) + (month - 1) * 30 + day
                daysSince1970 * 86400_000L
            } else 0L
        } catch (_: Throwable) {
            0L
        }
    }

    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        val withLineBreaks = html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)<li[^>]*>"), "• ")
            .replace(Regex("(?i)</li>"), "\n")

        val stripped = withLineBreaks.replace(Regex("<[^>]*>"), "")
        return decodeEntities(stripped).trim()
    }

    private fun decodeEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&#8216;", "‘")
            .replace("&#8217;", "’")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#8230;", "…")
            .replace("[&hellip;]", "…")
            .replace("&hellip;", "…")
            .replace(Regex("&#(\\d+);")) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else matchResult.value
            }
    }
}

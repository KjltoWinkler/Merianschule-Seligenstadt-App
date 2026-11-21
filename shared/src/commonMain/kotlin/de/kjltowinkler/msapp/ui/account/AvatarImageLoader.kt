package de.kjltowinkler.msapp.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Zentraler ImageLoader & In-Memory-Cache für Profilbilder über alle Plattformen hinweg.
 */
object AvatarImageLoader {
    private val memoryCache = mutableMapOf<String, ImageBitmap>()

    fun getCached(url: String): ImageBitmap? {
        return memoryCache[url]
    }

    fun setCache(url: String, bitmap: ImageBitmap) {
        memoryCache[url] = bitmap
    }

    fun clearCache(url: String? = null) {
        if (url != null) {
            memoryCache.remove(url)
        } else {
            memoryCache.clear()
        }
    }

    suspend fun loadAvatar(url: String): ImageBitmap? {
        val cached = memoryCache[url]
        if (cached != null) return cached

        return try {
            val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                val base = BackendApiClient.serverUrl.trimEnd('/')
                val path = if (url.startsWith("/")) url else "/$url"
                "$base$path"
            }

            val response = BackendApiClient.httpClient.get(fullUrl) {
                BackendApiClient.authToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val bytes = response.bodyAsBytes()
                val bitmap = bytes.decodeToImageBitmap()
                memoryCache[url] = bitmap
                bitmap
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Reaktiv geladenes Avatar-Bild für Compose-UIs.
 */
@Composable
fun rememberAvatarBitmap(avatarUrl: String?): ImageBitmap? {
    if (avatarUrl.isNullOrBlank()) return null
    var bitmap by remember(avatarUrl) { mutableStateOf(AvatarImageLoader.getCached(avatarUrl)) }
    LaunchedEffect(avatarUrl) {
        if (bitmap == null) {
            bitmap = AvatarImageLoader.loadAvatar(avatarUrl)
        }
    }
    return bitmap
}

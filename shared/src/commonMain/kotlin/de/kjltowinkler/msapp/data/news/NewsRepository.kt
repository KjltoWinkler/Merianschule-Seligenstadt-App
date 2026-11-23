package de.kjltowinkler.msapp.data.news

import de.kjltowinkler.msapp.data.backend.BackendApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object NewsRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val defaultNews = listOf(
        NewsItem(
            id = "news_sample_1",
            title = "Kurzer Titel",
            description = "Kurze Beschreibung",
            imageUrl = "res:news_stairs",
            type = NewsType.NEWS,
            dateText = "Stand 21.11.2026",
            timestamp = 1795305600000L,
            author = "Schulleitung",
            isFeatured = true
        ),
        NewsItem(
            id = "news_sample_2",
            title = "Kurzer Titel",
            description = "Kurze Beschreibung",
            imageUrl = "res:news_cups",
            type = NewsType.EVENT,
            dateText = "Stand 21.11.2026",
            timestamp = 1795219200000L,
            author = "Schulleitung",
            isFeatured = true
        ),
        NewsItem(
            id = "news_sample_3",
            title = "Kurzer Titel",
            description = "Kurze Beschreibung",
            imageUrl = "res:news_certificates",
            type = NewsType.NEWS,
            dateText = "Stand 21.11.2026",
            timestamp = 1795132800000L,
            author = "Schulleitung",
            isFeatured = true
        ),
        NewsItem(
            id = "news_sample_4",
            title = "Kurzer Titel",
            description = "Beschreibung",
            imageUrl = null,
            type = NewsType.EVENT,
            dateText = "Stand 21.11.2026",
            timestamp = 1795046400000L,
            author = "Schulgemeinschaft",
            isFeatured = false
        ),
        NewsItem(
            id = "news_sample_5",
            title = "Kurzer Titel",
            description = "Beschreibung",
            imageUrl = null,
            type = NewsType.ANNOUNCEMENT,
            dateText = "Stand 21.11.2026",
            timestamp = 1794960000000L,
            author = "Schulleitung",
            isFeatured = false
        ),
        NewsItem(
            id = "news_sample_6",
            title = "Kurzer Titel",
            description = "Beschreibung",
            imageUrl = null,
            type = NewsType.NEWS,
            dateText = "Stand 21.11.2026",
            timestamp = 1794873600000L,
            author = "Schulgemeinschaft",
            isFeatured = false
        ),
        NewsItem(
            id = "news_sample_7",
            title = "Kurzer Titel",
            description = "Beschreibung",
            imageUrl = null,
            type = NewsType.NEWS,
            dateText = "Stand 21.11.2026",
            timestamp = 1794787200000L,
            author = "Schulgemeinschaft",
            isFeatured = false
        ),
        NewsItem(
            id = "news_sample_8",
            title = "Kurzer Titel",
            description = "Beschreibung",
            imageUrl = null,
            type = NewsType.NEWS,
            dateText = "Stand 21.11.2026",
            timestamp = 1794700800000L,
            author = "Schulgemeinschaft",
            isFeatured = false
        )
    )

    private val _newsFlow = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsFlow: StateFlow<List<NewsItem>> = _newsFlow.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews(forceRefresh: Boolean = false) {
        scope.launch {
            _isLoading.value = true
            try {
                // Beiträge direkt von der Website der Merianschule via WordPress REST-API abrufen
                val wpNews = try {
                    WordPressApiClient.fetchPosts(perPage = 20)
                } catch (_: Throwable) {
                    emptyList()
                }

                // Ergänzend eventuelle app-interne Beiträge / Ankündigungen vom Backend laden
                val serverNews = try {
                    BackendApiClient.getNews().filterNot { it.id.startsWith("news_sample_") }
                } catch (_: Throwable) {
                    emptyList()
                }

                val combined = (serverNews + wpNews).distinctBy { it.id }
                if (combined.isNotEmpty()) {
                    _newsFlow.value = combined
                } else if (_newsFlow.value.isEmpty()) {
                    _newsFlow.value = defaultNews
                }
            } catch (_: Throwable) {
                // Bei Offline-Modus oder Nichterreichbarkeit bleiben defaultNews / Cache aktiv
                if (_newsFlow.value.isEmpty()) {
                    _newsFlow.value = defaultNews
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun createNews(request: CreateNewsRequest): Result<NewsItem> {
        return try {
            val result = BackendApiClient.createNews(request)
            loadNews(forceRefresh = true)
            Result.success(result)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun deleteNews(id: String): Result<Boolean> {
        return try {
            val success = BackendApiClient.deleteNews(id)
            loadNews(forceRefresh = true)
            Result.success(success)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}

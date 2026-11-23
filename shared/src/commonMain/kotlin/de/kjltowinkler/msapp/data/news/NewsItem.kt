package de.kjltowinkler.msapp.data.news

import kotlinx.serialization.Serializable

@Serializable
enum class NewsType {
    NEWS,
    EVENT,
    ANNOUNCEMENT
}

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val type: NewsType = NewsType.NEWS,
    val dateText: String = "",
    val timestamp: Long = 0L,
    val author: String = "Schulleitung",
    val isFeatured: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val allImages: List<String>
        get() {
            val list = if (imageUrls.isNotEmpty()) imageUrls else listOfNotNull(imageUrl)
            return list.distinct()
        }
}

@Serializable
data class CreateNewsRequest(
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val type: NewsType = NewsType.NEWS,
    val dateText: String = "",
    val timestamp: Long = 0L,
    val author: String = "Schulleitung",
    val isFeatured: Boolean = true
)

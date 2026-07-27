package com.santiifm.milou.domain.model

enum class MetadataSource {
    SCREEN_SCRAPER,
    IGDB,
    LOCAL,
    USER
}

data class GameMetadata(
    val id: String,
    val title: String,
    val description: String?,
    val releaseDate: Long?,
    val rating: Float?,
    val coverUrl: String?,
    val localCoverPath: String? = null,
    val developer: String?,
    val publisher: String?,
    val genres: List<String> = emptyList(),
    val source: MetadataSource,
    val confidence: Float = 1.0f
)

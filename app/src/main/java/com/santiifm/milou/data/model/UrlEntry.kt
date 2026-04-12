package com.santiifm.milou.data.model

data class UrlEntry(
    val url: String,
    val contentType: ContentType = ContentType.GAME,
    val folders: List<String> = emptyList()
)

enum class ContentType {
    GAME,
    MISCELLANEOUS,
    RETROACHIEVEMENTS
}

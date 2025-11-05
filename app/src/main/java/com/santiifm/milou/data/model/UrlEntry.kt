package com.santiifm.milou.data.model

data class UrlEntry(
    val url: String,
    val contentType: ContentType = ContentType.GAME
)

enum class ContentType {
    GAME,
    MISCELLANEOUS,
    RETROACHIEVEMENTS
}

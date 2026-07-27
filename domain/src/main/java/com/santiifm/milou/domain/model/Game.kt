package com.santiifm.milou.domain.model

data class Game(
    val id: String,
    val title: String,
    val consoleId: String,
    val region: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isInstalled: Boolean = false,
    val artworkPath: String? = null,
    val fileSize: Long = 0
)

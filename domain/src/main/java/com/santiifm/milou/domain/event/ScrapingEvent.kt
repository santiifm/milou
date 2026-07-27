package com.santiifm.milou.domain.event

sealed interface ScrapingEvent : MilouEvent {
    val identifier: String // Can be consoleId, manufacturerId, etc.

    data class Started(
        override val identifier: String,
        val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ScrapingEvent

    data class Progress(
        override val identifier: String,
        val message: String,
        val current: Int = 0,
        val total: Int = 0,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ScrapingEvent

    data class Completed(
        override val identifier: String,
        val filesFound: Int,
        val tagsFound: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ScrapingEvent

    data class Error(
        override val identifier: String,
        val errorMessage: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ScrapingEvent
}

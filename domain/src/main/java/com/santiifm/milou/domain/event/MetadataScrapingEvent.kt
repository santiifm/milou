package com.santiifm.milou.domain.event

sealed interface MetadataScrapingEvent : MilouEvent {
    val operationId: String

    data class Started(
        override val operationId: String,
        val gameTitle: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MetadataScrapingEvent

    data class Progress(
        override val operationId: String,
        val current: Int,
        val total: Int,
        val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MetadataScrapingEvent

    data class Completed(
        override val operationId: String,
        val gameId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MetadataScrapingEvent

    data class Failed(
        override val operationId: String,
        val reason: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MetadataScrapingEvent
}

package com.santiifm.milou.domain.event

sealed interface ExtractionEvent : MilouEvent {
    val operationId: String

    data class Started(
        override val operationId: String,
        val sourceFile: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ExtractionEvent

    data class Progress(
        override val operationId: String,
        val progress: Float,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ExtractionEvent

    data class Completed(
        override val operationId: String,
        val extractedFiles: List<String>,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ExtractionEvent

    data class Failed(
        override val operationId: String,
        val errorMessage: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ExtractionEvent
}

package com.santiifm.milou.domain.event

import com.santiifm.milou.domain.model.DownloadStatus

sealed interface DownloadEvent : MilouEvent {
    val downloadId: String

    data class Started(
        override val downloadId: String,
        val name: String,
        val fileName: String,
        val fileSize: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DownloadEvent

    data class Progress(
        override val downloadId: String,
        val progress: Float,
        val speed: Float,
        val downloadedBytes: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DownloadEvent

    data class StatusChanged(
        override val downloadId: String,
        val status: DownloadStatus,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DownloadEvent

    data class Completed(
        override val downloadId: String,
        val filePath: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DownloadEvent
}

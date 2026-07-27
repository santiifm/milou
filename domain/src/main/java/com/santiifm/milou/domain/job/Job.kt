package com.santiifm.milou.domain.job

enum class JobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED,
    RECOVERABLE
}

enum class JobType {
    DOWNLOAD,
    EXTRACTION,
    SCRAPING,
    IMPORT
}

data class Job(
    val id: String,
    val type: JobType,
    val status: JobStatus,
    val progress: Float = 0f,
    val payload: String, // JSON or serialized data
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

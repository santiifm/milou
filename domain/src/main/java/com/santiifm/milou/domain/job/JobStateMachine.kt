package com.santiifm.milou.domain.job

object JobStateMachine {
    private val validTransitions = mapOf(
        JobStatus.QUEUED to setOf(JobStatus.RUNNING, JobStatus.CANCELLED),
        JobStatus.RUNNING to setOf(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.RECOVERABLE, JobStatus.CANCELLED),
        JobStatus.PAUSED to setOf(JobStatus.RUNNING, JobStatus.CANCELLED),
        JobStatus.RECOVERABLE to setOf(JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.CANCELLED),
        JobStatus.FAILED to setOf(JobStatus.QUEUED, JobStatus.CANCELLED),
        JobStatus.CANCELLED to emptySet(),
        JobStatus.COMPLETED to emptySet()
    )

    fun canTransition(from: JobStatus, to: JobStatus): Boolean {
        if (from == to) return true
        return validTransitions[from]?.contains(to) ?: false
    }
}

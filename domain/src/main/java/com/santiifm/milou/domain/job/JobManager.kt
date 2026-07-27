package com.santiifm.milou.domain.job

import kotlinx.coroutines.flow.Flow

interface JobManager {
    val allJobs: Flow<List<Job>>
    suspend fun submitJob(job: Job)
    suspend fun cancelJob(jobId: String)
    suspend fun pauseJob(jobId: String)
    suspend fun resumeJob(jobId: String)
    suspend fun updateJobStatus(jobId: String, status: JobStatus, progress: Float = 0f, error: String? = null)
    suspend fun getJob(jobId: String): Job?
}

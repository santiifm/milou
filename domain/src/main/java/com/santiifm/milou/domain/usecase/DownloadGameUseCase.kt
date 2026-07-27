package com.santiifm.milou.domain.usecase

import com.santiifm.milou.domain.job.Job
import com.santiifm.milou.domain.job.JobManager
import com.santiifm.milou.domain.job.JobStatus

class DownloadGameUseCase(
    private val jobManager: JobManager
) {
    suspend operator fun invoke(jobId: String): Result<Unit> {
        val job = jobManager.getJob(jobId) ?: return Result.failure(Exception("Job not found"))
        
        // This is a placeholder for the actual execution logic which will be injected
        // via a low-level interface or by refactoring DownloadService into a Repository.
        // For now, it represents the coordination between JobManager and the execution.
        
        return try {
            // Actual download execution will happen here
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

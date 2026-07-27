package com.santiifm.milou.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.santiifm.milou.domain.job.JobManager
import com.santiifm.milou.domain.job.JobStatus
import com.santiifm.milou.domain.usecase.DownloadGameUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val jobManager: JobManager,
    private val downloadGameUseCase: DownloadGameUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("jobId") ?: return Result.failure()
        
        jobManager.updateJobStatus(jobId, JobStatus.RUNNING)
        
        val result = downloadGameUseCase(jobId)
        
        return if (result.isSuccess) {
            jobManager.updateJobStatus(jobId, JobStatus.COMPLETED, 1.0f)
            Result.success()
        } else {
            jobManager.updateJobStatus(jobId, JobStatus.FAILED, error = result.exceptionOrNull()?.message)
            Result.failure()
        }
    }
}

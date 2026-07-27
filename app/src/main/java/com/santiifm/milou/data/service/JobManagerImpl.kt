package com.santiifm.milou.data.service

import android.content.Context
import androidx.work.*
import com.santiifm.milou.data.local.dao.JobDao
import com.santiifm.milou.data.mapper.toDomain
import com.santiifm.milou.data.mapper.toEntity
import com.santiifm.milou.domain.event.MilouEvent
import com.santiifm.milou.domain.eventbus.EventBus
import com.santiifm.milou.domain.job.Job
import com.santiifm.milou.domain.job.JobManager
import com.santiifm.milou.domain.job.JobStatus
import com.santiifm.milou.domain.job.JobStateMachine
import com.santiifm.milou.data.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobDao: JobDao,
    private val eventBus: EventBus
) : JobManager {

    override val allJobs: Flow<List<Job>> = jobDao.getAllJobs().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun submitJob(job: Job) {
        jobDao.insertOrUpdateJob(job.toEntity())
        
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("jobId" to job.id))
            .addTag(job.id)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            job.id,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    override suspend fun cancelJob(jobId: String) {
        val job = getJob(jobId) ?: return
        if (JobStateMachine.canTransition(job.status, JobStatus.CANCELLED)) {
            updateJobStatus(jobId, JobStatus.CANCELLED)
            WorkManager.getInstance(context).cancelUniqueWork(jobId)
        }
    }

    override suspend fun pauseJob(jobId: String) {
        val job = getJob(jobId) ?: return
        if (JobStateMachine.canTransition(job.status, JobStatus.PAUSED)) {
            updateJobStatus(jobId, JobStatus.PAUSED)
            WorkManager.getInstance(context).cancelUniqueWork(jobId)
        }
    }

    override suspend fun resumeJob(jobId: String) {
        val job = getJob(jobId) ?: return
        if (JobStateMachine.canTransition(job.status, JobStatus.RUNNING)) {
            submitJob(job.copy(status = JobStatus.QUEUED))
        }
    }

    override suspend fun updateJobStatus(jobId: String, status: JobStatus, progress: Float, error: String?) {
        val currentJob = getJob(jobId) ?: return
        if (JobStateMachine.canTransition(currentJob.status, status)) {
            jobDao.updateStatus(jobId, status.name, progress, System.currentTimeMillis())
            // Potentially publish to EventBus here for generic Job status updates
        }
    }

    override suspend fun getJob(jobId: String): Job? {
        return jobDao.getJobById(jobId)?.toDomain()
    }
    
    suspend fun handleStartupRecovery() {
        val activeJobs = jobDao.getActiveJobs()
        activeJobs.forEach { entity ->
            if (entity.status == "RUNNING") {
                jobDao.updateStatus(entity.id, "RECOVERABLE", entity.progress, System.currentTimeMillis())
            }
        }
    }
}

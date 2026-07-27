package com.santiifm.milou.data.local.dao

import androidx.room.*
import com.santiifm.milou.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateJob(job: JobEntity)

    @Query("DELETE FROM jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("UPDATE jobs SET status = :status, progress = :progress, updatedAt = :updatedAt WHERE id = :jobId")
    suspend fun updateStatus(jobId: String, status: String, progress: Float, updatedAt: Long)

    @Query("SELECT * FROM jobs WHERE status IN ('RUNNING', 'RECOVERABLE', 'QUEUED')")
    suspend fun getActiveJobs(): List<JobEntity>
}

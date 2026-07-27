package com.santiifm.milou.data.mapper

import com.santiifm.milou.data.local.entity.JobEntity
import com.santiifm.milou.domain.job.Job
import com.santiifm.milou.domain.job.JobStatus
import com.santiifm.milou.domain.job.JobType

fun JobEntity.toDomain(): Job {
    return Job(
        id = id,
        type = JobType.valueOf(type),
        status = JobStatus.valueOf(status),
        progress = progress,
        payload = payload,
        error = error,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Job.toEntity(): JobEntity {
    return JobEntity(
        id = id,
        type = type.name,
        status = status.name,
        progress = progress,
        payload = payload,
        error = error,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

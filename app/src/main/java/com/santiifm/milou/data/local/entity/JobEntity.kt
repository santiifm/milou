package com.santiifm.milou.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val type: String,
    val status: String,
    val progress: Float,
    val payload: String,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long
)

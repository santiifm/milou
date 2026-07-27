package com.santiifm.milou.data.repository

import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.DownloadItemModel
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val downloads: StateFlow<List<DownloadItemModel>>
    suspend fun getDownloads(): List<DownloadItemModel>
    suspend fun startDownload(file: DownloadableFileEntity)
    suspend fun cancelDownload(id: String)
    suspend fun retryDownload(id: String)
    suspend fun deleteDownload(id: String, deleteFile: Boolean = false)
}

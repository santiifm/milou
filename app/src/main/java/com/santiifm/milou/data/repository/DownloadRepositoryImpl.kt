package com.santiifm.milou.data.repository

import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.DownloadItemModel
import com.santiifm.milou.data.service.DownloadService
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadService: DownloadService
) : DownloadRepository {

    override val downloads: StateFlow<List<DownloadItemModel>> = downloadService.downloads

    override suspend fun getDownloads(): List<DownloadItemModel> {
        return downloadService.getDownloads()
    }

    override suspend fun startDownload(file: DownloadableFileEntity) {
        downloadService.startDownload(file)
    }

    override suspend fun cancelDownload(id: String) {
        downloadService.cancelDownload(id)
    }

    override suspend fun retryDownload(id: String) {
        downloadService.retryDownload(id)
    }

    override suspend fun deleteDownload(id: String, deleteFile: Boolean) {
        downloadService.deleteDownload(id, deleteFile)
    }
}

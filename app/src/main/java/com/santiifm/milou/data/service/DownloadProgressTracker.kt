package com.santiifm.milou.data.service

import com.santiifm.milou.data.model.DownloadItemModel
import com.santiifm.milou.data.model.DownloadStatus
import com.santiifm.milou.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProgressTracker @Inject constructor() {

    private val _downloads = MutableStateFlow<List<DownloadItemModel>>(emptyList())
    val downloads: StateFlow<List<DownloadItemModel>> = _downloads

    fun updateDownloadStatus(fileName: String, status: DownloadStatus) {
        _downloads.update { list ->
            list.map { if (it.fileName == fileName) it.copy(status = status) else it }
        }
    }

    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()

    fun updateDownloadProgress(fileName: String, progress: Float, speed: Float, downloadedBytes: Long) {
        val now = System.currentTimeMillis()
        val lastUpdate = lastUpdateTimes[fileName] ?: 0L

        if (shouldUpdateProgress(progress, lastUpdate, now)) {
            lastUpdateTimes[fileName] = now
            _downloads.update { list ->
                list.map { item ->
                    if (item.fileName == fileName) {
                        item.copy(
                            progress = progress,
                            downloadSpeed = speed,
                            downloadedBytes = downloadedBytes
                        )
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun addDownload(downloadItem: DownloadItemModel) {
        _downloads.update { it + downloadItem }
    }

    fun removeDownload(fileName: String) {
        _downloads.update { list -> list.filter { it.fileName != fileName } }
        lastUpdateTimes.remove(fileName)
    }

    fun getDownloads(): List<DownloadItemModel> {
        return _downloads.value
    }

    fun resetDownloadForRetry(fileName: String) {
        _downloads.update { list ->
            list.map { item ->
                if (item.fileName == fileName) {
                    item.copy(
                        status = DownloadStatus.DOWNLOADING,
                        progress = 0f,
                        downloadSpeed = 0f,
                        downloadedBytes = 0L
                    )
                } else {
                    item
                }
            }
        }
    }

    fun canRetryDownload(fileName: String): Boolean {
        return _downloads.value.any {
            it.fileName == fileName &&
            (it.status == DownloadStatus.FAILED || it.status == DownloadStatus.STOPPED)
        }
    }

    fun hasActiveDownloads(): Boolean {
        return _downloads.value.any {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.COPYING ||
            it.status == DownloadStatus.UNZIPPING
        }
    }

    fun shouldUpdateProgress(progress: Float, lastUpdateTime: Long, currentTime: Long): Boolean {
        return progress >= Constants.PROGRESS_COMPLETE ||
               (currentTime - lastUpdateTime) > Constants.PROGRESS_UPDATE_INTERVAL_MS
    }
}

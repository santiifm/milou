package com.santiifm.milou.data.service

import com.santiifm.milou.data.model.DownloadItemModel
import com.santiifm.milou.domain.model.DownloadStatus
import com.santiifm.milou.domain.event.DownloadEvent
import com.santiifm.milou.domain.event.ExtractionEvent
import com.santiifm.milou.domain.eventbus.EventBus
import com.santiifm.milou.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProgressTracker @Inject constructor(
    private val eventBus: EventBus
) {

    private val _downloads = MutableStateFlow<List<DownloadItemModel>>(emptyList())
    val downloads: StateFlow<List<DownloadItemModel>> = _downloads

    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        trackerScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DownloadEvent -> handleDownloadEvent(event)
                    is ExtractionEvent -> handleExtractionEvent(event)
                }
            }
        }
    }

    private fun handleDownloadEvent(event: DownloadEvent) {
        when (event) {
            is DownloadEvent.Started -> {
                addDownload(
                    DownloadItemModel(
                        id = event.downloadId,
                        name = event.name,
                        fileName = event.fileName,
                        fileSize = event.fileSize,
                        downloadSpeed = 0f,
                        progress = 0f,
                        status = DownloadStatus.DOWNLOADING
                    )
                )
            }
            is DownloadEvent.Progress -> {
                updateDownloadProgress(event.downloadId, event.progress, event.speed, event.downloadedBytes)
            }
            is DownloadEvent.StatusChanged -> {
                updateDownloadStatus(event.downloadId, event.status)
            }
            is DownloadEvent.Completed -> {
                updateDownloadStatus(event.downloadId, DownloadStatus.COMPLETED)
            }
        }
    }

    private fun handleExtractionEvent(event: ExtractionEvent) {
        when (event) {
            is ExtractionEvent.Started -> {
                updateDownloadStatus(event.operationId, DownloadStatus.UNZIPPING)
            }
            is ExtractionEvent.Progress -> {
                updateDownloadProgress(event.operationId, event.progress, 0f, 0L)
            }
            is ExtractionEvent.Completed -> {
                updateDownloadStatus(event.operationId, DownloadStatus.COMPLETED)
            }
            is ExtractionEvent.Failed -> {
                updateDownloadStatus(event.operationId, DownloadStatus.FAILED)
            }
        }
    }

    private fun updateDownloadStatus(id: String, status: DownloadStatus) {
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = status) else it }
        }
    }

    private fun updateDownloadProgress(id: String, progress: Float, speed: Float, downloadedBytes: Long) {
        val now = System.currentTimeMillis()
        val lastUpdate = lastUpdateTimes[id] ?: 0L

        if (shouldUpdateProgress(progress, lastUpdate, now)) {
            lastUpdateTimes[id] = now
            _downloads.update { list ->
                list.map { item ->
                    if (item.id == id) {
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

    private fun addDownload(downloadItem: DownloadItemModel) {
        _downloads.update { it + downloadItem }
    }

    fun removeDownload(id: String) {
        _downloads.update { list -> list.filter { it.id != id } }
        lastUpdateTimes.remove(id)
    }

    fun getDownloads(): List<DownloadItemModel> {
        return _downloads.value
    }

    fun resetDownloadForRetry(id: String) {
        _downloads.update { list ->
            list.map { item ->
                if (item.id == id) {
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

    fun canRetryDownload(id: String): Boolean {
        return _downloads.value.any {
            it.id == id &&
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

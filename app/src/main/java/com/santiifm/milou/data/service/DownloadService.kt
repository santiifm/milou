package com.santiifm.milou.data.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.DownloadItemModel
import com.santiifm.milou.data.model.DownloadStatus
import com.santiifm.milou.data.repository.SettingsRepository
import com.santiifm.milou.util.ArchiveUtils
import com.santiifm.milou.util.ArchiveExtractionUtils
import com.santiifm.milou.util.Constants
import com.santiifm.milou.util.StorageHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val archiveExtractorService: ArchiveExtractorService,
    private val downloadSpeedController: DownloadSpeedController,
    private val downloadHttpClient: DownloadHttpClient,
    private val downloadProgressTracker: DownloadProgressTracker,
    private val downloadFileManager: DownloadFileManager
) {
    val downloads: StateFlow<List<DownloadItemModel>> = downloadProgressTracker.downloads

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private var downloadSemaphore = Semaphore(3) // Default concurrent downloads
    private var foregroundServiceStarted = false
    private val originalDownloadEntities = ConcurrentHashMap<String, DownloadableFileEntity>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.concurrentDownloads.collect { maxConcurrent: Int ->
                downloadSemaphore = Semaphore(maxConcurrent)
            }
        }
    }

    fun startDownload(file: DownloadableFileEntity) {
        val downloadItem = downloadFileManager.createDownloadItem(file)
        downloadProgressTracker.addDownload(downloadItem)
        originalDownloadEntities[file.fileName] = file

        startForegroundService()

        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadSemaphore.withPermit {
                    delay(1000L) // 1 second delay
                    performDownload(file)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.STOPPED)
                throw e
            } catch (e: Exception) {
                downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
                throw e
            } finally {
                downloadJobs.remove(file.fileName)
            }
        }
        downloadJobs[file.fileName] = job
    }

    private suspend fun performDownload(file: DownloadableFileEntity) {
        repeat(3) { attempt -> // Max 3 retries
            try {
                if (attempt > 0) {
                    delay(2000L * attempt) // Exponential backoff
                }
                
                performDownloadAttempt(file)
                return // Success, exit retry loop
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w("DownloadService", "Download cancelled for ${file.fileName}")
                downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.STOPPED)
                throw e
            } catch (e: Exception) {
                Log.w("DownloadService", "Download attempt ${attempt + 1} failed for ${file.fileName}: ${e.message}")
                
                if (attempt == 2) { // Last attempt failed
                    downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
                    throw e
                }
            }
        }
    }

    private suspend fun performDownloadAttempt(file: DownloadableFileEntity) {
        val downloadDirectoryUri = downloadFileManager.getDownloadDirectoryUri(file)
        if (downloadDirectoryUri.toString().isEmpty()) {
            throw Exception("Download directory not configured. Please set a download folder in settings.")
        }
        
        if (!StorageHelper.isValidUri(context, downloadDirectoryUri.toString())) {
            throw Exception("Download directory is not accessible. Please check your folder permissions in settings.")
        }
        
        var speedLimit = settingsRepository.limitSpeed.first()
        
        val speedLimitJob = downloadSpeedController.createSpeedLimiter { newLimit ->
            speedLimit = newLimit
        }
        
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var documentFile: DocumentFile? = null
        
        try {
            val connection = downloadHttpClient.createConnection(file.downloadUrl)
            val contentLength = connection.contentLengthLong
            inputStream = connection.inputStream

            val subPath = downloadFileManager.getSubPath(file)
            
            documentFile = downloadFileManager.createDocumentFile(file, downloadDirectoryUri.toString(), subPath)
                ?: throw Exception("Failed to create file in storage. Please check your folder permissions.")
            
            outputStream = downloadFileManager.getOutputStream(documentFile)
                ?: throw Exception("Failed to get output stream")

            performDownloadLoop(inputStream, outputStream, file, speedLimit, contentLength)
            
            handlePostDownloadProcessing(file, documentFile, subPath)
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            documentFile?.let { downloadFileManager.deleteFile(it) }
            downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.STOPPED)
            throw e
        } catch (e: Exception) {
            documentFile?.let { downloadFileManager.deleteFile(it) }
            downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
            throw e
        } finally {
            speedLimitJob.cancel()
            inputStream?.close()
            outputStream?.close()
        }
    }

    private suspend fun performDownloadLoop(
        inputStream: InputStream,
        outputStream: OutputStream,
        file: DownloadableFileEntity,
        initialSpeedLimit: Float,
        contentLength: Long
    ) {
        val speedLimit = initialSpeedLimit
        val buffer = ByteArray(Constants.BUFFER_SIZE)
        var downloadedBytes = 0L
        var bytesRead: Int
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        var lastDownloadedBytes = 0L
        var lastSpeedCheckTime = startTime
        var bytesSinceLastCheck = 0L
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (downloadJobs[file.fileName]?.isCancelled == true) {
                downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.STOPPED)
                return
            }
            
            outputStream.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead
            bytesSinceLastCheck += bytesRead
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastCheck = (currentTime - lastSpeedCheckTime) / 1000f
            
            if (timeSinceLastCheck >= Constants.SPEED_CHECK_INTERVAL_MS / 1000f) {
                val currentSpeed = downloadSpeedController.calculateSpeed(bytesSinceLastCheck, timeSinceLastCheck)
                downloadSpeedController.applySpeedThrottling(currentSpeed, speedLimit, bytesSinceLastCheck, timeSinceLastCheck)
                
                lastSpeedCheckTime = currentTime
                bytesSinceLastCheck = 0L
            }
            
            updateDownloadProgress(file, downloadedBytes, contentLength, startTime, lastUpdateTime, lastDownloadedBytes)
            
            if (downloadProgressTracker.shouldUpdateProgress(
                downloadedBytes.toFloat() / contentLength.toFloat().coerceAtLeast(1f),
                lastUpdateTime,
                System.currentTimeMillis()
            )) {
                lastUpdateTime = System.currentTimeMillis()
                lastDownloadedBytes = downloadedBytes
            }
        }
    }

    private fun updateDownloadProgress(
        file: DownloadableFileEntity,
        downloadedBytes: Long,
        contentLength: Long,
        startTime: Long,
        lastUpdateTime: Long,
        lastDownloadedBytes: Long
    ) {
        val progress = if (contentLength > 0) {
            ArchiveExtractionUtils.calculateProgress(downloadedBytes.toInt(), contentLength.toInt())
        } else {
            0f
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - startTime) / 1000f
        val recentElapsedTime = (currentTime - lastUpdateTime) / 1000f
        val recentDownloadedBytes = downloadedBytes - lastDownloadedBytes
        
        val downloadSpeed = downloadSpeedController.calculateSpeed(recentDownloadedBytes, recentElapsedTime)
            .takeIf { it > 0 } ?: downloadSpeedController.calculateSpeed(downloadedBytes, elapsedTime)
        
        downloadProgressTracker.updateDownloadProgress(file.fileName, progress, downloadSpeed, downloadedBytes)
    }

    private suspend fun handlePostDownloadProcessing(
        file: DownloadableFileEntity,
        documentFile: DocumentFile,
        subPath: String
    ) {
        val isExtractable = ArchiveUtils.isExtractable(file.fileExtension)
        
        if (isExtractable) {
            val autoUnzip = settingsRepository.autoUnzip.first()
            if (autoUnzip) {
                downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.UNZIPPING)
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val extractedFiles = archiveExtractorService.extractArchive(
                            context,
                            documentFile.uri,
                            downloadFileManager.getDownloadDirectoryUri(file),
                            subPath
                        )
                        
                        val extractionSuccess = extractedFiles.isNotEmpty()
                        if (extractionSuccess) {
                            downloadFileManager.deleteFile(documentFile)
                            // Store extracted files for later deletion
                            originalDownloadEntities[file.fileName] = file.copy(extractedFiles = extractedFiles)
                            Log.d("DownloadService", "Successfully deleted archive after extraction: ${file.fileName}")
                        }
                        
                        downloadProgressTracker.updateDownloadStatus(
                            file.fileName,
                            if (extractionSuccess) DownloadStatus.COMPLETED else DownloadStatus.FAILED
                        )
                        
                        checkServiceLifecycle()
                    } catch (e: Exception) {
                        Log.e("DownloadService", "Error during extraction: ${e.message}", e)
                        downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
                    }
                }
            } else {
                downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.COMPLETED)
                checkServiceLifecycle()
            }
        } else {
            downloadProgressTracker.updateDownloadStatus(file.fileName, DownloadStatus.COMPLETED)
            checkServiceLifecycle()
        }
    }

    fun cancelDownload(fileName: String) {
        downloadJobs[fileName]?.cancel()
        downloadJobs.remove(fileName)
        downloadProgressTracker.updateDownloadStatus(fileName, DownloadStatus.STOPPED)
    }

    fun retryDownload(fileName: String) {
        val originalEntity = originalDownloadEntities[fileName]
        if (originalEntity != null && downloadProgressTracker.canRetryDownload(fileName)) {
            val job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    downloadSemaphore.withPermit {
                        delay(2000L) // 2 second delay for retries
                        performDownload(originalEntity)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    downloadProgressTracker.updateDownloadStatus(fileName, DownloadStatus.STOPPED)
                    throw e
                } catch (e: Exception) {
                    downloadProgressTracker.updateDownloadStatus(fileName, DownloadStatus.FAILED)
                    throw e
                } finally {
                    downloadJobs.remove(fileName)
                }
            }
            downloadJobs[fileName] = job
        }
    }

    fun deleteDownload(fileName: String, deleteFile: Boolean = false) {
        downloadJobs[fileName]?.cancel()
        downloadJobs.remove(fileName)
        
        val originalEntity = originalDownloadEntities[fileName]
        originalDownloadEntities.remove(fileName)
        
        if (deleteFile && originalEntity != null) {
            CoroutineScope(Dispatchers.IO).launch {
                downloadFileManager.deleteFileByName(originalEntity, deleteFile = true)
            }
        }
        
        downloadProgressTracker.removeDownload(fileName)
    }

    private fun startForegroundService() {
        if (!foregroundServiceStarted) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = DownloadForegroundService.ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
            foregroundServiceStarted = true
        }
    }

    private fun stopForegroundService() {
        if (foregroundServiceStarted && !downloadProgressTracker.hasActiveDownloads()) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = DownloadForegroundService.ACTION_STOP_SERVICE
            }
            context.startService(intent)
            foregroundServiceStarted = false
        }
    }
    
    private fun checkServiceLifecycle() {
        if (foregroundServiceStarted && !downloadProgressTracker.hasActiveDownloads()) {
            stopForegroundService()
        }
    }

    fun getDownloads(): List<DownloadItemModel> = downloadProgressTracker.getDownloads()
}

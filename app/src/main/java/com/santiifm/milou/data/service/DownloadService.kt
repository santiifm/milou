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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadService"

@Singleton
class DownloadService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val archiveExtractorService: ArchiveExtractorService,
    private val downloadSpeedController: DownloadSpeedController,
    private val downloadHttpClient: DownloadHttpClient,
    private val downloadProgressTracker: DownloadProgressTracker,
    private val downloadFileManager: DownloadFileManager,
    private val torrentDownloadService: TorrentDownloadService,
    private val torrentHandleRegistry: TorrentHandleRegistry
) {
    val downloads: StateFlow<List<DownloadItemModel>> = downloadProgressTracker.downloads

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private var downloadSemaphore = Semaphore(3)
    private var foregroundServiceStarted = false
    private val downloadEntities = ConcurrentHashMap<String, DownloadableFileEntity>()
    private val extractedFilesMap = ConcurrentHashMap<String, List<String>>()

    // Single supervised scope for all internal coroutines — tied to this singleton's lifetime
    // so jobs are not orphaned if the service is destroyed.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        serviceScope.launch {
            settingsRepository.concurrentDownloads.collect { max ->
                downloadSemaphore = Semaphore(max)
            }
        }
    }

    fun startDownload(file: DownloadableFileEntity) {
        downloadProgressTracker.addDownload(downloadFileManager.createDownloadItem(file))
        downloadEntities[file.fileName] = file
        startForegroundService()

        val job = serviceScope.launch {
            try {
                downloadSemaphore.withPermit {
                    // Brief delay to allow the foreground service and initial UI state to settle
                    // before network/torrent activity begins.
                    delay(1000L)
                    if (file.isTorrent) performTorrentDownload(file)
                    else performHttpDownload(file)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                updateStatus(file.fileName, DownloadStatus.STOPPED)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${file.fileName}: ${e.message}")
                updateStatus(file.fileName, DownloadStatus.FAILED)
            } finally {
                downloadJobs.remove(file.fileName)
            }
        }
        downloadJobs[file.fileName] = job
    }

    fun cancelDownload(fileName: String) {
        downloadJobs.remove(fileName)?.cancel()
        val entity = downloadEntities[fileName] ?: return
        serviceScope.launch {
            if (entity.isTorrent) torrentDownloadService.cancelDownload(entity)
            else updateStatus(fileName, DownloadStatus.STOPPED)
        }
    }

    fun retryDownload(fileName: String) {
        if (!downloadProgressTracker.canRetryDownload(fileName)) return
        val entity = downloadEntities[fileName] ?: return
        startDownload(entity)
    }

    fun deleteDownload(fileName: String, deleteFile: Boolean = false) {
        downloadJobs.remove(fileName)?.cancel()
        val entity = downloadEntities.remove(fileName)
        val extracted = extractedFilesMap.remove(fileName) ?: emptyList()
        serviceScope.launch {
            if (entity?.isTorrent == true) torrentDownloadService.cancelDownload(entity)
            if (deleteFile && entity != null) downloadFileManager.deleteFileByName(entity, true, extracted)
        }
        downloadProgressTracker.removeDownload(fileName)
    }

    fun cancelAllDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }

    fun getDownloads(): List<DownloadItemModel> = downloadProgressTracker.getDownloads()

    private suspend fun performTorrentDownload(file: DownloadableFileEntity) {
        Log.d(TAG, "Starting torrent download for ${file.fileName}")
        torrentDownloadService.startDownload(file)

        // Collect just this file's status as a distinct flow instead of polling the full
        // downloads list on every tick — O(1) vs O(n) and no busy-wait sleep.
        val finalStatus = downloadProgressTracker.downloads
            .map { list -> list.find { it.fileName == file.fileName }?.status }
            .distinctUntilChanged()
            .first { it == DownloadStatus.COMPLETED || it == DownloadStatus.FAILED || it == DownloadStatus.STOPPED }

        when (finalStatus) {
            DownloadStatus.FAILED  -> { Log.e(TAG, "Torrent FAILED: ${file.fileName}"); return }
            DownloadStatus.STOPPED -> { Log.i(TAG, "Torrent STOPPED: ${file.fileName}"); return }
            else -> moveTorrentFile(file)
        }
    }

    private suspend fun moveTorrentFile(file: DownloadableFileEntity) {
        try {
            val downloadDirUri = downloadFileManager.getDownloadDirectoryUri(file)
            if (downloadDirUri == android.net.Uri.EMPTY)
                throw Exception("Download directory not configured or no longer accessible.")

            // Use the info cached at download-start time so this works even if the handle was
            // invalidated (e.g. session stopped during app shutdown before the copy finishes).
            val fileInfo = torrentDownloadService.getFileInfo(file.fileName)
                ?: throw Exception("Could not get torrent file info for ${file.fileName}")
            val relativePath = fileInfo.relativePath
            val expectedSize = fileInfo.expectedSize
            val fileExtension = relativePath.substringAfterLast(".", "")

            val internalFile = File(context.cacheDir, "torrent_data/$relativePath")
            Log.d(TAG, "Internal torrent file: ${internalFile.absolutePath}, exists: ${internalFile.exists()}")

            if (!internalFile.exists())
                throw Exception("Internal torrent file not found at ${internalFile.absolutePath}")

            // libtorrent marks a file complete (via fileProgress) after hash-verification,
            // but its disk thread flushes writes asynchronously. Poll until the OS-visible
            // file size matches the torrent metadata size before we copy.
            // Also bail early if the session has been stopped (e.g. app shutdown) — the file
            // will never grow any further and the job should fail fast rather than wait 15s.
            var waitedMs = 0
            while (internalFile.length() < expectedSize && waitedMs < 15_000 && torrentHandleRegistry.isRunning) {
                Log.d(TAG, "Waiting for disk flush for ${file.fileName}: ${internalFile.length()}/$expectedSize bytes")
                delay(500)
                waitedMs += 500
            }
            if (internalFile.length() < expectedSize) {
                Log.w(TAG, "Disk flush incomplete for ${file.fileName}: ${internalFile.length()}/$expectedSize bytes written")
            }

            val subPath = downloadFileManager.getSubPath(file)

            if (ArchiveUtils.isExtractable(fileExtension) && settingsRepository.autoUnzip.first()) {
                // Extract directly from cache — skips writing the compressed archive to SAF entirely.
                // Flow: cacheDir/torrent_data/ → extraction_temp/ → SAF destination
                Log.d(TAG, "Extracting torrent archive directly from cache: ${internalFile.name}")
                updateStatus(file.fileName, DownloadStatus.UNZIPPING)
                val extracted = archiveExtractorService.extractArchiveFile(
                    context, internalFile, downloadDirUri, subPath
                )
                if (extracted.isNotEmpty()) {
                    extractedFilesMap[file.fileName] = extracted
                } else {
                    Log.w(TAG, "Extraction produced no files for ${file.fileName}")
                }
            } else {
                // Non-archive or auto-unzip disabled: copy directly from cache to SAF
                val documentFile = downloadFileManager.createDocumentFile(file, downloadDirUri.toString(), subPath)
                    ?: throw Exception("Failed to create destination file in storage.")
                Log.d(TAG, "Copying torrent file to SAF: ${documentFile.uri}")
                updateStatus(file.fileName, DownloadStatus.COPYING)
                context.contentResolver.openOutputStream(documentFile.uri)?.use { out ->
                    BufferedOutputStream(out, Constants.EXTRACTION_BUFFER_SIZE).use { buffOut ->
                        internalFile.inputStream().use { it.copyTo(buffOut, Constants.EXTRACTION_BUFFER_SIZE) }
                    }
                } ?: throw Exception("Could not open output stream for ${documentFile.uri}")
            }

            // Clean up cache
            internalFile.delete()
            internalFile.parentFile?.takeIf { it.list()?.isEmpty() == true }?.delete()

            // Release handle only when no sibling files still downloading from same torrent
            torrentDownloadService.finishDownload(file)

            Log.i(TAG, "Torrent processed successfully: ${file.fileName}")
            updateStatus(file.fileName, DownloadStatus.COMPLETED)
            checkServiceLifecycle()

        } catch (e: Exception) {
            Log.e(TAG, "Error processing torrent file for ${file.fileName}: ${e.message}", e)
            updateStatus(file.fileName, DownloadStatus.FAILED)
        }
    }

    private suspend fun performHttpDownload(file: DownloadableFileEntity) {
        repeat(3) { attempt ->
            try {
                if (attempt > 0) delay(2000L * attempt)
                performHttpDownloadAttempt(file)
                return
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed for ${file.fileName}: ${e.message}")
                if (attempt == 2) throw e
            }
        }
    }

    private suspend fun performHttpDownloadAttempt(file: DownloadableFileEntity) {
        val downloadDirUri = downloadFileManager.getDownloadDirectoryUri(file)
        if (downloadDirUri == android.net.Uri.EMPTY)
            throw Exception("Download directory not configured or no longer accessible.")

        var speedLimit = settingsRepository.limitSpeed.first()
        val speedLimitJob = downloadSpeedController.createSpeedLimiter { speedLimit = it }

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var documentFile: DocumentFile? = null

        try {
            val connection = downloadHttpClient.createConnection(file.downloadUrl)
            val contentLength = connection.contentLengthLong
            inputStream = connection.inputStream

            val subPath = downloadFileManager.getSubPath(file)
            documentFile = downloadFileManager.createDocumentFile(file, downloadDirUri.toString(), subPath)
                ?: throw Exception("Failed to create file in storage.")
            outputStream = downloadFileManager.getOutputStream(documentFile)
                ?: throw Exception("Failed to open output stream for ${documentFile.uri}")

            streamWithProgress(inputStream, outputStream, file, speedLimit, contentLength)
            handlePostDownload(file, documentFile, subPath)

        } catch (e: kotlinx.coroutines.CancellationException) {
            documentFile?.let { downloadFileManager.deleteFile(it) }
            updateStatus(file.fileName, DownloadStatus.STOPPED)
            throw e
        } catch (e: Exception) {
            documentFile?.let { downloadFileManager.deleteFile(it) }
            updateStatus(file.fileName, DownloadStatus.FAILED)
            throw e
        } finally {
            speedLimitJob.cancel()
            inputStream?.close()
            outputStream?.close()
        }
    }

    private suspend fun streamWithProgress(
        input: InputStream,
        output: OutputStream,
        file: DownloadableFileEntity,
        initialSpeedLimit: Float,
        contentLength: Long
    ) {
        val buffer = ByteArray(Constants.BUFFER_SIZE)
        var downloaded = 0L
        var bytesSinceCheck = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        var lastDownloaded = 0L
        var lastSpeedCheckTime = startTime

        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            if (downloadJobs[file.fileName]?.isCancelled == true) {
                updateStatus(file.fileName, DownloadStatus.STOPPED)
                return
            }

            output.write(buffer, 0, bytesRead)
            downloaded += bytesRead
            bytesSinceCheck += bytesRead

            val now = System.currentTimeMillis()
            val timeSinceCheck = (now - lastSpeedCheckTime) / 1000f
            if (timeSinceCheck >= Constants.SPEED_CHECK_INTERVAL_MS / 1000f) {
                val spd = downloadSpeedController.calculateSpeed(bytesSinceCheck, timeSinceCheck)
                downloadSpeedController.applySpeedThrottling(spd, initialSpeedLimit, bytesSinceCheck, timeSinceCheck)
                lastSpeedCheckTime = now
                bytesSinceCheck = 0L
            }

            val progress = if (contentLength > 0)
                ArchiveExtractionUtils.calculateProgress(downloaded, contentLength) else 0f

            if (downloadProgressTracker.shouldUpdateProgress(progress, lastUpdateTime, now)) {
                val elapsed = (now - lastUpdateTime) / 1000f
                val speedMBs = downloadSpeedController.calculateSpeed(downloaded - lastDownloaded, elapsed)
                    .takeIf { it > 0 }
                    ?: downloadSpeedController.calculateSpeed(downloaded, (now - startTime) / 1000f)
                downloadProgressTracker.updateDownloadProgress(file.fileName, progress, speedMBs, downloaded)
                lastUpdateTime = now
                lastDownloaded = downloaded
            }
        }
    }

    private suspend fun handlePostDownload(
        file: DownloadableFileEntity,
        documentFile: DocumentFile,
        subPath: String
    ) {
        if (!ArchiveUtils.isExtractable(file.fileExtension) || !settingsRepository.autoUnzip.first()) {
            updateStatus(file.fileName, DownloadStatus.COMPLETED)
            checkServiceLifecycle()
            return
        }

        updateStatus(file.fileName, DownloadStatus.UNZIPPING)
        try {
            val extracted = archiveExtractorService.extractArchive(
                context, documentFile.uri, downloadFileManager.getDownloadDirectoryUri(file), subPath)
            if (extracted.isNotEmpty()) {
                downloadFileManager.deleteFile(documentFile)
                extractedFilesMap[file.fileName] = extracted
            } else {
                Log.w(TAG, "Extraction produced no files for ${file.fileName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed for ${file.fileName}: ${e.message}")
        }
        updateStatus(file.fileName, DownloadStatus.COMPLETED)
        checkServiceLifecycle()
    }

    private suspend fun updateStatus(fileName: String, status: DownloadStatus) =
        downloadProgressTracker.updateDownloadStatus(fileName, status)

    private fun startForegroundService() {
        context.startForegroundService(Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_START_SERVICE
        })
        foregroundServiceStarted = true
    }

    private fun checkServiceLifecycle() {
        if (foregroundServiceStarted && !downloadProgressTracker.hasActiveDownloads()) {
            context.startService(Intent(context, DownloadForegroundService::class.java).apply {
                action = DownloadForegroundService.ACTION_STOP_SERVICE
            })
            foregroundServiceStarted = false
        }
    }
}

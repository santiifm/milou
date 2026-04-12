package com.santiifm.milou.data.service

import android.content.Context
import android.util.Log
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.DownloadStatus
import com.santiifm.milou.util.StorageHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libtorrent4j.Priority
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts a selective torrent file download:
 * 1. Gets the handle from [TorrentHandleRegistry] (instant cache hit after indexing).
 * 2. Sets every file to IGNORE except [DownloadableFileEntity.torrentFileIndex].
 * 3. Resumes the torrent and hands off to [TorrentProgressBridge] for updates.
 */
@Singleton
class TorrentDownloadService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: TorrentHandleRegistry,
    private val progressBridge: TorrentProgressBridge,
    private val downloadFileManager: DownloadFileManager,
    private val progressTracker: DownloadProgressTracker
) {
    data class TorrentFileInfo(val relativePath: String, val expectedSize: Long)

    // Caches file path/size at download-start time so moveTorrentFile can proceed even if the
    // handle is later invalidated (e.g. session stopped during app shutdown before copy finishes).
    private val fileInfoCache = ConcurrentHashMap<String, TorrentFileInfo>()

    fun getFileInfo(fileName: String): TorrentFileInfo? = fileInfoCache[fileName]

    suspend fun startDownload(file: DownloadableFileEntity) = withContext(Dispatchers.IO) {
        val magnet = file.torrentMagnet
            ?: throw IllegalArgumentException("No torrentMagnet on ${file.fileName}")
        val fileIndex = file.torrentFileIndex
            ?: throw IllegalArgumentException("No torrentFileIndex on ${file.fileName}")

        val handle = try {
            registry.getOrFetch(magnet)
        } catch (e: TorrentMetadataTimeoutException) {
            Log.e(TAG, "Metadata timeout: ${e.message}")
            progressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
            return@withContext
        }

        val torrentInfo = handle.torrentFile() ?: run {
            Log.e(TAG, "No TorrentInfo for ${file.fileName}")
            progressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
            return@withContext
        }

        if (fileIndex >= torrentInfo.numFiles()) {
            Log.e(TAG, "fileIndex $fileIndex out of range")
            progressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
            return@withContext
        }

        val downloadDirUri = downloadFileManager.getDownloadDirectoryUri(file)
        val subPath = downloadFileManager.getSubPath(file)

        val finalDir = StorageHelper.createDirectory(context, downloadDirUri.toString(), subPath)
        if (finalDir == null) {
            Log.e(TAG, "Could not create/access download directory")
            progressTracker.updateDownloadStatus(file.fileName, DownloadStatus.FAILED)
            return@withContext
        }

        // Cache file path/size before tracking so moveTorrentFile can find the file
        // even if the handle is gone at move time.
        val relativePath = torrentInfo.files().filePath(fileIndex)
        val expectedSize = torrentInfo.files().fileSize(fileIndex)
        fileInfoCache[file.fileName] = TorrentFileInfo(relativePath, expectedSize)

        // Track first so getTrackedFileIndicesForHandle includes this file when building priorities.
        progressBridge.trackDownload(file.fileName, fileIndex, handle)

        // Build merged priorities: IGNORE all files, then DEFAULT for every file currently tracked
        // from this torrent (including the one we just added). This prevents concurrent startDownload
        // calls from clobbering each other's priority and leaving sibling files un-downloaded.
        val priorities = Array(torrentInfo.numFiles()) { Priority.IGNORE }
        for (idx in progressBridge.getTrackedFileIndicesForHandle(handle)) {
            if (idx < priorities.size) priorities[idx] = Priority.DEFAULT
        }
        handle.prioritizeFiles(priorities)

        // Only move storage if the torrent isn't already downloading to our cache directory.
        // Calling moveStorage() is asynchronous — redundant calls on the same handle fire
        // unnecessary STORAGE_MOVED_ALERT events and can briefly pause in-flight downloads.
        val torrentDataDir = File(context.cacheDir, "torrent_data").apply { mkdirs() }
        val currentSavePath = try { handle.status().swig().save_path } catch (_: Exception) { null }
        if (currentSavePath != torrentDataDir.absolutePath) {
            handle.moveStorage(torrentDataDir.absolutePath)
        }

        handle.resume()

        Log.i(TAG, "Torrent resumed for ${file.fileName} — TorrentProgressBridge takes over")
    }

    suspend fun finishDownload(file: DownloadableFileEntity) = withContext(Dispatchers.IO) {
        val magnet = file.torrentMagnet ?: return@withContext
        val fileIndex = file.torrentFileIndex ?: return@withContext

        fileInfoCache.remove(file.fileName)
        val handle = registry.getCachedHandle(magnet)
        progressBridge.untrackDownload(file.fileName, fileIndex)

        val stillTracked = if (handle != null) progressBridge.countTrackedForHandle(handle) else 0
        if (stillTracked == 0) {
            registry.releaseHandle(magnet)
            Log.i(TAG, "Handle released after successful move for ${file.fileName}")
        } else {
            Log.i(TAG, "Handle kept alive after move for ${file.fileName} ($stillTracked files still active)")
        }
    }

    suspend fun cancelDownload(file: DownloadableFileEntity) = withContext(Dispatchers.IO) {
        val magnet = file.torrentMagnet ?: return@withContext
        val fileIndex = file.torrentFileIndex ?: return@withContext

        fileInfoCache.remove(file.fileName)
        val handle = registry.getCachedHandle(magnet)
        progressBridge.untrackDownload(file.fileName, fileIndex)

        val remainingForThisTorrent = if (handle != null) progressBridge.countTrackedForHandle(handle) else 0
        if (remainingForThisTorrent == 0) {
            registry.releaseHandle(magnet)
            Log.i(TAG, "Stopped and purged torrent download for ${file.fileName}")
        } else {
            Log.i(TAG, "Stopped tracking ${file.fileName}, handle kept alive ($remainingForThisTorrent files still active)")
        }

        progressTracker.updateDownloadStatus(file.fileName, DownloadStatus.STOPPED)
    }

    companion object { private const val TAG = "TorrentDownloadService" }
}

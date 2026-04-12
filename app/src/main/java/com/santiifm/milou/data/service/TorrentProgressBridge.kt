package com.santiifm.milou.data.service

import android.util.Log
import com.santiifm.milou.data.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.libtorrent4j.AlertListener
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.FileErrorAlert
import org.libtorrent4j.alerts.TorrentErrorAlert
import org.libtorrent4j.alerts.TorrentFinishedAlert
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges libtorrent4j alert callbacks into [DownloadProgressTracker] updates.
 * Registered as an alert listener on the session in [DownloadForegroundService].
 */
@Singleton
class TorrentProgressBridge @Inject constructor(
    private val progressTracker: DownloadProgressTracker
) : AlertListener {

    private data class TrackedFile(
        val fileIndex: Int,
        val handle: TorrentHandle,
        /** Cached at track time — fileSize never changes, no need to re-call JNI every tick. */
        val expectedSize: Long
    )

    // Single map replaces the old separate `tracked` (fileName→index) and `handles` (fileName→handle).
    private val tracked = mutableMapOf<String, TrackedFile>()
    // Polling runs on IO — avoids JNI calls on the main thread.
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var pollingJob: Job? = null

    fun trackDownload(fileName: String, fileIndex: Int, handle: TorrentHandle) {
        // Read expectedSize outside the lock to avoid holding the monitor during a JNI call.
        val expectedSize = handle.torrentFile()?.files()?.fileSize(fileIndex) ?: 0L
        synchronized(this) {
            tracked[fileName] = TrackedFile(fileIndex, handle, expectedSize)
            startPolling()
        }
        Log.d(TAG, "Tracking $fileName at index $fileIndex")
    }

    fun untrackDownload(fileName: String, fileIndex: Int) {
        synchronized(this) {
            tracked.remove(fileName)
            if (tracked.isEmpty()) stopPolling()
        }
    }

    private fun startPolling() {
        if (pollingJob != null) return
        pollingJob = scope.launch {
            while (true) {
                updateProgress()
                delay(1000L)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun updateProgress() {
        // Snapshot under lock so we never hold the monitor during JNI calls. libtorrent alert
        // callbacks (onTorrentFinished etc.) also acquire this lock, so holding it while calling
        // native methods risks a deadlock.
        val snapshot: Map<String, TrackedFile>
        synchronized(this) { snapshot = tracked.toMap() }

        snapshot.forEach { (fileName, info) ->
            val handle = info.handle
            if (!handle.isValid) return@forEach

            val status = try { handle.status() } catch (e: Exception) { return@forEach }
            val total = info.expectedSize
            if (total <= 0) return@forEach

            val fileProgressBytes = try { handle.fileProgress() } catch (e: Exception) { null }
            val downloaded = if (fileProgressBytes != null && info.fileIndex < fileProgressBytes.size) {
                fileProgressBytes[info.fileIndex]
            } else {
                (status.progress() * total).toLong()
            }
            val progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val speedMBs = status.downloadPayloadRate() / (1024f * 1024f)

            val state = status.state()
            val isChecking = state == org.libtorrent4j.TorrentStatus.State.CHECKING_FILES ||
                             state == org.libtorrent4j.TorrentStatus.State.CHECKING_RESUME_DATA
            val isFinished = downloaded >= total

            val currentItem = progressTracker.downloads.value.find { it.fileName == fileName }
            val currentStatus = currentItem?.status

            when {
                isFinished && currentStatus != DownloadStatus.COMPLETED &&
                currentStatus != DownloadStatus.UNZIPPING -> {
                    progressTracker.updateDownloadStatus(fileName, DownloadStatus.COMPLETED)
                }
                isChecking -> {
                    if (currentStatus != DownloadStatus.DOWNLOADING)
                        progressTracker.updateDownloadStatus(fileName, DownloadStatus.DOWNLOADING)
                }
                !isFinished && !isChecking && currentStatus != DownloadStatus.DOWNLOADING -> {
                    progressTracker.updateDownloadStatus(fileName, DownloadStatus.DOWNLOADING)
                }
            }

            progressTracker.updateDownloadProgress(fileName, progress, speedMBs, downloaded)
        }
    }

    fun countTrackedForHandle(handle: TorrentHandle): Int {
        synchronized(this) {
            return tracked.values.count { it.handle.infoHash() == handle.infoHash() }
        }
    }

    fun getTrackedFileIndicesForHandle(handle: TorrentHandle): Set<Int> {
        synchronized(this) {
            return tracked.values
                .filter { it.handle.infoHash() == handle.infoHash() }
                .map { it.fileIndex }
                .toSet()
        }
    }

    override fun types(): IntArray = intArrayOf(
        AlertType.TORRENT_FINISHED.swig(),
        AlertType.FILE_ERROR.swig(),
        AlertType.TORRENT_ERROR.swig()
    )

    override fun alert(alert: Alert<*>) {
        when (alert.type()) {
            AlertType.TORRENT_FINISHED -> onTorrentFinished(alert as TorrentFinishedAlert)
            AlertType.FILE_ERROR       -> onFileError(alert as FileErrorAlert)
            AlertType.TORRENT_ERROR    -> onTorrentError(alert as TorrentErrorAlert)
            else -> Unit
        }
    }

    private fun onTorrentFinished(alert: TorrentFinishedAlert) {
        val handle = alert.handle() ?: return
        val snapshot: Map<String, TrackedFile>
        synchronized(this) { snapshot = tracked.toMap() }
        snapshot.entries
            .filter { (_, info) -> info.handle.infoHash() == handle.infoHash() }
            .forEach { (fn, _) ->
                val currentItem = progressTracker.downloads.value.find { it.fileName == fn }
                if (currentItem?.status != DownloadStatus.UNZIPPING &&
                    currentItem?.status != DownloadStatus.COMPLETED) {
                    progressTracker.updateDownloadStatus(fn, DownloadStatus.COMPLETED)
                    Log.i(TAG, "Torrent finished: $fn — Now moving")
                }
            }
    }

    private fun onFileError(alert: FileErrorAlert) {
        val handle = alert.handle() ?: return
        Log.e(TAG, "File error: ${alert.message()}")
        val snapshot: Map<String, TrackedFile>
        synchronized(this) { snapshot = tracked.toMap() }
        snapshot.entries
            .filter { (_, info) -> info.handle.infoHash() == handle.infoHash() }
            .forEach { (fn, _) -> progressTracker.updateDownloadStatus(fn, DownloadStatus.FAILED) }
    }

    private fun onTorrentError(alert: TorrentErrorAlert) {
        val handle = alert.handle() ?: return
        Log.e(TAG, "Torrent error: ${alert.message()}")
        val snapshot: Map<String, TrackedFile>
        synchronized(this) { snapshot = tracked.toMap() }
        snapshot.entries
            .filter { (_, info) -> info.handle.infoHash() == handle.infoHash() }
            .forEach { (fn, _) -> progressTracker.updateDownloadStatus(fn, DownloadStatus.FAILED) }
    }

    companion object { private const val TAG = "TorrentProgressBridge" }
}

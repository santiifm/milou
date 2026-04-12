package com.santiifm.milou.data.service

import android.util.Log
import com.santiifm.milou.data.model.TorrentFileEntry
import org.libtorrent4j.TorrentInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Walks the file tree inside a [TorrentInfo] and returns indexable ROM files.
 *
 * Filtering applied (in order):
 * 1. Zero-size entries and hidden files (leading dot) are always skipped.
 * 2. Files whose extension matches [BLOCKED_EXTENSIONS] are skipped.
 * 3. If [allowedFolders] is non-empty, only files whose torrent path contains
 *    one of the specified folder segments are included (case-insensitive).
 *    If empty, all surviving files are included.
 *
 * Example:
 *   torrent path  : "Redump/Sony - PS2/Gran Turismo 3 (USA).iso"
 *   allowedFolders: ["Sony - PS2"]  → included
 *   allowedFolders: ["Nintendo 64"] → excluded
 *   allowedFolders: []              → included (no filter)
 */
@Singleton
class TorrentFileIndexer @Inject constructor() {

    fun index(info: TorrentInfo, magnet: String, allowedFolders: List<String> = emptyList()): List<TorrentFileEntry> {
        val files = info.files()
        val results = mutableListOf<TorrentFileEntry>()

        val normalizedFolders = allowedFolders.map { it.replace('\\', '/').lowercase().trimEnd('/') }

        for (i in 0 until files.numFiles()) {
            val rawPath = files.filePath(i)
            val size = files.fileSize(i)

            if (size == 0L) continue
            if (rawPath.isBlank()) continue

            val fileName = rawPath.substringAfterLast('/').trim()
            if (fileName.startsWith(".") || fileName.isBlank()) continue

            val ext = ".${fileName.substringAfterLast('.', "")}".lowercase()
            if (ext in BLOCKED_EXTENSIONS) continue

            if (normalizedFolders.isNotEmpty()) {
                val normalizedPath = rawPath.replace('\\', '/').lowercase()
                val inAllowedFolder = normalizedFolders.any { folder ->
                    normalizedPath.startsWith("$folder/") || normalizedPath.contains("/$folder/")
                }
                if (!inAllowedFolder) continue
            }

            results.add(TorrentFileEntry(fileName, i, size, magnet))
        }

        Log.i(TAG, "Indexed ${results.size} files in '${info.name()}'" +
            if (normalizedFolders.isNotEmpty()) " (folders: $allowedFolders)" else "")
        return results
    }

    companion object {
        private const val TAG = "TorrentFileIndexer"

        private val BLOCKED_EXTENSIONS = setOf(
            // Metadata / database
            ".xml", ".sqlite", ".nfo", ".txt", ".pdf", ".log", ".html", ".htm",
            // Checksums / verification
            ".sfv", ".md5", ".sha1", ".sha256",
            // Images
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            // System / hidden
            ".ds_store"
        )
    }
}

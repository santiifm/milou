package com.santiifm.milou.data.model

/**
 * A single ROM file discovered inside a torrent.
 *
 * @param fileName      File name only (no directory path).
 * @param fileIndex     Zero-based index in the torrent's file storage —
 *                      used to set selective-download priority.
 * @param fileSize      Size in bytes from torrent metadata.
 * @param torrentMagnet The magnet URI this file belongs to.
 */
data class TorrentFileEntry(
    val fileName: String,
    val fileIndex: Int,
    val fileSize: Long,
    val torrentMagnet: String
)

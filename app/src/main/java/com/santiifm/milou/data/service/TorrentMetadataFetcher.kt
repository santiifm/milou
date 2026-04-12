package com.santiifm.milou.data.service

import org.libtorrent4j.TorrentInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [TorrentHandleRegistry.getOrFetch] that returns
 * [TorrentInfo] directly — the object the scraping pipeline needs.
 */
@Singleton
class TorrentMetadataFetcher @Inject constructor(
    private val registry: TorrentHandleRegistry
) {
    suspend fun fetch(magnet: String): TorrentInfo {
        val handle = registry.getOrFetch(magnet)
        return checkNotNull(handle.torrentFile()) {
            "TorrentHandle has no TorrentInfo after fetch for: $magnet"
        }
    }
}

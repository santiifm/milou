package com.santiifm.milou.util

object TorrentConstants {
    const val METADATA_FETCH_TIMEOUT_MS = 20_000L
    const val METADATA_POLL_INTERVAL_MS = 500L
    const val SESSION_FLAGS = 0x003L

    val DHT_BOOTSTRAP_NODES = listOf(
        "router.bittorrent.com" to 6881,
        "dht.transmissionbt.com" to 6881,
        "router.utorrent.com" to 6881,
        "dht.aelitis.com" to 6881
    )

    const val PRIORITY_DO_NOT_DOWNLOAD = 0
    const val PRIORITY_NORMAL = 4

    const val MAX_TRACKERS_PER_MAGNET = 4
}

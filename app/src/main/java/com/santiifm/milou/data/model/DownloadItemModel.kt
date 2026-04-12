package com.santiifm.milou.data.model

import com.santiifm.milou.R

enum class DownloadStatus {
    DOWNLOADING,
    COPYING,
    UNZIPPING,
    COMPLETED,
    FAILED,
    STOPPED
}

data class StatusAssets(
    val currentStatusIcon: Int,
    val availableStatusIcons: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatusAssets

        if (currentStatusIcon != other.currentStatusIcon) return false
        if (!availableStatusIcons.contentEquals(other.availableStatusIcons)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentStatusIcon
        result = 31 * result + availableStatusIcons.contentHashCode()
        return result
    }
}

data class DownloadItemModel(
    val name: String,
    val fileName: String,
    val downloadSpeed: Float, // MB/s
    val progress: Float, // 0f..1f
    val fileSize: Long, // bytes
    val downloadedBytes: Long = 0L, // bytes downloaded so far
    val status: DownloadStatus
)
fun DownloadItemModel.getStatusAssets(): StatusAssets = when (status) {
    DownloadStatus.DOWNLOADING ->
        StatusAssets(
            currentStatusIcon = R.drawable.ic_arrow_down,
            availableStatusIcons = intArrayOf(
                R.drawable.ic_stop
            )
        )
    DownloadStatus.COPYING ->
        StatusAssets(
            currentStatusIcon = R.drawable.ic_folder,
            availableStatusIcons = intArrayOf()
        )
    DownloadStatus.UNZIPPING ->
        StatusAssets(
            currentStatusIcon = R.drawable.ic_extract,
            availableStatusIcons = intArrayOf(
                R.drawable.ic_stop
            )
        )
    DownloadStatus.COMPLETED ->
        StatusAssets(
            currentStatusIcon = R.drawable.ic_arrow_up,
            availableStatusIcons = intArrayOf(
                R.drawable.ic_retry,
                R.drawable.ic_trash
            )
        )
    DownloadStatus.FAILED ->
        StatusAssets(
            currentStatusIcon = R.drawable.ic_error,
            availableStatusIcons = intArrayOf(
                R.drawable.ic_retry,
                R.drawable.ic_trash
            )
        )
    DownloadStatus.STOPPED ->
        StatusAssets(
            currentStatusIcon = R.drawable.ic_stop,
            availableStatusIcons = intArrayOf(
                R.drawable.ic_retry,
                R.drawable.ic_trash
            )
        )
}

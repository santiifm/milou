package com.santiifm.milou.data.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val downloadDirectory: Flow<String>
    val separateByConsole: Flow<Boolean>
    val limitSpeed: Flow<Float>
    val autoUnzip: Flow<Boolean>
    val concurrentDownloads: Flow<Int>
    val consoleDownloadDirectories: Flow<Map<String, String>>

    suspend fun updateDownloadDirectory(path: String): Preferences
    suspend fun setSeparateByConsole(enabled: Boolean): Preferences
    suspend fun setLimitSpeed(limit: Float): Preferences
    suspend fun setAutoUnzip(enabled: Boolean): Preferences
    suspend fun setConcurrentDownloads(count: Int): Preferences
    suspend fun updateConsoleDownloadDirectory(consoleId: String, path: String)
}

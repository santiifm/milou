package com.santiifm.milou.data.repository

import androidx.datastore.preferences.core.Preferences
import com.santiifm.milou.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val downloadDirectory: Flow<String> = settingsDataStore.downloadDirectory
    override val separateByConsole: Flow<Boolean> = settingsDataStore.separateByConsole
    override val limitSpeed: Flow<Float> = settingsDataStore.limitSpeed
    override val autoUnzip: Flow<Boolean> = settingsDataStore.autoUnzip
    override val concurrentDownloads: Flow<Int> = settingsDataStore.concurrentDownloads
    override val consoleDownloadDirectories: Flow<Map<String, String>> = settingsDataStore.consoleDownloadDirectories

    override suspend fun updateDownloadDirectory(path: String): Preferences {
        return settingsDataStore.updateDownloadDirectory(path)
    }

    override suspend fun setSeparateByConsole(enabled: Boolean): Preferences {
        return settingsDataStore.setSeparateByConsole(enabled)
    }

    override suspend fun setLimitSpeed(limit: Float): Preferences {
        return settingsDataStore.setLimitSpeed(limit)
    }

    override suspend fun setAutoUnzip(enabled: Boolean): Preferences {
        return settingsDataStore.setAutoUnzip(enabled)
    }

    override suspend fun setConcurrentDownloads(count: Int): Preferences {
        return settingsDataStore.setConcurrentDownloads(count)
    }

    override suspend fun updateConsoleDownloadDirectory(consoleId: String, path: String) {
        settingsDataStore.updateConsoleDownloadDirectory(consoleId, path)
    }
}

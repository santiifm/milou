package com.santiifm.milou.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.santiifm.milou.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = Constants.SETTINGS_DATASTORE_NAME)

object SettingsKeys {
    val DOWNLOAD_DIRECTORY = stringPreferencesKey("download_directory")
    val SEPARATE_BY_CONSOLE = booleanPreferencesKey("separate_by_console")
    val LIMIT_SPEED = floatPreferencesKey("limit_speed")
    val AUTO_UNZIP = booleanPreferencesKey("auto_unzip")
    val CONCURRENT_DOWNLOADS = intPreferencesKey("concurrent_downloads")
    val CONSOLE_DOWNLOAD_DIRECTORIES = stringSetPreferencesKey("console_download_directories")
}

@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val downloadDirectory: Flow<String> = context.dataStore.data.map { 
        it[SettingsKeys.DOWNLOAD_DIRECTORY] ?: "" 
    }
    val separateByConsole: Flow<Boolean> = context.dataStore.data.map { 
        it[SettingsKeys.SEPARATE_BY_CONSOLE] ?: false 
    }
    val limitSpeed: Flow<Float> = context.dataStore.data.map { 
        it[SettingsKeys.LIMIT_SPEED] ?: Float.POSITIVE_INFINITY 
    }
    val autoUnzip: Flow<Boolean> = context.dataStore.data.map { 
        it[SettingsKeys.AUTO_UNZIP] ?: true 
    }
    val concurrentDownloads: Flow<Int> = context.dataStore.data.map { 
        it[SettingsKeys.CONCURRENT_DOWNLOADS] ?: Constants.DEFAULT_CONCURRENT_DOWNLOADS 
    }
    val consoleDownloadDirectories: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        (preferences[SettingsKeys.CONSOLE_DOWNLOAD_DIRECTORIES] ?: emptySet()).associate {
            val (key, value) = it.split(":", limit = 2)
            key to value
        }
    }

    suspend fun updateDownloadDirectory(path: String) = context.dataStore.edit { it[SettingsKeys.DOWNLOAD_DIRECTORY] = path }
    suspend fun setSeparateByConsole(enabled: Boolean) = context.dataStore.edit { it[SettingsKeys.SEPARATE_BY_CONSOLE] = enabled }
    suspend fun setLimitSpeed(limit: Float) = context.dataStore.edit { it[SettingsKeys.LIMIT_SPEED] = limit}
    suspend fun setAutoUnzip(enabled: Boolean) = context.dataStore.edit { it[SettingsKeys.AUTO_UNZIP] = enabled }
    suspend fun setConcurrentDownloads(count: Int) = context.dataStore.edit { it[SettingsKeys.CONCURRENT_DOWNLOADS] = count }
    suspend fun updateConsoleDownloadDirectory(consoleId: String, path: String) {
        context.dataStore.edit { settings ->
            val currentDirs = settings[SettingsKeys.CONSOLE_DOWNLOAD_DIRECTORIES] ?: emptySet()
            val newDirs = currentDirs.filterNot { it.startsWith("$consoleId:") }.toMutableSet()
            if (path.isNotEmpty()) {
                newDirs.add("$consoleId:$path")
            }
            settings[SettingsKeys.CONSOLE_DOWNLOAD_DIRECTORIES] = newDirs
        }
    }
}

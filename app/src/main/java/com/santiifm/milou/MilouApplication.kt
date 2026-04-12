package com.santiifm.milou

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.sf.sevenzipjbinding.SevenZip
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class MilouApplication : Application() {
    
    @Inject
    lateinit var versionCheckerService: com.santiifm.milou.data.service.VersionCheckerService
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        applicationScope.launch {
            clearStaleCache()

            // Load the native 7-zip library (blocking — must run on IO thread)
            try {
                SevenZip.initSevenZipFromPlatformJAR()
                Log.d("MilouApplication", "7-Zip native library loaded")
            } catch (e: Exception) {
                Log.e("MilouApplication", "Failed to load 7-Zip native library: ${e.message}")
            }
        }

        // Check for updates on app startup
        applicationScope.launch {
            versionCheckerService.checkForUpdates(this@MilouApplication)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            DOWNLOAD_CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active download progress"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Deletes cache directories left over from downloads that were interrupted before
     * they could finish copying to the user's storage and clean up after themselves.
     * Safe to run at startup because no downloads are active yet.
     */
    private fun clearStaleCache() {
        listOf("extraction_temp", "torrent_data").forEach { dir ->
            val cacheDir = File(cacheDir, dir)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                Log.d("MilouApplication", "Cleared stale cache: $dir")
            }
        }
    }

    companion object {
        const val DOWNLOAD_CHANNEL_ID = "download_channel"
    }
}
package com.santiifm.milou.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.santiifm.milou.MilouApplication
import com.santiifm.milou.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var downloadService: DownloadService
    @Inject lateinit var torrentHandleRegistry: TorrentHandleRegistry
    @Inject lateinit var torrentProgressBridge: TorrentProgressBridge

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START_SERVICE = "start_service"
        const val ACTION_STOP_SERVICE = "stop_service"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        torrentHandleRegistry.start()
        torrentHandleRegistry.session().addListener(torrentProgressBridge)

        // Keep the CPU awake so downloads continue when the screen is off.
        // Released in onDestroy when all downloads have finished.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "milou:DownloadWakeLock").apply {
            acquire()
        }
    }

    override fun onDestroy() {
        downloadService.cancelAllDownloads()
        torrentHandleRegistry.session().removeListener(torrentProgressBridge)
        torrentHandleRegistry.stop()
        wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> promoteToForeground()
            ACTION_STOP_SERVICE -> stopSelf()
        }
        return START_STICKY
    }

    private fun promoteToForeground() {
        val notification = NotificationCompat.Builder(this, MilouApplication.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText("Downloads are running in the background")
            .setSmallIcon(R.drawable.ic_arrow_down)
            .setOngoing(true)
            .setSilent(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override fun onBind(intent: Intent?) = null
}

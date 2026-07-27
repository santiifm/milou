package com.santiifm.milou.data.service

import com.santiifm.milou.domain.event.DownloadEvent
import com.santiifm.milou.domain.eventbus.StandardEventBus
import com.santiifm.milou.domain.model.DownloadStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadProgressTrackerTest {

    @Test
    fun `tracker should update state when receiving download events`() = runTest {
        val eventBus = StandardEventBus()
        val tracker = DownloadProgressTracker(eventBus)
        val downloadId = "test-id"

        // 1. Start download
        eventBus.publish(
            DownloadEvent.Started(
                downloadId = downloadId,
                name = "Test Game",
                fileName = "test.zip",
                fileSize = 1000L
            )
        )
        
        // Use a small delay for background flow collection
        kotlinx.coroutines.delay(100)
        
        assertEquals(1, tracker.downloads.value.size)
        val item = tracker.downloads.value[0]
        assertEquals(downloadId, item.id)
        assertEquals(DownloadStatus.DOWNLOADING, item.status)

        // 2. Progress update
        eventBus.publish(
            DownloadEvent.Progress(
                downloadId = downloadId,
                progress = 0.5f,
                speed = 10f,
                downloadedBytes = 500L
            )
        )
        
        kotlinx.coroutines.delay(100)
        assertEquals(0.5f, tracker.downloads.value[0].progress)
        assertEquals(10f, tracker.downloads.value[0].downloadSpeed)

        // 3. Status changed to COPYING
        eventBus.publish(DownloadEvent.StatusChanged(downloadId, DownloadStatus.COPYING))
        
        kotlinx.coroutines.delay(100)
        assertEquals(DownloadStatus.COPYING, tracker.downloads.value[0].status)
    }
}

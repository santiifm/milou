package com.santiifm.milou.di

import com.santiifm.milou.data.service.TorrentFileIndexer
import com.santiifm.milou.data.service.TorrentHandleRegistry
import com.santiifm.milou.data.service.TorrentMetadataFetcher
import com.santiifm.milou.data.service.TorrentProgressBridge
import com.santiifm.milou.data.service.DownloadProgressTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the torrent engine.
 *
 * All services annotated with @Singleton + @Inject are discovered automatically
 * by Hilt via constructor injection:
 *   TorrentScrapingService, TorrentDownloadService, AppStatusNotificationService
 *
 * The classes below need explicit @Provides because they are either constructed
 * without an @Inject constructor (TorrentHandleRegistry is plain-constructed so
 * the module controls the single instance) or depend on the registry instance
 * that must be the same object everywhere.
 */
@Module
@InstallIn(SingletonComponent::class)
object TorrentModule {

    /**
     * The single libtorrent4j session owner. Explicit @Provides so every
     * injection site gets the exact same instance — critical because the
     * registry holds the handle cache and the session reference.
     */
    @Provides
    @Singleton
    fun provideTorrentHandleRegistry(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): TorrentHandleRegistry = TorrentHandleRegistry(context)

    @Provides
    @Singleton
    fun provideTorrentMetadataFetcher(
        registry: TorrentHandleRegistry
    ): TorrentMetadataFetcher = TorrentMetadataFetcher(registry)

    @Provides
    @Singleton
    fun provideTorrentFileIndexer(): TorrentFileIndexer = TorrentFileIndexer()

    /**
     * Progress bridge needs an explicit @Provides because it is also registered
     * as an AlertListener on the session in DownloadForegroundService — the
     * injected instance and the listener must be the same object.
     */
    @Provides
    @Singleton
    fun provideTorrentProgressBridge(
        progressTracker: DownloadProgressTracker
    ): TorrentProgressBridge = TorrentProgressBridge(progressTracker)
}

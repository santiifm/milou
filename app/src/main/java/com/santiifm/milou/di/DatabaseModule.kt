package com.santiifm.milou.di

import android.content.Context
import androidx.room.Room
import com.santiifm.milou.data.local.MilouDatabase
import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.dao.ManufacturerDao
import com.santiifm.milou.data.local.SettingsDataStore
import com.santiifm.milou.data.repository.DownloadRepository
import com.santiifm.milou.data.repository.DownloadRepositoryImpl
import com.santiifm.milou.data.repository.SettingsRepository
import com.santiifm.milou.data.repository.SettingsRepositoryImpl
import com.santiifm.milou.data.service.ArchiveExtractorService
import com.santiifm.milou.data.repository.DownloadableFileRepository
import com.santiifm.milou.domain.repository.SearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MilouDatabase =
        Room.databaseBuilder(context, MilouDatabase::class.java, "milou_db")
            .addMigrations(
                MilouDatabase.MIGRATION_1_2, 
                MilouDatabase.MIGRATION_2_3, 
                MilouDatabase.MIGRATION_3_4
            )
            .build()

    @Provides
    fun provideConsoleDao(db: MilouDatabase): ConsoleDao = db.consoleDao()

    @Provides
    fun provideDownloadableFileDao(db: MilouDatabase): DownloadableFileDao = db.downloadableFileDao()

    @Provides
    fun provideManufacturerDao(db: MilouDatabase): ManufacturerDao = db.manufacturerDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore = SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(settingsRepositoryImpl: SettingsRepositoryImpl): SettingsRepository = settingsRepositoryImpl

    @Provides
    @Singleton
    fun provideDownloadRepository(downloadRepositoryImpl: DownloadRepositoryImpl): DownloadRepository = downloadRepositoryImpl

    @Provides
    @Singleton
    fun provideSearchRepository(downloadableFileRepository: DownloadableFileRepository): SearchRepository = downloadableFileRepository

    @Provides
    @Singleton
    fun provideJobDao(db: MilouDatabase): JobDao = db.jobDao()

    @Provides
    @Singleton
    fun provideJobManager(jobManagerImpl: JobManagerImpl): JobManager = jobManagerImpl

    @Provides
    @Singleton
    fun provideGameLauncher(emulatorLauncherImpl: EmulatorLauncherImpl): com.santiifm.milou.domain.launcher.GameLauncher = emulatorLauncherImpl

    @Provides
    @Singleton
    fun provideMetadataProviders(
        igdb: com.santiifm.milou.data.service.scraper.IGDBProvider,
        screenScraper: com.santiifm.milou.data.service.scraper.ScreenScraperProvider
    ): List<com.santiifm.milou.domain.scraper.MetadataProvider> = listOf(screenScraper, igdb)
}

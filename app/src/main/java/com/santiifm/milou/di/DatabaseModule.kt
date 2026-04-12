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
            .addMigrations(MilouDatabase.MIGRATION_1_2)
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
    fun provideArchiveExtractorService(): ArchiveExtractorService = ArchiveExtractorService()
}

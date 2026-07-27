package com.santiifm.milou.di

import com.santiifm.milou.domain.eventbus.EventBus
import com.santiifm.milou.domain.eventbus.StandardEventBus
import com.santiifm.milou.domain.repository.SearchRepository
import com.santiifm.milou.domain.usecase.SearchLibraryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.santiifm.milou.domain.usecase.DownloadGameUseCase
import com.santiifm.milou.domain.job.JobManager

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideSearchLibraryUseCase(
        searchRepository: SearchRepository
    ): SearchLibraryUseCase {
        return SearchLibraryUseCase(searchRepository)
    }

    @Provides
    @Singleton
    fun provideDownloadGameUseCase(
        jobManager: JobManager
    ): DownloadGameUseCase {
        return DownloadGameUseCase(jobManager)
    }

    @Provides
    @Singleton
    fun provideLaunchGameUseCase(
        launcher: com.santiifm.milou.domain.launcher.GameLauncher,
        eventBus: EventBus
    ): com.santiifm.milou.domain.usecase.LaunchGameUseCase {
        return com.santiifm.milou.domain.usecase.LaunchGameUseCase(launcher, eventBus)
    }

    @Provides
    @Singleton
    fun provideScrapeMetadataUseCase(
        providers: List<com.santiifm.milou.domain.scraper.MetadataProvider>,
        eventBus: EventBus
    ): com.santiifm.milou.domain.usecase.ScrapeMetadataUseCase {
        return com.santiifm.milou.domain.usecase.ScrapeMetadataUseCase(providers, eventBus)
    }

    @Provides
    @Singleton
    fun provideEventBus(): EventBus {
        return StandardEventBus()
    }
}

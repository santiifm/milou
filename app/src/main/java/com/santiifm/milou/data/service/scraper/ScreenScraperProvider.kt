package com.santiifm.milou.data.service.scraper

import com.santiifm.milou.domain.model.GameMetadata
import com.santiifm.milou.domain.model.MetadataSource
import com.santiifm.milou.domain.scraper.MetadataProvider
import com.santiifm.milou.domain.scraper.ScrapeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenScraperProvider @Inject constructor() : MetadataProvider {
    override val name: String = "ScreenScraper"
    override val priority: Int = 1 // Hash match is primary

    override suspend fun fetchByHash(hash: String, consoleId: String): Result<ScrapeResult> {
        // Placeholder for HTTP call to ScreenScraper API
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun searchByName(name: String, consoleId: String): Result<List<ScrapeResult>> {
        // Placeholder for ScreenScraper search
        return Result.success(emptyList())
    }
}

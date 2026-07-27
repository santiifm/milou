package com.santiifm.milou.data.service.scraper

import com.santiifm.milou.domain.model.GameMetadata
import com.santiifm.milou.domain.model.MetadataSource
import com.santiifm.milou.domain.scraper.MetadataProvider
import com.santiifm.milou.domain.scraper.ScrapeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IGDBProvider @Inject constructor() : MetadataProvider {
    override val name: String = "IGDB"
    override val priority: Int = 2 // Name match is secondary

    override suspend fun fetchByHash(hash: String, consoleId: String): Result<ScrapeResult> {
        // IGDB does not support direct ROM hash matching
        return Result.failure(Exception("IGDB does not support hash matching"))
    }

    override suspend fun searchByName(name: String, consoleId: String): Result<List<ScrapeResult>> {
        // Placeholder for Retrofit call to IGDB API
        return Result.success(emptyList())
    }
}

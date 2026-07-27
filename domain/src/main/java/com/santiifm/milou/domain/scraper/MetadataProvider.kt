package com.santiifm.milou.domain.scraper

import com.santiifm.milou.domain.model.GameMetadata

data class ScrapeResult(
    val metadata: GameMetadata,
    val confidence: Float
)

interface MetadataProvider {
    val name: String
    val priority: Int
    suspend fun fetchByHash(hash: String, consoleId: String): Result<ScrapeResult>
    suspend fun searchByName(name: String, consoleId: String): Result<List<ScrapeResult>>
}

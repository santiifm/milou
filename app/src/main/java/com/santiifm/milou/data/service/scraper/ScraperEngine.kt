package com.santiifm.milou.data.service.scraper

import com.santiifm.milou.domain.model.Game
import com.santiifm.milou.domain.model.GameMetadata
import com.santiifm.milou.domain.scraper.MetadataProvider
import com.santiifm.milou.domain.scraper.ScrapeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperEngine @Inject constructor(
    private val providers: List<MetadataProvider>
) {
    suspend fun scrape(game: Game, romHash: String? = null): Result<GameMetadata> {
        val sortedProviders = providers.sortedBy { it.priority }

        if (romHash != null) {
            for (provider in sortedProviders) {
                provider.fetchByHash(romHash, game.consoleId).onSuccess { result ->
                    if (result.confidence > 0.9f) return Result.success(result.metadata)
                }
            }
        }

        for (provider in sortedProviders) {
            provider.searchByName(game.title, game.consoleId).onSuccess { results ->
                val bestMatch = results.maxByOrNull { it.confidence }
                if (bestMatch != null && bestMatch.confidence > 0.7f) return Result.success(bestMatch.metadata)
            }
        }

        return Result.failure(Exception("No metadata found"))
    }
}

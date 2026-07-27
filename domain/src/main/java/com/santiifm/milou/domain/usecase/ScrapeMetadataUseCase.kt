package com.santiifm.milou.domain.usecase

import com.santiifm.milou.domain.event.MetadataScrapingEvent
import com.santiifm.milou.domain.eventbus.EventBus
import com.santiifm.milou.domain.model.Game
import com.santiifm.milou.domain.model.GameMetadata
import com.santiifm.milou.domain.model.MetadataSource
import com.santiifm.milou.domain.scraper.MetadataProvider
import com.santiifm.milou.domain.scraper.ScrapeResult

class ScrapeMetadataUseCase(
    private val providers: List<MetadataProvider>,
    private val eventBus: EventBus
) {
    suspend operator fun invoke(
        operationId: String,
        game: Game,
        romHash: String? = null
    ): Result<GameMetadata> {
        eventBus.publish(MetadataScrapingEvent.Started(operationId, game.title))

        val sortedProviders = providers.sortedBy { it.priority }

        // 1. Try Hash Match (High Priority)
        if (romHash != null) {
            for (provider in sortedProviders) {
                provider.fetchByHash(romHash, game.consoleId).onSuccess { result ->
                    if (result.confidence > 0.9f) {
                        eventBus.publish(MetadataScrapingEvent.Completed(operationId, game.id.toString()))
                        return Result.success(result.metadata)
                    }
                }
            }
        }

        // 2. Try Name Match (Lower Priority)
        for (provider in sortedProviders) {
            provider.searchByName(game.title, game.consoleId).onSuccess { results ->
                val bestMatch = results.maxByOrNull { it.confidence }
                if (bestMatch != null && bestMatch.confidence > 0.7f) {
                    eventBus.publish(MetadataScrapingEvent.Completed(operationId, game.id.toString()))
                    return Result.success(bestMatch.metadata)
                }
            }
        }

        val error = "No suitable metadata found for ${game.title}"
        eventBus.publish(MetadataScrapingEvent.Failed(operationId, error))
        return Result.failure(Exception(error))
    }
}

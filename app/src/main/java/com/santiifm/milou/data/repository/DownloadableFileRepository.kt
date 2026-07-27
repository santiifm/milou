package com.santiifm.milou.data.repository

import com.santiifm.milou.data.local.dao.ConsoleWithFileCount
import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.CategorizedTags
import com.santiifm.milou.data.model.DownloadableFileWithTags
import com.santiifm.milou.data.model.TagCategorizer
import com.santiifm.milou.domain.model.FilterMode
import com.santiifm.milou.domain.model.Game
import com.santiifm.milou.domain.model.SearchCriteria
import com.santiifm.milou.domain.model.SortOrder
import com.santiifm.milou.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadableFileRepository @Inject constructor(
    private val dao: DownloadableFileDao
) : SearchRepository {
    override suspend fun search(
        criteria: SearchCriteria,
        limit: Int,
        offset: Int
    ): List<Game> {
        val downloadableFilesWithTags = searchFilesWithTags(
            query = criteria.query,
            consoleIds = criteria.consoles,
            tags = criteria.tags,
            matchAllTags = criteria.filterMode == FilterMode.AND,
            sortAsc = criteria.sortOrder == SortOrder.ASC,
            limit = limit,
            offset = offset
        )
        return downloadableFilesWithTags.map { it.toDomain() }
    }

    private fun DownloadableFileWithTags.toDomain(): Game {
        return Game(
            id = file.id.toString(),
            title = file.name,
            consoleId = file.consoleId,
            tags = buildList {
                addAll(tags)
                if (file.fileExtension.isNotEmpty()) {
                    add(file.fileExtension.uppercase())
                }
            },
            // Favorites and installed status would be added here in future phases
            isFavorite = false,
            isInstalled = false,
            artworkPath = null,
            fileSize = file.fileSize
        )
    }

    suspend fun searchFilesWithTags(
        query: String,
        manufacturer: String? = null,
        consoleIds: Set<String> = emptySet(),
        tags: Set<String> = emptySet(),
        matchAllTags: Boolean = false,
        sortAsc: Boolean = true,
        limit: Int = 100,
        offset: Int = 0
    ): List<DownloadableFileWithTags> {
        val results = dao.queryFilesWithTags(
            query = query.ifBlank { "*" },
            manufacturer = manufacturer,
            consoleIds = consoleIds.toList(),
            consoleIdsCount = consoleIds.size,
            tags = tags.toList(),
            tagsCount = tags.size,
            matchAllTags = matchAllTags,
            sortAsc = sortAsc,
            limit = limit,
            offset = offset
        )
        return results.map { result ->
            DownloadableFileWithTags(
                file = DownloadableFileEntity(
                    id = result.id,
                    name = result.name,
                    fileName = result.fileName,
                    consoleId = result.consoleId,
                    downloadUrl = result.downloadUrl,
                    fileSize = result.fileSize,
                    fileExtension = result.fileExtension,
                    torrentFileIndex = result.torrentFileIndex,
                    torrentMagnet = result.torrentMagnet
                ),
                tags = result.tags?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            )
        }
    }

    suspend fun clearAll() = dao.clearAll()

    suspend fun getAvailableTags(
        query: String,
        manufacturer: String? = null,
        consoleIds: Set<String> = emptySet()
    ): List<String> =
        dao.getAvailableTags(query.ifBlank { "*" }, manufacturer, consoleIds.toList(), consoleIds.size)

    suspend fun getCategorizedTags(
        query: String,
        manufacturer: String? = null,
        consoleIds: Set<String> = emptySet()
    ): CategorizedTags =
        TagCategorizer.categorizeTags(
            dao.getAvailableTags(query.ifBlank { "*" }, manufacturer, consoleIds.toList(), consoleIds.size)
        )

    suspend fun getConsolesWithFiles(query: String, manufacturer: String? = null): List<ConsoleWithFileCount> =
        dao.getConsolesWithFiles(query.ifBlank { "*" }, manufacturer)
}

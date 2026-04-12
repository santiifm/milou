package com.santiifm.milou.data.repository

import com.santiifm.milou.data.local.dao.ConsoleWithFileCount
import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.CategorizedTags
import com.santiifm.milou.data.model.DownloadableFileWithTags
import com.santiifm.milou.data.model.TagCategorizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadableFileRepository @Inject constructor(
    private val dao: DownloadableFileDao
) {
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

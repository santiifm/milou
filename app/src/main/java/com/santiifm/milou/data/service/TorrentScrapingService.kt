package com.santiifm.milou.data.service

import android.util.Log
import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.local.entity.FileTagEntity
import com.santiifm.milou.data.model.Console
import com.santiifm.milou.data.model.UrlEntry
import com.santiifm.milou.data.state.RescanStateHolder
import com.santiifm.milou.util.FileParsingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torrent equivalent of [DatabaseScrapingService].
 *
 * 1. Calls [TorrentMetadataFetcher] to get (or reuse) the TorrentInfo.
 * 2. Calls [TorrentFileIndexer] to list all files, filtered by [UrlEntry.folders] if specified.
 * 3. Maps each file to a [DownloadableFileEntity] with torrentFileIndex +
 *    torrentMagnet filled in, then batch-inserts — same pattern as HTTP scraper.
 * 4. Runs [FileParsingUtils.extractNameAndTags] on each file name so region /
 *    language / version tags work the same as for HTTP sources.
 */
@Singleton
class TorrentScrapingService @Inject constructor(
    private val metadataFetcher: TorrentMetadataFetcher,
    private val fileIndexer: TorrentFileIndexer,
    private val downloadableFileDao: DownloadableFileDao,
    private val rescanStateHolder: RescanStateHolder,
    private val registry: TorrentHandleRegistry
) {

    suspend fun scrapeAndInsert(urlEntry: UrlEntry, console: Console): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            val magnet = urlEntry.url

            rescanStateHolder.setTorrentFetchProgress("Fetching torrent metadata for ${console.name}…")

            val info = try {
                metadataFetcher.fetch(magnet)
            } catch (e: Exception) {
                rescanStateHolder.setTorrentFetchProgress("")
                throw e
            }

            rescanStateHolder.setTorrentFetchProgress("Indexing files for ${console.name}…")

            val entries = fileIndexer.index(info, magnet, urlEntry.folders)
            if (entries.isEmpty()) {
                Log.w(TAG, "No files found in torrent")
                rescanStateHolder.setTorrentFetchProgress("")
                return@withContext Pair(0, 0)
            }

            val allFiles = mutableListOf<DownloadableFileEntity>()
            val allTagPairs = mutableListOf<Pair<DownloadableFileEntity, List<FileTagEntity>>>()

            entries.forEach { entry ->
                val (cleanName, tagStrings) = FileParsingUtils.extractNameAndTags(entry.fileName)
                val entity = DownloadableFileEntity(
                    id = 0,
                    name = cleanName,
                    fileName = entry.fileName,
                    consoleId = console.id,
                    downloadUrl = magnet,
                    fileSize = entry.fileSize,
                    fileExtension = entry.fileName.substringAfterLast('.', ""),
                    torrentFileIndex = entry.fileIndex,
                    torrentMagnet = magnet
                )
                val tagEntities = tagStrings.map { FileTagEntity(fileId = 0, tag = it) }
                val contentTypeTag = FileTagEntity(
                    fileId = 0,
                    tag = FileParsingUtils.normalizeTag(urlEntry.contentType.name)
                )
                allFiles.add(entity)
                allTagPairs.add(entity to (tagEntities + contentTypeTag))
            }

            val insertedIds = downloadableFileDao.insertAll(allFiles)
            val fileIdMap = allFiles.zip(insertedIds).associate { (f, id) -> f.fileName to id }

            val updatedTags = allTagPairs.flatMap { (file, tags) ->
                val id = fileIdMap[file.fileName] ?: 0L
                tags.map { it.copy(fileId = id) }
            }
            downloadableFileDao.insertTags(updatedTags)
            rescanStateHolder.setTorrentFetchProgress("")

            Log.i(TAG, "Inserted ${allFiles.size} files, ${updatedTags.size} tags for ${console.name}")
            
            // NOTE: We no longer release the handle here.
            // Keeping it in the session allows immediate starting of downloads.

            Pair(allFiles.size, updatedTags.size)
        }

    companion object { private const val TAG = "TorrentScrapingService" }
}

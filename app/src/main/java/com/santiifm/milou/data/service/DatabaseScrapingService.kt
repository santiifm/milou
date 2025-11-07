package com.santiifm.milou.data.service

import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.local.entity.FileTagEntity
import com.santiifm.milou.data.model.Manufacturer
import com.santiifm.milou.util.FileParsingUtils
import com.santiifm.milou.util.HttpHeadersUtils
import com.santiifm.milou.util.ScrapingConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseScrapingService @Inject constructor(
    private val downloadableFileDao: DownloadableFileDao
) {
    
    private suspend fun makeRequest(url: String): org.jsoup.nodes.Document {
        var lastException: Exception? = null
        
        repeat(ScrapingConstants.MAX_RETRIES) { attempt ->
            try {
                if (attempt > 0) {
                    delay(ScrapingConstants.RETRY_DELAY_MS * attempt)
                }
                
                val connection = Jsoup.connect(url)
                    .userAgent(ScrapingConstants.USER_AGENT)
                    .timeout(ScrapingConstants.CONNECTION_TIMEOUT_MS.toInt())
                
                HttpHeadersUtils.configureBrowserHeaders(connection)
                
                return connection.get()
            } catch (e: Exception) {
                lastException = e
                println("Request attempt ${attempt + 1} failed for $url: ${e.message}")
                if (attempt < ScrapingConstants.MAX_RETRIES - 1) {
                    delay(ScrapingConstants.RETRY_DELAY_MS)
                }
            }
        }
        
        throw lastException ?: Exception("All retry attempts failed")
    }

    suspend fun scrapeAndInsertToDatabase(
        baseUrl: String,
        consoleId: String,
        contentType: com.santiifm.milou.data.model.ContentType = com.santiifm.milou.data.model.ContentType.GAME,
        visitedUrls: MutableSet<String> = mutableSetOf(),
        rootUrl: String = baseUrl
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<DownloadableFileEntity>()
        val allTags = mutableListOf<Pair<DownloadableFileEntity, List<FileTagEntity>>>()
        
        if (visitedUrls.contains(baseUrl)) {
            return@withContext Pair(0, 0)
        }
        
        if (!baseUrl.startsWith(rootUrl)) {
            return@withContext Pair(0, 0)
        }
        
        visitedUrls.add(baseUrl)
        
        try {
            
            delay(ScrapingConstants.REQUEST_DELAY_MS)
            
            val doc = makeRequest(baseUrl)
            
            val table = doc.select(ScrapingConstants.TABLE_SELECTOR).first()
            if (table == null) {
                return@withContext Pair(0, 0)
            }
            
            val rows = table.select("tr")
            
            for (row in rows) {
                val linkCell = row.select("td.link a").first()

                if (linkCell == null) continue
                
                val href = linkCell.attr("href")
                val linkText = linkCell.text().trim()

                if (href == ScrapingConstants.PARENT_DIRECTORY || href == ScrapingConstants.CURRENT_DIRECTORY ||
                    linkText == "Parent directory/" || linkText == "./" || linkText == "../") {
                    continue
                }
                
                if (href.endsWith("/")) {
                    val subUrl = FileParsingUtils.buildDownloadUrl(baseUrl, href)
                    
                    if (subUrl.contains("/../") || subUrl.endsWith("/..")) {
                        continue
                    }
                    
                    if (visitedUrls.contains(subUrl)) {
                        continue
                    }
                    
                    delay(ScrapingConstants.REQUEST_DELAY_MS)
                    continue
                }
                
                val (fileEntity, tagEntities) = FileParsingUtils.parseFileFromRow(row, baseUrl, consoleId)
                
                if (fileEntity != null) {
                    val contentTypeTag = FileTagEntity(
                        fileId = fileEntity.id,
                        tag = FileParsingUtils.normalizeTag(contentType.name)
                    )
                    val allTagEntities = tagEntities + contentTypeTag

                    allFiles.add(fileEntity)
                    allTags.add(Pair(fileEntity, allTagEntities))
                }
            }
            
            if (allFiles.isNotEmpty()) {
                val fileIds = downloadableFileDao.insertAll(allFiles)
                
                val fileIdMap = allFiles.zip(fileIds).associate { (file, id) -> "${file.fileName}|${file.downloadUrl}" to id }
                
                val updatedTags = allTags.flatMap { (file, tags) ->
                    val fileId = fileIdMap["${file.fileName}|${file.downloadUrl}"] ?: 0L
                    tags.map { tag -> tag.copy(fileId = fileId) }
                }
                
                downloadableFileDao.insertTags(updatedTags)
            }
            
            Pair(allFiles.size, allTags.sumOf { it.second.size })
            
        } catch (e: Exception) {
            println("Error scraping $baseUrl recursively: ${e.message}")
            Pair(0, 0)
        }
    }
    
    suspend fun scrapeManufacturer(manufacturer: Manufacturer): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var totalFiles = 0
        var totalTags = 0
        
        manufacturer.consoles.forEach { console ->
            console.urls.forEach { urlEntry ->
                val (files, tags) = scrapeAndInsertToDatabase(urlEntry.url, console.id, urlEntry.contentType)
                totalFiles += files
                totalTags += tags
            }
        }
        
        Pair(totalFiles, totalTags)
    }
    
    suspend fun clearAllData() {
        downloadableFileDao.clearAll()
    }
}

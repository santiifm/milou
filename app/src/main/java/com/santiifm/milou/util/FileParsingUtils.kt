package com.santiifm.milou.util

import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.local.entity.FileTagEntity
import org.jsoup.nodes.Element
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object FileParsingUtils {
    
    fun buildDownloadUrl(baseUrl: String, href: String): String {
        return if (href.startsWith("http")) {
            href
        } else {
            val base = baseUrl.trimEnd('/')
            val path = href.trimStart('/')
            val fullUrl = "$base/$path"
            
            normalizeUrl(fullUrl)
        }
    }
    
    private fun normalizeUrl(url: String): String {
        val parts = url.split("/")
        val normalized = mutableListOf<String>()
        
        for (part in parts) {
            when (part) {
                ".." -> {
                    if (normalized.isNotEmpty() && normalized.last() != "") {
                        normalized.removeAt(normalized.size - 1)
                    }
                }
                "." -> {
                    continue
                }
                else -> {
                    normalized.add(part)
                }
            }
        }
        
        return normalized.joinToString("/")
    }
    
    fun extractNameAndTags(displayName: String): Pair<String, List<String>> {
        val tags = mutableListOf<String>()
        var cleanName = displayName
        
        val tagPattern = Regex("\\(([^)]+)\\)")
        val matches = tagPattern.findAll(displayName)
        
        for (match in matches) {
            val content = match.groupValues[1].trim()
            
            // Split by comma to handle multiple values in parentheses
            val values = content.split(",").map { it.trim() }
            
            for (value in values) {
                if (isValidTag(value)) {
                    tags.add(normalizeTag(value))
                }
            }
            
            // Remove the entire parentheses group from the name
            cleanName = cleanName.replace(match.value, "").trim()
        }
        
        // Clean up any extra spaces that might be left
        cleanName = cleanName.replace(Regex("\\s+"), " ").trim()
        
        return Pair(cleanName, tags)
    }
    
    fun isValidTag(tag: String): Boolean {
        if (tag.length < 2) return false

        val cleanTag = tag.trim().uppercase()

        if (cleanTag in Constants.Tags.VIDEO_STANDARDS) return true

        if (cleanTag in Constants.Tags.CONTENT_TYPES) return true

        if (cleanTag.matches(Constants.Tags.LANGUAGE_CODE_PATTERN)) return true

        if (cleanTag.matches(Constants.Tags.VERSION_PATTERN)) return true

        if (cleanTag in Constants.Tags.ALL_REGIONS) return true

        for (matcher in Constants.Tags.PARTIAL_MATCHERS) {
            if (cleanTag.contains(matcher)) return true
        }

        return false
    }

    fun normalizeTag(tag: String): String {
        val trimmed = tag.trim()
        val upper = trimmed.uppercase()

        if (upper in Constants.Tags.VIDEO_STANDARDS) {
            return upper
        }

        if (trimmed.length <= 3) {
            return upper
        }

        return trimmed.lowercase().replaceFirstChar { it.uppercase() }
    }
    
    fun decodeUrlEncodedFileName(fileName: String): String {
        return try {
            URLDecoder.decode(fileName, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            fileName
        }
    }
    
    fun parseFileFromRow(row: Element, baseUrl: String, consoleId: String): Pair<DownloadableFileEntity?, List<FileTagEntity>> {
        val linkCell = row.select(ScrapingConstants.LINK_CELL_SELECTOR).first()
        val sizeCell = row.select(ScrapingConstants.SIZE_CELL_SELECTOR).first()
        
        if (linkCell == null) return Pair(null, emptyList())
        
        val href = linkCell.attr("href")
        val title = linkCell.attr("title")
        val linkText = linkCell.text()
        
        if (shouldSkipFile(href)) return Pair(null, emptyList())
        
        val fileName = href
        val fileSize = sizeCell?.text()?.takeIf { it != ScrapingConstants.UNKNOWN_FILE_SIZE } ?: ScrapingConstants.DEFAULT_FILE_SIZE
        val downloadUrl = buildDownloadUrl(baseUrl, href)
        
        val actualFileExtension = if (fileName.contains(".")) {
            "." + fileName.substringAfterLast(".")
        } else {
            ""
        }
        
        val displayName = title.ifEmpty { linkText }
        val (cleanName, tags) = extractNameAndTags(displayName)
        
        val fileEntity = DownloadableFileEntity(
            name = cleanName,
            fileName = fileName,
            consoleId = consoleId,
            downloadUrl = downloadUrl,
            fileSize = FileSizeUtils.parseFileSize(fileSize),
            fileExtension = actualFileExtension
        )
        
        val tagEntities = tags.map { tag ->
            FileTagEntity(
                fileId = 0L, // Will be set after file insertion
                tag = tag
            )
        }
        
        return Pair(fileEntity, tagEntities)
    }
    
    private fun shouldSkipFile(href: String): Boolean {
        return href == ScrapingConstants.PARENT_DIRECTORY || 
               href == ScrapingConstants.CURRENT_DIRECTORY || 
               href.endsWith("/") && !href.contains(".") ||
               href.endsWith("/")
    }
    
}

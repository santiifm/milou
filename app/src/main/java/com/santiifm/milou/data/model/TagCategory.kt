package com.santiifm.milou.data.model

import com.santiifm.milou.util.Constants

data class TagCategory(
    val name: String,
    val tags: List<String>
)

data class CategorizedTags(
    val regions: TagCategory,
    val languages: TagCategory,
    val videoStandards: TagCategory,
    val contentTypes: TagCategory,
    val fileTypes: TagCategory
)

object TagCategorizer {
    
    fun categorizeTags(tags: List<String>): CategorizedTags {
        val regions = mutableListOf<String>()
        val languages = mutableListOf<String>()
        val videoStandards = mutableListOf<String>()
        val contentTypes = mutableListOf<String>()
        val fileTypes = mutableListOf<String>()
        
        tags.forEach { tag ->
            val cleanTag = tag.trim().uppercase()

            when {
                isRegion(cleanTag) -> regions.add(tag)
                isVideoStandard(cleanTag) -> videoStandards.add(tag)
                isContentType(cleanTag) -> contentTypes.add(tag)
                isLanguage(cleanTag) -> languages.add(tag)
                isFileType(cleanTag) -> fileTypes.add(tag)
            }
        }
        
        return CategorizedTags(
            regions = TagCategory("Regions", regions.sorted()),
            languages = TagCategory("Languages", languages.sorted()),
            videoStandards = TagCategory("Video Standards", videoStandards.sorted()),
            contentTypes = TagCategory("Content Types", contentTypes.sorted()),
            fileTypes = TagCategory("File Types", fileTypes.sorted())
        )
    }
    
    private fun isRegion(tag: String): Boolean {
        return tag in Constants.Tags.ALL_REGIONS
    }
    
    private fun isLanguage(tag: String): Boolean {
        return tag.matches(Regex("^[A-Z]{2,3}$"))
    }
    
    private fun isVideoStandard(tag: String): Boolean {
        return tag in Constants.Tags.VIDEO_STANDARDS
    }
    
    private fun isContentType(tag: String): Boolean {
        return tag in Constants.Tags.CONTENT_TYPES
    }
    
    private fun isFileType(tag: String): Boolean {
        return tag.matches(Constants.Tags.FILE_EXTENSION_PATTERN)
    }
}

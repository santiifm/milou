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
            when {
                isVideoStandard(tag) -> videoStandards.add(tag)
                isContentType(tag) -> contentTypes.add(tag)
                isRegion(tag) -> regions.add(tag)
                isLanguage(tag) -> languages.add(tag)
                isFileType(tag) -> fileTypes.add(tag)
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
        return tag.uppercase() in Constants.Tags.ALL_REGIONS
    }

    private fun isLanguage(tag: String): Boolean {
        return tag.uppercase().matches(Regex("^[A-Z]{2,3}$"))
    }

    private fun isVideoStandard(tag: String): Boolean {
        return tag.uppercase() in Constants.Tags.VIDEO_STANDARDS
    }

    private fun isContentType(tag: String): Boolean {
        return tag.uppercase() in Constants.Tags.CONTENT_TYPES
    }

    private fun isFileType(tag: String): Boolean {
        return tag.uppercase().matches(Constants.Tags.FILE_EXTENSION_PATTERN)
    }
}

package com.santiifm.milou.util

object ArchiveUtils {

    private val SUPPORTED_EXTENSIONS = setOf(
        ".zip", ".7z", ".rar", ".tar", ".gz", ".tgz", ".bz2", ".xz", ".cab", ".iso"
    )

    fun isExtractable(fileExtension: String): Boolean {
        if (fileExtension.isBlank()) return false
        val ext = if (fileExtension.startsWith(".")) fileExtension.lowercase() else ".${fileExtension.lowercase()}"
        return SUPPORTED_EXTENSIONS.contains(ext)
    }
}

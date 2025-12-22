package com.santiifm.milou.util

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream

object StorageHelper {

    fun getDocumentFile(context: Context, uriString: String): DocumentFile? {
        return try {
            val uri = uriString.toUri()
            DocumentFile.fromTreeUri(context, uri)
        } catch (_: Exception) {
            null
        }
    }

    fun createDirectory(context: Context, uriString: String, subPath: String): DocumentFile? {
        val baseDocument = getDocumentFile(context, uriString) ?: return null

        if (subPath.isEmpty()) return baseDocument

        val pathParts = subPath.split("/").filter { it.isNotEmpty() }
        var currentDir = baseDocument

        for (part in pathParts) {
            val existingDir = currentDir.listFiles().find { it.isDirectory && it.name.equals(part, ignoreCase = true) }
            currentDir = if (existingDir != null) {
                existingDir
            } else {
                currentDir.createDirectory(part) ?: return null
            }
        }

        return currentDir
    }

    fun createFile(
        context: Context,
        uriString: String,
        subPath: String,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): DocumentFile? {
        val directory = createDirectory(context, uriString, subPath) ?: return null
        return directory.createFile(mimeType, fileName)
    }

    fun getOutputStream(context: Context, documentFile: DocumentFile): OutputStream? {
        return try {
            context.contentResolver.openOutputStream(documentFile.uri)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFile(documentFile: DocumentFile?): Boolean {
        return documentFile?.delete() == true
    }

    fun isValidUri(context: Context, uriString: String): Boolean {
        if (uriString.isEmpty()) return false
        val documentFile = getDocumentFile(context, uriString)
        return documentFile != null && documentFile.exists() && documentFile.canWrite()
    }
}

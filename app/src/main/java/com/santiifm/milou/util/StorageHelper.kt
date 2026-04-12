package com.santiifm.milou.util

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream

object StorageHelper {
    private const val TAG = "StorageHelper"

    fun getDocumentFile(context: Context, uriString: String): DocumentFile? {
        return try {
            val uri = uriString.toUri()
            if (uri.scheme == "content") {
                if (DocumentsContract.isTreeUri(uri)) {
                    DocumentFile.fromTreeUri(context, uri)
                } else {
                    DocumentFile.fromSingleUri(context, uri)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting document file for $uriString: ${e.message}")
            null
        }
    }

    fun createDirectory(context: Context, uriString: String, subPath: String): DocumentFile? {
        val baseDocument = getDocumentFile(context, uriString) ?: run {
            Log.e(TAG, "Could not get base document for URI: $uriString")
            return null
        }
        
        try {
            if (!baseDocument.exists()) {
                Log.e(TAG, "Base directory does not exist: ${baseDocument.uri}")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking existence of base directory: ${e.message}")
            return null
        }

        // Extra check: try to read the directory to ensure it's physically present
        try {
            if (!baseDocument.canRead()) {
                Log.e(TAG, "Base directory is inaccessible (might be physically deleted): ${baseDocument.uri}")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Base directory is inaccessible (might be physically deleted): ${baseDocument.uri}. Error: ${e.message}")
            return null
        }

        val pathParts = subPath.split("/").filter { it.isNotEmpty() }
        var currentDir = baseDocument

        for (part in pathParts) {
            val existingDir = try {
                currentDir.findFile(part)
            } catch (e: Exception) {
                Log.w(TAG, "Error finding sub-directory '$part' in '${currentDir.uri}': ${e.message}")
                null
            }

            currentDir = if (existingDir != null && existingDir.isDirectory) {
                existingDir
            } else {
                try {
                    currentDir.createDirectory(part) ?: run {
                        Log.e(TAG, "Failed to create sub-directory '$part' in '${currentDir.uri}' (returned null)")
                        return null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception creating sub-directory '$part' in '${currentDir.uri}': ${e.message}")
                    return null
                }
            }
        }
        return currentDir
    }

    fun createFile(
        context: Context,
        uriString: String,
        subPath: String,
        fileName: String,
        mimeType: String = "application/octet-stream",
        overwrite: Boolean = true
    ): DocumentFile? {
        val directory = createDirectory(context, uriString, subPath) ?: run {
            Log.e(TAG, "Failed to create/access directory for $fileName in $uriString")
            return null
        }
        if (overwrite) {
            try {
                directory.findFile(fileName)?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting existing file $fileName: ${e.message}")
            }
        } else {
            try {
                val existing = directory.findFile(fileName)
                if (existing != null) return existing
            } catch (e: Exception) {
                Log.w(TAG, "Error checking for existing file $fileName: ${e.message}")
            }
        }
        return try {
            directory.createFile(mimeType, fileName) ?: run {
                Log.e(TAG, "Failed to create file $fileName in ${directory.uri} (returned null)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating file $fileName in ${directory.uri}: ${e.message}")
            null
        }
    }

    fun getOutputStream(context: Context, documentFile: DocumentFile): OutputStream? {
        return try {
            context.contentResolver.openOutputStream(documentFile.uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening output stream: ${e.message}")
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

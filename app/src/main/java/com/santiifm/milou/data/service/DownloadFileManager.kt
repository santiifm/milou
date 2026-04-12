package com.santiifm.milou.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.model.DownloadItemModel
import com.santiifm.milou.data.model.DownloadStatus
import com.santiifm.milou.data.repository.ConsoleRepository
import com.santiifm.milou.data.repository.SettingsRepository
import com.santiifm.milou.util.ConsoleFormatter
import com.santiifm.milou.util.FileParsingUtils
import com.santiifm.milou.util.StorageHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val consoleRepository: ConsoleRepository
) {

    fun createDownloadItem(file: DownloadableFileEntity): DownloadItemModel {
        return DownloadItemModel(
            name = file.name,
            fileName = file.fileName,
            downloadSpeed = 0f,
            progress = 0f,
            status = DownloadStatus.DOWNLOADING,
            downloadedBytes = 0L,
            fileSize = file.fileSize
        )
    }

    fun createDocumentFile(
        file: DownloadableFileEntity,
        downloadDirectoryUri: String,
        subPath: String
    ): DocumentFile? {
        val decodedFileName = FileParsingUtils.decodeUrlEncodedFileName(file.fileName)
        return StorageHelper.createFile(
            context = context,
            uriString = downloadDirectoryUri,
            subPath = subPath,
            fileName = decodedFileName,
            mimeType = "application/octet-stream",
            overwrite = true
        )
    }

    fun getOutputStream(documentFile: DocumentFile): java.io.OutputStream? {
        return StorageHelper.getOutputStream(context, documentFile)
    }

    suspend fun deleteFile(documentFile: DocumentFile): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                StorageHelper.deleteFile(documentFile)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun deleteFileByName(
        file: DownloadableFileEntity,
        deleteFile: Boolean = false,
        extractedFiles: List<String> = emptyList()
    ): Boolean {
        if (!deleteFile) return true

        return try {
            val downloadDirectoryUri = getDownloadDirectoryUri(file)
            val subPath = getSubPath(file)
            val decodedFileName = FileParsingUtils.decodeUrlEncodedFileName(file.fileName)

            val directory = StorageHelper.createDirectory(
                context = context,
                uriString = downloadDirectoryUri.toString(),
                subPath = subPath
            ) ?: return false

            if (extractedFiles.isNotEmpty()) {
                var deletedAny = false
                extractedFiles.forEach { extractedFileName ->
                    val fileToDelete = directory.findFile(extractedFileName)
                    if (fileToDelete?.delete() == true) {
                        deletedAny = true
                    }
                }
                return deletedAny
            }

            val archiveFile = directory.findFile(decodedFileName)
            if (archiveFile != null && archiveFile.exists()) {
                return archiveFile.delete()
            }

            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getDownloadDirectoryUri(file: DownloadableFileEntity): Uri {
        val uriString = settingsRepository.consoleDownloadDirectories.first()[file.consoleId]
            ?: settingsRepository.downloadDirectory.first()

        if (uriString.isEmpty()) return Uri.EMPTY

        val uri = uriString.toUri()
        try {
            val df = DocumentFile.fromTreeUri(context, uri)
            if (df == null || !df.exists() || !df.canWrite()) {
                Log.e("DownloadFileManager", "Tree URI is no longer valid: $uriString")
                return Uri.EMPTY
            }
        } catch (e: Exception) {
            Log.e("DownloadFileManager", "Error validating Tree URI: ${e.message}")
            return Uri.EMPTY
        }

        return uri
    }

    suspend fun getSubPath(file: DownloadableFileEntity): String {
        val consoleDownloadDirs = settingsRepository.consoleDownloadDirectories.first()
        if (consoleDownloadDirs.containsKey(file.consoleId)) return ""

        val separateByConsole = settingsRepository.separateByConsole.first()
        if (!separateByConsole) return ""

        val console = consoleRepository.getConsoleById(file.consoleId)
        return if (console != null) {
            FileParsingUtils.sanitizeFolderName(ConsoleFormatter.getConsoleFolderName(console.id))
        } else {
            "Unknown"
        }
    }
}

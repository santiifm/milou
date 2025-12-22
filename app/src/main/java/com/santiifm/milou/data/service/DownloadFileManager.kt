package com.santiifm.milou.data.service

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.flow.first
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
            fileName = file.fileName, // Keep original for tracking
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
            mimeType = "application/octet-stream"
        )
    }

    fun getOutputStream(documentFile: DocumentFile): java.io.OutputStream? {
        return StorageHelper.getOutputStream(context, documentFile)
    }

    fun deleteFile(documentFile: DocumentFile): Boolean {
        return try {
            StorageHelper.deleteFile(documentFile)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteFileByName(file: DownloadableFileEntity, deleteFile: Boolean = false): Boolean {
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

            // If we have extracted files list, delete those specific files
            if (file.extractedFiles.isNotEmpty()) {
                var deletedAny = false
                file.extractedFiles.forEach { extractedFileName ->
                    val fileToDelete = directory.findFile(extractedFileName)
                    if (fileToDelete?.delete() == true) {
                        deletedAny = true
                    }
                }
                return deletedAny
            }

            // Fallback: try to delete the original archive file
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
        val consoleDownloadDirs = settingsRepository.consoleDownloadDirectories.first()
        val customPath = consoleDownloadDirs[file.consoleId]
        return customPath?.toUri() ?: settingsRepository.downloadDirectory.first().toUri()
    }

    suspend fun getSubPath(file: DownloadableFileEntity): String {
        val consoleDownloadDirs = settingsRepository.consoleDownloadDirectories.first()
        if (consoleDownloadDirs.containsKey(file.consoleId)) {
            return ""
        }

        val separateByConsole = settingsRepository.separateByConsole.first()

        if (!separateByConsole) return ""

        val consoles = consoleRepository.getAllConsoles().first()
        val console = consoles.find { it.id == file.consoleId }
        return if (console != null) {
            FileParsingUtils.sanitizeFolderName(ConsoleFormatter.getConsoleFolderName(console.id))
        } else {
            "Unknown"
        }
    }
}

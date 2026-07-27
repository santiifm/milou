package com.santiifm.milou.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.santiifm.milou.domain.event.ExtractionEvent
import com.santiifm.milou.domain.eventbus.EventBus
import com.santiifm.milou.util.ArchiveExtractionUtils
import com.santiifm.milou.util.Constants
import com.santiifm.milou.util.StorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInStream
import net.sf.sevenzipjbinding.ISeekableStream
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ArchiveExtractorService"

@Singleton
class ArchiveExtractorService @Inject constructor(
    private val eventBus: EventBus
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Extracts an archive from a SAF URI.
     */
    suspend fun extractArchive(
        context: Context,
        archiveUri: Uri,
        destinationUri: Uri,
        operationId: String,
        subPath: String = ""
    ): List<String> = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, "extraction_temp/${System.currentTimeMillis()}")
        extractDir.mkdirs()

        try {
            val pfd = context.contentResolver.openFileDescriptor(archiveUri, "r")
                ?: return@withContext emptyList<String>().also {
                    Log.e(TAG, "Could not open file descriptor for $archiveUri")
                    publishError(operationId, "Could not open file descriptor")
                }

            val cachedFiles = pfd.use {
                FileInputStream(it.fileDescriptor).use { fis ->
                    extractToCache(fis, extractDir, operationId)
                }
            }
            if (cachedFiles.isEmpty()) return@withContext emptyList<String>()

            copyToSaf(context, cachedFiles, destinationUri, subPath)
        } catch (e: Throwable) {
            Log.e(TAG, "Extraction failed: ${e.message}", e)
            publishError(operationId, e.message ?: "Unknown error")
            emptyList()
        } finally {
            extractDir.deleteRecursively()
        }
    }

    /**
     * Extracts an archive that is already on the local filesystem.
     */
    suspend fun extractArchiveFile(
        context: Context,
        archiveFile: File,
        destinationUri: Uri,
        operationId: String,
        subPath: String = ""
    ): List<String> = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, "extraction_temp/${System.currentTimeMillis()}")
        extractDir.mkdirs()

        try {
            val cachedFiles = FileInputStream(archiveFile).use { fis ->
                extractToCache(fis, extractDir, operationId)
            }
            if (cachedFiles.isEmpty()) return@withContext emptyList<String>()

            copyToSaf(context, cachedFiles, destinationUri, subPath)
        } catch (e: Throwable) {
            Log.e(TAG, "Extraction of ${archiveFile.name} failed: ${e.message}", e)
            publishError(operationId, e.message ?: "Unknown error")
            emptyList()
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private fun extractToCache(
        fis: FileInputStream,
        extractDir: File,
        operationId: String
    ): List<File> {
        val results = mutableListOf<File>()
        val channel = fis.channel
        val channelSize = channel.size()

        val inStream = object : IInStream {
            override fun read(data: ByteArray): Int {
                val n = channel.read(ByteBuffer.wrap(data))
                return if (n == -1) 0 else n
            }

            override fun seek(offset: Long, seekOrigin: Int): Long {
                val newPos = when (seekOrigin) {
                    ISeekableStream.SEEK_SET -> offset
                    ISeekableStream.SEEK_CUR -> channel.position() + offset
                    ISeekableStream.SEEK_END -> channelSize + offset
                    else -> throw SevenZipException("Unknown seek origin: $seekOrigin")
                }
                channel.position(newPos)
                return channel.position()
            }

            override fun close() {}
        }

        try {
            SevenZip.openInArchive(null, inStream).use { archive ->
                val total = archive.numberOfItems

                archive.extract(null, false, object : IArchiveExtractCallback {
                    var out: OutputStream? = null
                    var dest: File? = null
                    var skip = false
                    var done = 0

                    override fun getStream(index: Int, mode: ExtractAskMode): ISequentialOutStream? {
                        val isFolder = archive.getProperty(index, PropID.IS_FOLDER) as? Boolean ?: false
                        if (isFolder) { skip = true; return null }

                        val rawPath = (archive.getProperty(index, PropID.PATH) as? String) ?: "file_$index"
                        val name = rawPath.replace('\\', '/').split("/").last()
                            .replace(Regex("[<>:\"|?*\u0000]"), "_")
                            .ifBlank { "file_$index" }

                        val file = File(extractDir, name)
                        dest = file
                        val stream = BufferedOutputStream(FileOutputStream(file), Constants.EXTRACTION_BUFFER_SIZE)
                        out = stream
                        skip = false

                        return ISequentialOutStream { data -> stream.write(data); data.size }
                    }

                    override fun prepareOperation(mode: ExtractAskMode) {}

                    override fun setOperationResult(result: ExtractOperationResult) {
                        try { out?.close() } catch (_: Exception) {}
                        out = null

                        if (!skip && result == ExtractOperationResult.OK) {
                            dest?.let { results.add(it); done++ }
                            val progress = if (total > 0) done.toFloat() / total else 1f
                            scope.launch {
                                eventBus.publish(ExtractionEvent.Progress(operationId, progress))
                            }
                        } else if (!skip) {
                            Log.w(TAG, "Entry result: $result for ${dest?.name}")
                        }
                        dest = null
                        skip = false
                    }

                    override fun setCompleted(complete: Long) {}
                    override fun setTotal(total: Long) {}
                })
            }
        } catch (e: SevenZipException) {
            Log.e(TAG, "7-zip extraction error: ${e.message}")
            publishError(operationId, e.message ?: "7-zip error")
        }

        return results
    }

    private fun copyToSaf(
        context: Context,
        files: List<File>,
        destinationUri: Uri,
        subPath: String
    ): List<String> {
        val destUri = ArchiveExtractionUtils.prepareExtractionDestination(context, destinationUri, subPath)
        val copied = mutableListOf<String>()
        val buffer = ByteArray(Constants.EXTRACTION_BUFFER_SIZE)

        for (file in files) {
            val outUri = StorageHelper.createFile(
                context = context,
                uriString = destUri.toString(),
                subPath = "",
                fileName = file.name,
                overwrite = true
            )?.uri ?: continue

            context.contentResolver.openOutputStream(outUri)?.use { raw ->
                BufferedOutputStream(raw, Constants.EXTRACTION_BUFFER_SIZE).use { buffOut ->
                    file.inputStream().use { input ->
                        var n: Int
                        while (input.read(buffer).also { n = it } != -1) buffOut.write(buffer, 0, n)
                    }
                }
            }
            copied.add(file.name)
            Log.d(TAG, "Copied to SAF: ${file.name}")
        }

        return copied
    }

    private fun publishError(operationId: String, message: String) {
        scope.launch {
            eventBus.publish(ExtractionEvent.Failed(operationId, message))
        }
    }
}

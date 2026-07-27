package com.santiifm.milou.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun downloadImage(url: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "game_images/covers")
            if (!directory.exists()) directory.mkdirs()

            val file = File(directory, fileName)
            URL(url).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalImagePath(fileName: String): String? {
        val file = File(context.filesDir, "game_images/covers/$fileName")
        return if (file.exists()) file.absolutePath else null
    }
}

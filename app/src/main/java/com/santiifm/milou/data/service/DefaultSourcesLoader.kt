package com.santiifm.milou.data.service

import android.content.Context
import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.dao.ManufacturerDao
import com.santiifm.milou.data.local.entity.ConsoleEntity
import com.santiifm.milou.data.local.entity.ManufacturerEntity
import com.santiifm.milou.util.FileParsingUtils
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DefaultSourcesLoader"

@Singleton
class DefaultSourcesLoader @Inject constructor(
    private val manufacturerDao: ManufacturerDao,
    private val consoleDao: ConsoleDao
) {

    suspend fun loadDefaultSourcesToDatabase(context: Context) = withContext(Dispatchers.IO) {
        try {
            consoleDao.clearAll()
            manufacturerDao.clearAll()

            val jsonString = context.assets.open("consoles.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val manufacturers = mutableListOf<ManufacturerEntity>()
            val consoles = mutableListOf<ConsoleEntity>()

            jsonObject.keys().forEach { manufacturerName ->
                val manufacturerObj = jsonObject.getJSONObject(manufacturerName)
                manufacturers.add(ManufacturerEntity(id = manufacturerName, name = formatManufacturerName(manufacturerName)))

                manufacturerObj.keys().forEach { consoleName ->
                    val consoleObj = manufacturerObj.getJSONObject(consoleName)
                    val urlEntries = mutableListOf<JSONObject>()

                    val urlsArray = consoleObj.getJSONArray("urls")
                    for (i in 0 until urlsArray.length()) {
                        val urlObj = urlsArray.getJSONObject(i)
                        var url = urlObj.getString("url")
                        
                        if (url.startsWith("magnet:")) {
                            url = FileParsingUtils.optimizeMagnetUri(url)
                        }

                        val entry = JSONObject().apply {
                            put("url", url)
                            put("contentType", urlObj.optString("contentType", "GAME"))
                            if (urlObj.has("folders")) put("folders", urlObj.getJSONArray("folders"))
                        }
                        urlEntries.add(entry)
                    }

                    consoles.add(ConsoleEntity(
                        id = "${manufacturerName}_${consoleName}",
                        name = formatConsoleName(consoleName),
                        manufacturerId = manufacturerName,
                        urls = JSONArray(urlEntries).toString()
                    ))
                }
            }

            manufacturerDao.insertManufacturers(manufacturers)
            consoleDao.insertConsoles(consoles)
            Log.d(TAG, "Loaded ${manufacturers.size} manufacturers and ${consoles.size} consoles")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading default sources: ${e.message}", e)
        }
    }

    private fun formatManufacturerName(name: String) =
        name.split("_").joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }

    private fun formatConsoleName(name: String) =
        name.split("_").joinToString(" ") { word ->
            when (word.lowercase()) {
                "snes" -> "SNES"
                "ps1", "ps2", "ps3", "ps4", "ps5" -> word.uppercase()
                "n64" -> "N64"
                "gb", "gbc", "gba" -> word.uppercase()
                else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
}

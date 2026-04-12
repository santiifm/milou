package com.santiifm.milou.ui.screens.sources

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.dao.ManufacturerDao
import com.santiifm.milou.data.local.entity.ConsoleEntity
import com.santiifm.milou.data.local.entity.ManufacturerEntity
import com.santiifm.milou.data.model.Console
import com.santiifm.milou.data.model.ContentType
import com.santiifm.milou.data.model.Manufacturer
import com.santiifm.milou.data.model.UrlEntry
import com.santiifm.milou.data.repository.ConsoleRepository
import com.santiifm.milou.data.repository.SettingsRepository
import com.santiifm.milou.data.service.DatabaseScrapingService
import com.santiifm.milou.data.service.DefaultSourcesLoader
import com.santiifm.milou.data.service.TorrentHandleRegistry
import com.santiifm.milou.data.state.RescanStateHolder
import com.santiifm.milou.util.FileParsingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseScrapingService: DatabaseScrapingService,
    private val manufacturerDao: ManufacturerDao,
    private val consoleDao: ConsoleDao,
    private val defaultSourcesLoader: DefaultSourcesLoader,
    private val consoleRepository: ConsoleRepository,
    private val rescanStateHolder: RescanStateHolder,
    private val settingsRepository: SettingsRepository,
    private val torrentHandleRegistry: TorrentHandleRegistry
) : ViewModel() {

    val manufacturers = combine(
        manufacturerDao.getAllManufacturers(),
        consoleDao.getAllConsoles()
    ) { manufacturers, consoles ->
        manufacturers.map { manufacturer ->
            val manufacturerConsoles = consoles.filter { it.manufacturerId == manufacturer.id }
                .map { console ->
                    Console(
                        id = console.id,
                        name = console.name,
                        urls = parseUrlEntries(console.urls)
                    )
                }
            Manufacturer(id = manufacturer.id, name = manufacturer.name, consoles = manufacturerConsoles)
        }
    }

    val isRescanning: StateFlow<Boolean> = rescanStateHolder.isRescanning
    val rescanErrorMessage: StateFlow<String?> = rescanStateHolder.errorMessage

    val consoleDownloadDirectories: StateFlow<Map<String, String>> = settingsRepository.consoleDownloadDirectories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    private val _showAddManufacturerDialog = MutableStateFlow(false)
    val showAddManufacturerDialog: StateFlow<Boolean> = _showAddManufacturerDialog.asStateFlow()
    private val _showAddConsoleDialog = MutableStateFlow(false)
    val showAddConsoleDialog: StateFlow<Boolean> = _showAddConsoleDialog.asStateFlow()
    private val _showAddUrlDialog = MutableStateFlow(false)
    val showAddUrlDialog: StateFlow<Boolean> = _showAddUrlDialog.asStateFlow()
    private val _showEditManufacturerDialog = MutableStateFlow(false)
    val showEditManufacturerDialog: StateFlow<Boolean> = _showEditManufacturerDialog.asStateFlow()
    private val _showEditConsoleDialog = MutableStateFlow(false)
    val showEditConsoleDialog: StateFlow<Boolean> = _showEditConsoleDialog.asStateFlow()
    private val _selectedManufacturerId = MutableStateFlow<String?>(null)
    val selectedManufacturerId: StateFlow<String?> = _selectedManufacturerId.asStateFlow()
    private val _selectedConsoleId = MutableStateFlow<String?>(null)
    val selectedConsoleId: StateFlow<String?> = _selectedConsoleId.asStateFlow()

    fun loadDefaultSources(context: Context) {
        viewModelScope.launch {
            if (consoleRepository.isDatabaseEmpty()) {
                rescanStateHolder.setRescanning(true)
                try {
                    defaultSourcesLoader.loadDefaultSourcesToDatabase(context)
                    var totalFiles = 0; var totalTags = 0; var processedConsoles = 0
                    val currentManufacturers = manufacturers.first()
                    val totalConsoles = currentManufacturers.sumOf { it.consoles.size }
                    rescanStateHolder.setProgressMessage("Starting initial scrape of $totalConsoles consoles...")
                    for (manufacturer in currentManufacturers) {
                        for (console in manufacturer.consoles) {
                            processedConsoles++
                            rescanStateHolder.setProgressMessage(
                                "Processing console $processedConsoles/$totalConsoles: ${console.name}")
                            val (files, tags) = databaseScrapingService.scrapeManufacturer(
                                Manufacturer(manufacturer.id, manufacturer.name, listOf(console)),
                                onScrapeError = { rescanStateHolder.setErrorMessage(it) }
                            )
                            totalFiles += files; totalTags += tags
                        }
                    }
                } finally {
                    rescanStateHolder.setRescanning(false)
                    rescanStateHolder.clearProgressMessage()
                    rescanStateHolder.clearTorrentFetchProgress()
                }
            }
        }
    }

    fun rescanAllSources() {
        viewModelScope.launch {
            rescanStateHolder.setRescanning(true)
            try {
                databaseScrapingService.clearAllData()
                var totalFiles = 0; var totalTags = 0; var processedConsoles = 0
                val currentManufacturers = manufacturers.first()
                val totalConsoles = currentManufacturers.sumOf { it.consoles.size }
                rescanStateHolder.setProgressMessage("Starting rescan of $totalConsoles consoles...")
                for (manufacturer in currentManufacturers) {
                    for (console in manufacturer.consoles) {
                        processedConsoles++
                        rescanStateHolder.setProgressMessage(
                            "Processing console $processedConsoles/$totalConsoles: ${console.name}")
                        val (files, tags) = databaseScrapingService.scrapeManufacturer(
                            Manufacturer(manufacturer.id, manufacturer.name, listOf(console)),
                            onScrapeError = { rescanStateHolder.setErrorMessage(it) }
                        )
                        totalFiles += files; totalTags += tags
                    }
                }
            } finally {
                rescanStateHolder.setRescanning(false)
                rescanStateHolder.clearProgressMessage()
                rescanStateHolder.clearTorrentFetchProgress()
            }
        }
    }

    fun refreshConsole(consoleId: String) {
        if (rescanStateHolder.isRescanning.value) return
        viewModelScope.launch {
            rescanStateHolder.setRescanning(true)
            try {
                val console = consoleDao.getConsoleById(consoleId) ?: return@launch
                rescanStateHolder.setProgressMessage("Refreshing ${console.name}...")
                databaseScrapingService.clearConsoleData(consoleId)
                val consoleModel = Console(
                    id = console.id,
                    name = console.name,
                    urls = parseUrlEntries(console.urls)
                )
                databaseScrapingService.scrapeManufacturer(
                    Manufacturer(console.manufacturerId, console.manufacturerId, listOf(consoleModel)),
                    onScrapeError = { rescanStateHolder.setErrorMessage(it) }
                )
            } finally {
                rescanStateHolder.setRescanning(false)
                rescanStateHolder.clearProgressMessage()
                rescanStateHolder.clearTorrentFetchProgress()
            }
        }
    }

    fun showAddManufacturerDialog() { _showAddManufacturerDialog.value = true }
    fun hideAddManufacturerDialog() { _showAddManufacturerDialog.value = false }

    fun showAddConsoleDialog(manufacturerId: String) {
        _selectedManufacturerId.value = manufacturerId
        _showAddConsoleDialog.value = true
    }
    fun hideAddConsoleDialog() {
        _showAddConsoleDialog.value = false
        _selectedManufacturerId.value = null
    }

    fun showAddUrlDialog(consoleId: String) {
        _selectedConsoleId.value = consoleId
        _showAddUrlDialog.value = true
    }
    fun hideAddUrlDialog() {
        _showAddUrlDialog.value = false
        _selectedConsoleId.value = null
    }

    fun showEditManufacturerDialog(manufacturerId: String) {
        _selectedManufacturerId.value = manufacturerId
        _showEditManufacturerDialog.value = true
    }
    fun hideEditManufacturerDialog() {
        _showEditManufacturerDialog.value = false
        _selectedManufacturerId.value = null
    }

    fun showEditConsoleDialog(manufacturerId: String, consoleId: String) {
        _selectedManufacturerId.value = manufacturerId
        _selectedConsoleId.value = consoleId
        _showEditConsoleDialog.value = true
    }
    fun hideEditConsoleDialog() {
        _showEditConsoleDialog.value = false
        _selectedManufacturerId.value = null
        _selectedConsoleId.value = null
    }

    fun clearRescanError() {
        rescanStateHolder.setErrorMessage(null)
    }

    fun addManufacturer(name: String) {
        viewModelScope.launch {
            manufacturerDao.insertManufacturer(
                ManufacturerEntity(id = name.lowercase().replace(" ", "_"), name = name))
        }
    }

    fun addConsole(manufacturerId: String, name: String) {
        viewModelScope.launch {
            consoleDao.insertConsole(ConsoleEntity(
                id = "${manufacturerId}_${name.lowercase().replace(" ", "_")}",
                name = name, manufacturerId = manufacturerId, urls = JSONArray().toString()))
        }
    }

    fun addUrl(
        consoleId: String,
        url: String,
        contentType: ContentType = ContentType.GAME
    ) {
        viewModelScope.launch {
            val console = consoleDao.getConsoleById(consoleId) ?: return@launch
            val urls = parseUrlEntries(console.urls).toMutableList()

            val finalUrl = if (url.startsWith("content://")) {
                copyTorrentToInternalStorage(url) ?: return@launch
            } else if (url.startsWith("magnet:")) {
                FileParsingUtils.optimizeMagnetUri(url)
            } else {
                url
            }

            urls.add(UrlEntry(url = finalUrl, contentType = contentType))
            consoleDao.updateConsole(console.copy(urls = serializeUrlEntries(urls)))
        }
    }

    private fun copyTorrentToInternalStorage(uriString: String): String? {
        return try {
            val uri = uriString.toUri()
            val fileName = "uploaded_${System.currentTimeMillis()}.torrent"
            val destFile = File(context.filesDir, "torrents/$fileName")
            destFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteManufacturer(manufacturerId: String) {
        viewModelScope.launch {
            // Release all torrent handles for consoles under this manufacturer
            val consolesForManufacturer = consoleDao.getConsolesByManufacturerOnce(manufacturerId)
            consolesForManufacturer.forEach { console ->
                releaseTorrentHandlesForConsole(console.urls)
            }
            manufacturerDao.deleteManufacturerById(manufacturerId)
        }
    }

    fun deleteConsole(consoleId: String) {
        viewModelScope.launch {
            // Release any torrent handles tied to this console's URLs
            val console = consoleDao.getConsoleById(consoleId)
            if (console != null) releaseTorrentHandlesForConsole(console.urls)
            consoleDao.deleteConsoleById(consoleId)
        }
    }

    fun deleteUrl(consoleId: String, urlIndex: Int) {
        viewModelScope.launch {
            val console = consoleDao.getConsoleById(consoleId) ?: return@launch
            val urls = parseUrlEntries(console.urls).toMutableList()
            if (urlIndex in urls.indices) {
                val removed = urls.removeAt(urlIndex)
                releaseTorrentSource(removed.url)
                consoleDao.updateConsole(console.copy(urls = serializeUrlEntries(urls)))
            }
        }
    }

    fun updateManufacturer(manufacturerId: String, name: String) {
        viewModelScope.launch {
            val m = manufacturerDao.getManufacturerById(manufacturerId) ?: return@launch
            manufacturerDao.updateManufacturer(m.copy(name = name))
        }
    }

    fun updateConsole(consoleId: String, name: String) {
        viewModelScope.launch {
            val c = consoleDao.getConsoleById(consoleId) ?: return@launch
            consoleDao.updateConsole(c.copy(name = name))
        }
    }

    fun setSelectedConsoleId(consoleId: String) { _selectedConsoleId.value = consoleId }

    fun updateConsoleDownloadDirectory(consoleId: String, path: String) {
        viewModelScope.launch { settingsRepository.updateConsoleDownloadDirectory(consoleId, path) }
    }

    /**
     * Releases all torrent handles for URLs in [urlsJson] and deletes any copied
     * .torrent files. Called before deleting a console or manufacturer.
     */
    private suspend fun releaseTorrentHandlesForConsole(urlsJson: String) {
        parseUrlEntries(urlsJson).forEach { releaseTorrentSource(it.url) }
    }

    /**
     * Releases the handle for a torrent source URL (magnet or .torrent file path)
     * and deletes the copied .torrent file from internal storage if present.
     */
    private suspend fun releaseTorrentSource(url: String) {
        when {
            url.startsWith("magnet:") -> torrentHandleRegistry.releaseHandle(url)
            url.endsWith(".torrent") -> {
                torrentHandleRegistry.releaseHandle(url)
                // File.delete() is blocking I/O — must not run on the main dispatcher.
                withContext(Dispatchers.IO) {
                    File(url).takeIf { it.exists() && it.absolutePath.startsWith(context.filesDir.absolutePath) }?.delete()
                }
            }
        }
    }

    private fun parseUrlEntries(json: String): List<UrlEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            val foldersArr = obj.optJSONArray("folders")
            val folders = if (foldersArr != null) {
                (0 until foldersArr.length()).map { i -> foldersArr.getString(i) }
            } else emptyList()
            UrlEntry(
                url = obj.getString("url"),
                contentType = ContentType.valueOf(obj.optString("contentType", "GAME")),
                folders = folders
            )
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeUrlEntries(entries: List<UrlEntry>): String =
        JSONArray(entries.map { e ->
            JSONObject().apply {
                put("url", e.url)
                put("contentType", e.contentType.name)
                if (e.folders.isNotEmpty()) put("folders", JSONArray(e.folders))
            }
        }).toString()
}

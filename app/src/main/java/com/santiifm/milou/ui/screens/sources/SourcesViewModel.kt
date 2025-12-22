package com.santiifm.milou.ui.screens.sources

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.dao.ManufacturerDao
import com.santiifm.milou.data.local.entity.ConsoleEntity
import com.santiifm.milou.data.local.entity.ManufacturerEntity
import com.santiifm.milou.data.model.Console
import com.santiifm.milou.data.model.ContentType
import com.santiifm.milou.data.model.Manufacturer
import com.santiifm.milou.data.model.UrlEntry
import com.santiifm.milou.data.repository.ConsoleRepository
import com.santiifm.milou.data.repository.DownloadableFileRepository
import com.santiifm.milou.data.repository.SettingsRepository
import com.santiifm.milou.data.service.DatabaseScrapingService
import com.santiifm.milou.data.service.DefaultSourcesLoader
import com.santiifm.milou.data.state.RescanStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val databaseScrapingService: DatabaseScrapingService,
    private val manufacturerDao: ManufacturerDao,
    private val consoleDao: ConsoleDao,
    private val defaultSourcesLoader: DefaultSourcesLoader,
    private val consoleRepository: ConsoleRepository,
    private val downloadableFileRepository: DownloadableFileRepository,
    private val rescanStateHolder: RescanStateHolder,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val manufacturers = combine(
        manufacturerDao.getAllManufacturers(),
        consoleDao.getAllConsoles()
    ) { manufacturers, consoles ->
        manufacturers.map { manufacturer ->
            val manufacturerConsoles = consoles.filter { it.manufacturerId == manufacturer.id }
                .map { console ->
                    val urls = try {
                        val jsonArray = JSONArray(console.urls)
                        (0 until jsonArray.length()).map {
                            val urlObj = jsonArray.getJSONObject(it)
                            UrlEntry(
                                url = urlObj.getString("url"),
                                contentType = ContentType.valueOf(urlObj.optString("contentType", "GAME"))
                            )
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    Console(
                        id = console.id,
                        name = console.name,
                        urls = urls
                    )
                }
            Manufacturer(
                id = manufacturer.id,
                name = manufacturer.name,
                consoles = manufacturerConsoles
            )
        }
    }

    val isRescanning: StateFlow<Boolean> = rescanStateHolder.isRescanning

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
            if (consoleRepository.isDatabaseEmpty() || downloadableFileRepository.isDatabaseEmpty()) {
                rescanStateHolder.setRescanning(true)
                try {
                    defaultSourcesLoader.loadDefaultSourcesToDatabase(context)

                    var totalFiles = 0
                    var totalTags = 0
                    var processedConsoles = 0
                    var totalConsoles: Int

                    val currentManufacturers = manufacturers.first()
                    totalConsoles = currentManufacturers.sumOf { it.consoles.size }
                    rescanStateHolder.setProgressMessage("Starting initial scrape of $totalConsoles consoles...")
                    println("Starting initial scrape of $totalConsoles consoles...")

                    for (manufacturer in currentManufacturers) {
                        for (console in manufacturer.consoles) {
                            processedConsoles++
                            rescanStateHolder.setProgressMessage("Processing console $processedConsoles/$totalConsoles: ${console.name}")
                            println("Processing console $processedConsoles/$totalConsoles: ${console.name}")

                            val (files, tags) = databaseScrapingService.scrapeManufacturer(
                                Manufacturer(manufacturer.id, manufacturer.name, listOf(console))
                            )
                            totalFiles += files
                            totalTags += tags
                        }
                    }

                    println("Initial load and scrape completed: $processedConsoles consoles processed, $totalFiles files and $totalTags tags")
                } finally {
                    rescanStateHolder.setRescanning(false)
                    rescanStateHolder.clearProgressMessage()
                }
            } else {
                println("Database not empty, skipping initial load and scrape")
            }
        }
    }

    fun rescanAllSources() {
        viewModelScope.launch {
            rescanStateHolder.setRescanning(true)
            try {
                databaseScrapingService.clearAllData()

                var totalFiles = 0
                var totalTags = 0
                var processedConsoles = 0
                var totalConsoles: Int

                val currentManufacturers = manufacturers.first()
                totalConsoles = currentManufacturers.sumOf { it.consoles.size }
                rescanStateHolder.setProgressMessage("Starting rescan of $totalConsoles consoles...")
                println("Starting rescan of $totalConsoles consoles...")

                for (manufacturer in currentManufacturers) {
                    for (console in manufacturer.consoles) {
                        processedConsoles++
                        rescanStateHolder.setProgressMessage("Processing console $processedConsoles/$totalConsoles: ${console.name}")
                        println("Processing console $processedConsoles/$totalConsoles: ${console.name}")

                        val (files, tags) = databaseScrapingService.scrapeManufacturer(
                            Manufacturer(manufacturer.id, manufacturer.name, listOf(console))
                        )
                        totalFiles += files
                        totalTags += tags
                    }
                }

                println("Rescan completed: $processedConsoles consoles processed, $totalFiles files and $totalTags tags")
                rescanStateHolder.setProgressMessage("Rescan completed: $processedConsoles consoles processed")
            } finally {
                rescanStateHolder.setRescanning(false)
                rescanStateHolder.clearProgressMessage()
            }
        }
    }

    fun showAddManufacturerDialog() {
        _showAddManufacturerDialog.value = true
    }

    fun hideAddManufacturerDialog() {
        _showAddManufacturerDialog.value = false
    }

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

    fun addManufacturer(name: String) {
        viewModelScope.launch {
            val manufacturer = ManufacturerEntity(
                id = name.lowercase().replace(" ", "_"),
                name = name
            )
            manufacturerDao.insertManufacturer(manufacturer)
        }
    }

    fun addConsole(manufacturerId: String, name: String) {
        viewModelScope.launch {
            val console = ConsoleEntity(
                id = "${manufacturerId}_${name.lowercase().replace(" ", "_")}",
                name = name,
                manufacturerId = manufacturerId,
                urls = JSONArray().toString()
            )
            consoleDao.insertConsole(console)
        }
    }

    fun addUrl(consoleId: String, url: String, contentType: ContentType = ContentType.GAME) {
        viewModelScope.launch {
            val console = consoleDao.getConsoleById(consoleId)
            if (console != null) {
                val urls = try {
                    val jsonArray = JSONArray(console.urls)
                    (0 until jsonArray.length()).map {
                        val urlObj = jsonArray.getJSONObject(it)
                        UrlEntry(
                            url = urlObj.getString("url"),
                            contentType = ContentType.valueOf(urlObj.optString("contentType", "GAME"))
                        )
                    }.toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
                urls.add(UrlEntry(url = url, contentType = contentType))
                val urlsJson = JSONArray(urls.map {
                    JSONObject().apply {
                        put("url", it.url)
                        put("contentType", it.contentType.name)
                    }
                })
                val updatedConsole = console.copy(urls = urlsJson.toString())
                consoleDao.updateConsole(updatedConsole)
            }
        }
    }

    fun deleteManufacturer(manufacturerId: String) {
        viewModelScope.launch {
            manufacturerDao.deleteManufacturerById(manufacturerId)
        }
    }

    fun deleteConsole(consoleId: String) {
        viewModelScope.launch {
            consoleDao.deleteConsoleById(consoleId)
        }
    }

    fun deleteUrl(consoleId: String, urlIndex: Int) {
        viewModelScope.launch {
            val console = consoleDao.getConsoleById(consoleId)
            if (console != null) {
                val urls = try {
                    val jsonArray = JSONArray(console.urls)
                    (0 until jsonArray.length()).map {
                        val urlObj = jsonArray.getJSONObject(it)
                        UrlEntry(
                            url = urlObj.getString("url"),
                            contentType = ContentType.valueOf(urlObj.optString("contentType", "GAME"))
                        )
                    }.toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
                if (urlIndex in urls.indices) {
                    urls.removeAt(urlIndex)
                    val urlsJson = JSONArray(urls.map {
                        JSONObject().apply {
                            put("url", it.url)
                            put("contentType", it.contentType.name)
                        }
                    })
                    val updatedConsole = console.copy(urls = urlsJson.toString())
                    consoleDao.updateConsole(updatedConsole)
                }
            }
        }
    }

    fun showEditManufacturerDialog(manufacturerId: String) {
        _selectedManufacturerId.value = manufacturerId
        _showEditManufacturerDialog.value = true
    }

    fun hideEditManufacturerDialog() {
        _showEditManufacturerDialog.value = false
        _selectedManufacturerId.value = null
    }

    fun updateManufacturer(manufacturerId: String, name: String) {
        viewModelScope.launch {
            val manufacturer = manufacturerDao.getManufacturerById(manufacturerId)
            if (manufacturer != null) {
                val updatedManufacturer = manufacturer.copy(name = name)
                manufacturerDao.updateManufacturer(updatedManufacturer)
            }
        }
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

    fun updateConsole(consoleId: String, name: String) {
        viewModelScope.launch {
            val console = consoleDao.getConsoleById(consoleId)
            if (console != null) {
                val updatedConsole = console.copy(name = name)
                consoleDao.updateConsole(updatedConsole)
            }
        }
    }

    fun setSelectedConsoleId(consoleId: String) {
        _selectedConsoleId.value = consoleId
    }

    fun updateConsoleDownloadDirectory(consoleId: String, path: String) {
        viewModelScope.launch {
            settingsRepository.updateConsoleDownloadDirectory(consoleId, path)
        }
    }
}
package com.santiifm.milou.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiifm.milou.data.local.entity.ConsoleEntity
import com.santiifm.milou.data.local.dao.ConsoleWithFileCount
import com.santiifm.milou.data.model.DownloadableFileWithTags
import com.santiifm.milou.data.model.CategorizedTags
import com.santiifm.milou.data.repository.ConsoleRepository
import com.santiifm.milou.data.repository.DownloadableFileRepository
import com.santiifm.milou.data.repository.SettingsRepository
import com.santiifm.milou.data.service.DownloadService
import com.santiifm.milou.data.state.RescanStateHolder
import com.santiifm.milou.util.ConsoleFormatter
import com.santiifm.milou.util.StorageHelper
import com.santiifm.milou.util.ToastUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DownloadableFileRepository,
    private val consoleRepository: ConsoleRepository,
    private val downloadService: DownloadService,
    private val settingsRepository: SettingsRepository,
    private val rescanStateHolder: RescanStateHolder
) : ViewModel() {

    private val _selectedConsoles = MutableStateFlow<Set<String>>(emptySet())
    val selectedConsoles: StateFlow<Set<String>> = _selectedConsoles

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeTags = MutableStateFlow<Set<String>>(emptySet())
    val activeTags: StateFlow<Set<String>> = _activeTags.asStateFlow()

    private val _sortAsc = MutableStateFlow(true)
    val sortAsc: StateFlow<Boolean> = _sortAsc.asStateFlow()

    private val _tagFilterMode = MutableStateFlow(FilterMode.OR)
    val tagFilterMode: StateFlow<FilterMode> = _tagFilterMode

    private val _results = MutableStateFlow<List<DownloadableFileWithTags>>(emptyList())
    val results: StateFlow<List<DownloadableFileWithTags>> = _results

    private val _consoles = MutableStateFlow<List<ConsoleEntity>>(emptyList())
    val consoles: StateFlow<List<ConsoleEntity>> = _consoles

    private val _consolesWithFiles = MutableStateFlow<List<ConsoleWithFileCount>>(emptyList())
    val consolesWithFiles: StateFlow<List<ConsoleWithFileCount>> = _consolesWithFiles

    private val _categorizedTags = MutableStateFlow<CategorizedTags?>(null)
    val categorizedTags: StateFlow<CategorizedTags?> = _categorizedTags

    private val _hasMoreResults = MutableStateFlow(true)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private var currentOffset = 0
    private val pageSize = 100

    init {
        viewModelScope.launch {
            combine(
                combine(_searchQuery, _selectedConsoles, _activeTags) { q, c, t -> Triple(q, c, t) },
                combine(_sortAsc, _tagFilterMode, rescanStateHolder.lastRescanTime) { s, m, r -> Triple(s, m, r) }
            ) { first, second ->
                FilterParams(
                    query = first.first,
                    consoles = first.second,
                    tags = first.third,
                    sortAsc = second.first,
                    tagMode = second.second
                )
            }.collect { params ->
                currentOffset = 0
                val initialResults = performSearch(params)
                _results.value = initialResults
                _hasMoreResults.value = initialResults.size >= pageSize
                loadConsoles()
                loadAvailableTags(params.query, params.consoles)
            }
        }
    }

    fun toggleConsoleFilter(consoleId: String) {
        val currentConsoles = _selectedConsoles.value.toMutableSet()
        if (currentConsoles.contains(consoleId)) {
            currentConsoles.remove(consoleId)
        } else {
            currentConsoles.add(consoleId)
        }
        _selectedConsoles.value = currentConsoles
    }

    fun clearConsoleFilters() {
        _selectedConsoles.value = emptySet()
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleTag(tag: String) {
        val currentTags = _activeTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _activeTags.value = currentTags
    }

    fun removeTag(tag: String) {
        _activeTags.value = _activeTags.value - tag
    }

    fun removeConsole(consoleId: String) {
        _selectedConsoles.value = _selectedConsoles.value - consoleId
    }

    fun setSortAsc(ascending: Boolean) {
        _sortAsc.value = ascending
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _activeTags.value = emptySet()
        _selectedConsoles.value = emptySet()
        _sortAsc.value = true
        _tagFilterMode.value = FilterMode.OR
    }

    fun toggleTagFilterMode() {
        _tagFilterMode.value = if (_tagFilterMode.value == FilterMode.OR) FilterMode.AND else FilterMode.OR
    }

    private suspend fun performSearch(params: FilterParams): List<DownloadableFileWithTags> {
        currentOffset = 0
        return repository.searchFilesWithTags(
            query = params.query,
            consoleIds = params.consoles,
            tags = params.tags,
            matchAllTags = params.tagMode == FilterMode.AND,
            sortAsc = params.sortAsc,
            limit = pageSize,
            offset = 0
        )
    }

    private suspend fun loadConsoles() {
        val allConsoles = consoleRepository.getAllConsoles().first()
        _consoles.value = allConsoles.sortedBy { ConsoleFormatter.getConsoleDisplayName(it.id) }

        val consolesWithFiles = repository.getConsolesWithFiles(
            query = _searchQuery.value.ifBlank { "*" },
            manufacturer = null
        )
        _consolesWithFiles.value = consolesWithFiles.sortedBy { ConsoleFormatter.getConsoleDisplayName(it.id) }
    }

    private suspend fun loadAvailableTags(query: String, consoleIds: Set<String>) {
        _categorizedTags.value = repository.getCategorizedTags(
            query = query,
            consoleIds = consoleIds
        )
    }

    fun getConsoleName(consoleId: String): String {
        return ConsoleFormatter.formatConsoleField(consoleId)
    }

    suspend fun loadMore() {
        if (_isLoadingMore.value || !_hasMoreResults.value) return

        _isLoadingMore.value = true
        currentOffset += pageSize

        val newResults = repository.searchFilesWithTags(
            query = _searchQuery.value,
            consoleIds = _selectedConsoles.value,
            tags = _activeTags.value,
            matchAllTags = _tagFilterMode.value == FilterMode.AND,
            sortAsc = _sortAsc.value,
            limit = pageSize,
            offset = currentOffset
        )

        if (newResults.isEmpty()) {
            _hasMoreResults.value = false
        } else {
            _results.value += newResults
            if (newResults.size < pageSize) _hasMoreResults.value = false
        }

        _isLoadingMore.value = false
    }

    suspend fun startDownload(fileWithTags: DownloadableFileWithTags, context: Context) {
        val downloadDirectory = settingsRepository.downloadDirectory.first()
        if (downloadDirectory.isEmpty()) {
            ToastUtil.showError(context, "Download directory not configured. Please set a download folder in settings.")
            return
        }

        if (!StorageHelper.isValidUri(context, downloadDirectory)) {
            ToastUtil.showError(context, "Download directory is not accessible. Please check your folder permissions in settings.")
            return
        }

        downloadService.startDownload(fileWithTags.file)
    }
}

data class FilterParams(
    val query: String,
    val consoles: Set<String>,
    val tags: Set<String>,
    val sortAsc: Boolean,
    val tagMode: FilterMode
)

enum class FilterMode {
    AND, OR
}

package com.santiifm.milou.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santiifm.milou.data.model.DownloadItemModel
import com.santiifm.milou.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItemModel>> = repository.downloads
    
    private val _showDeleteConfirmation = MutableStateFlow<String?>(null)
    val showDeleteConfirmation: StateFlow<String?> = _showDeleteConfirmation.asStateFlow()

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            repository.cancelDownload(id)
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch { 
            repository.retryDownload(id) 
        }
    }

    fun deleteDownload(id: String, deleteFile: Boolean = false) {
        viewModelScope.launch { 
            repository.deleteDownload(id, deleteFile) 
        }
    }
    
    fun deleteDownloadWithConfirmation(id: String, isCompleted: Boolean) {
        if (isCompleted) {
            _showDeleteConfirmation.value = id
        } else {
            deleteDownload(id, deleteFile = false)
        }
    }
    
    fun confirmDeleteKeepFile(id: String) {
        _showDeleteConfirmation.value = null
        deleteDownload(id, deleteFile = false)
    }
    
    fun confirmDeleteRemoveFile(id: String) {
        _showDeleteConfirmation.value = null
        deleteDownload(id, deleteFile = true)
    }
    
    fun cancelDeleteConfirmation() {
        _showDeleteConfirmation.value = null
    }
}

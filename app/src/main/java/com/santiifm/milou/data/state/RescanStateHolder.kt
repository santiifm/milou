package com.santiifm.milou.data.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RescanStateHolder @Inject constructor() {
    private val _isRescanning = MutableStateFlow(false)
    val isRescanning: StateFlow<Boolean> = _isRescanning.asStateFlow()

    private val _lastRescanTime = MutableStateFlow(0L)
    val lastRescanTime: StateFlow<Long> = _lastRescanTime.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _torrentFetchProgress = MutableStateFlow("")
    val torrentFetchProgress: StateFlow<String> = _torrentFetchProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setRescanning(value: Boolean) { 
        _isRescanning.value = value 
        if (!value) {
            _lastRescanTime.value = System.currentTimeMillis()
        }
    }
    fun setProgressMessage(message: String) { _progressMessage.value = message }
    fun clearProgressMessage() { _progressMessage.value = "" }
    fun setTorrentFetchProgress(message: String) { _torrentFetchProgress.value = message }
    fun clearTorrentFetchProgress() { _torrentFetchProgress.value = "" }
    fun setErrorMessage(message: String?) { _errorMessage.value = message }
}

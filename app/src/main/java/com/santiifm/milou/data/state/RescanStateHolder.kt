package com.santiifm.milou.data.state

import com.santiifm.milou.domain.event.ScrapingEvent
import com.santiifm.milou.domain.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RescanStateHolder @Inject constructor(
    private val eventBus: EventBus
) {
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        scope.launch {
            eventBus.events.collect { event ->
                if (event is ScrapingEvent) {
                    handleScrapingEvent(event)
                }
            }
        }
    }

    private fun handleScrapingEvent(event: ScrapingEvent) {
        when (event) {
            is ScrapingEvent.Started -> {
                setRescanning(true)
                setProgressMessage(event.message)
                setErrorMessage(null)
            }
            is ScrapingEvent.Progress -> {
                if (event.message.contains("torrent", ignoreCase = true)) {
                    setTorrentFetchProgress(event.message)
                } else {
                    setProgressMessage(event.message)
                }
            }
            is ScrapingEvent.Completed -> {
                setRescanning(false)
                clearProgressMessage()
                clearTorrentFetchProgress()
            }
            is ScrapingEvent.Error -> {
                setErrorMessage(event.errorMessage)
            }
        }
    }

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

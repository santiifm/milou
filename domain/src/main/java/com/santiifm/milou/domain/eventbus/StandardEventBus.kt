package com.santiifm.milou.domain.eventbus

import com.santiifm.milou.domain.event.MilouEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StandardEventBus : EventBus {
    private val _events = MutableSharedFlow<MilouEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<MilouEvent> = _events.asSharedFlow()

    override suspend fun publish(event: MilouEvent) {
        _events.emit(event)
    }
}

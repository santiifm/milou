package com.santiifm.milou.domain.eventbus

import com.santiifm.milou.domain.event.MilouEvent
import kotlinx.coroutines.flow.SharedFlow

interface EventBus {
    val events: SharedFlow<MilouEvent>
    suspend fun publish(event: MilouEvent)
}

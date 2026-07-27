package com.santiifm.milou.domain.event

sealed interface MilouEvent {
    val timestamp: Long
}

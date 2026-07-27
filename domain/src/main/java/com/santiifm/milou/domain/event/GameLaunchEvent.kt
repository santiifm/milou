package com.santiifm.milou.domain.event

sealed interface GameLaunchEvent : MilouEvent {
    val gameId: String

    data class Started(
        override val gameId: String,
        val emulatorId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : GameLaunchEvent

    data class Succeeded(
        override val gameId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : GameLaunchEvent

    data class Failed(
        override val gameId: String,
        val reason: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : GameLaunchEvent
}

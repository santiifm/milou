package com.santiifm.milou.domain.usecase

import com.santiifm.milou.domain.event.GameLaunchEvent
import com.santiifm.milou.domain.eventbus.EventBus
import com.santiifm.milou.domain.launcher.GameLauncher
import com.santiifm.milou.domain.model.Emulator
import com.santiifm.milou.domain.model.Game

class LaunchGameUseCase(
    private val launcher: GameLauncher,
    private val eventBus: EventBus
) {
    suspend operator fun invoke(game: Game, emulator: Emulator, romPath: String): Result<Unit> {
        if (!launcher.isInstalled(emulator.packageName)) {
            val error = "${emulator.name} is not installed."
            eventBus.publish(GameLaunchEvent.Failed(game.id.toString(), error))
            return Result.failure(Exception(error))
        }

        if (!emulator.supportedSystems.contains(game.consoleId)) {
            val error = "${emulator.name} does not support ${game.consoleId}."
            eventBus.publish(GameLaunchEvent.Failed(game.id.toString(), error))
            return Result.failure(Exception(error))
        }

        eventBus.publish(GameLaunchEvent.Started(game.id.toString(), emulator.id))

        val result = launcher.launch(game, emulator, romPath)

        if (result.isSuccess) {
            eventBus.publish(GameLaunchEvent.Succeeded(game.id.toString()))
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            eventBus.publish(GameLaunchEvent.Failed(game.id.toString(), error))
        }

        return result
    }
}

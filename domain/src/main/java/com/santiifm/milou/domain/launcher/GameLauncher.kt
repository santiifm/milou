package com.santiifm.milou.domain.launcher

import com.santiifm.milou.domain.model.Emulator
import com.santiifm.milou.domain.model.Game

interface GameLauncher {
    suspend fun launch(game: Game, emulator: Emulator, romPath: String): Result<Unit>
    fun isInstalled(packageName: String): Boolean
}

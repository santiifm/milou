package com.santiifm.milou.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.santiifm.milou.domain.launcher.GameLauncher
import com.santiifm.milou.domain.model.Emulator
import com.santiifm.milou.domain.model.Game
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmulatorLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GameLauncher {

    override suspend fun launch(game: Game, emulator: Emulator, romPath: String): Result<Unit> {
        return try {
            val romFile = File(romPath)
            if (!romFile.exists()) {
                return Result.failure(Exception("ROM file not found at $romPath"))
            }

            val romUri = Uri.fromFile(romFile)
            val intent = IntentFactory.buildIntent(emulator, romUri)
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

package com.santiifm.milou.data.service

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.santiifm.milou.domain.model.Emulator
import com.santiifm.milou.domain.model.LaunchType

object IntentFactory {

    fun buildIntent(emulator: Emulator, romUri: Uri): Intent {
        return when (emulator.launchType) {
            LaunchType.URI_DATA -> {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(romUri, "application/octet-stream")
                    setPackage(emulator.packageName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            LaunchType.EXTRA_BOOT_ROM -> {
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage(emulator.packageName)
                    putExtra("bootRom", romUri.toString())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            LaunchType.EXTRA_BOOT_PATH -> {
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage(emulator.packageName)
                    putExtra("bootPath", romUri.toString())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            LaunchType.RETROARCH_CORE -> {
                // RetroArch requires a bit more complexity
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(
                        emulator.packageName,
                        emulator.activityName ?: "com.retroarch.browser.retroactivity.RetroActivityFuture"
                    )
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    putExtra("ROM", romUri.toString())
                    // Note: Core path would normally be needed here, but for now we rely on RetroArch auto-detection
                    // or user configuration.
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
    }
}

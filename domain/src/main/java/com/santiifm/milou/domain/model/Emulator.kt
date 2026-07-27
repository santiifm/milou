package com.santiifm.milou.domain.model

enum class LaunchType {
    URI_DATA,
    EXTRA_BOOT_ROM,
    EXTRA_BOOT_PATH,
    RETROARCH_CORE
}

enum class EmulatorCapability {
    SAVE_STATES,
    CHEATS,
    FAST_FORWARD,
    NETPLAY,
    TOUCH_CONTROL
}

data class Emulator(
    val id: String,
    val name: String,
    val packageName: String,
    val supportedSystems: Set<String>,
    val launchType: LaunchType,
    val capabilities: Set<EmulatorCapability> = emptySet(),
    val activityName: String? = null
)

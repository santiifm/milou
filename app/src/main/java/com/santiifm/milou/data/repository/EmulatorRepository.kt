package com.santiifm.milou.data.repository

import com.santiifm.milou.domain.model.Emulator
import com.santiifm.milou.domain.model.LaunchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmulatorRepository @Inject constructor() {

    private val knownEmulators = listOf(
        Emulator(
            id = "retroarch",
            name = "RetroArch",
            packageName = "com.retroarch",
            supportedSystems = setOf("nes", "snes", "gba", "gbc", "gb", "n64", "genesis", "megadrive"),
            launchType = LaunchType.RETROARCH_CORE
        ),
        Emulator(
            id = "retroarch_plus",
            name = "RetroArch Plus",
            packageName = "com.retroarch.aarch64",
            supportedSystems = setOf("nes", "snes", "gba", "gbc", "gb", "n64", "genesis", "megadrive"),
            launchType = LaunchType.RETROARCH_CORE
        ),
        Emulator(
            id = "ppsspp",
            name = "PPSSPP",
            packageName = "org.ppsspp.ppsspp",
            supportedSystems = setOf("psp"),
            launchType = LaunchType.URI_DATA
        ),
        Emulator(
            id = "duckstation",
            name = "DuckStation",
            packageName = "com.github.stenzek.duckstation",
            supportedSystems = setOf("ps1"),
            launchType = LaunchType.EXTRA_BOOT_ROM
        ),
        Emulator(
            id = "aethersx2",
            name = "AetherSX2",
            packageName = "xyz.aethersx2.android",
            supportedSystems = setOf("ps2"),
            launchType = LaunchType.EXTRA_BOOT_PATH
        )
    )

    fun getKnownEmulators(): List<Emulator> = knownEmulators

    fun getEmulatorsForConsole(consoleId: String): List<Emulator> {
        return knownEmulators.filter { it.supportedSystems.contains(consoleId) }
    }
}

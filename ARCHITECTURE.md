# Milou Architecture Evolution

This document outlines the architectural transformation of Milou from a simple ROM downloader into a robust, multi-module retro gaming appliance.

## Core Philosophy
- **Offline-First**: All data, metadata, and visual assets are stored locally.
- **Clean Architecture**: Clear separation between UI, Business Logic (Domain), and Infrastructure.
- **Resilient Workflows**: Long-running tasks are persistent and resumable.

## Module Structure

### `:domain` (Pure Kotlin)
The heart of the application. Contains business rules, models, and interfaces.
- **Models**: `Game`, `GameFile`, `GameMetadata`, `Job`, `Emulator`.
- **UseCases**: `SearchLibraryUseCase`, `LaunchGameUseCase`, `ScrapeMetadataUseCase`.
- **Event System**: Centralized `EventBus` for cross-component communication.

### `:app` (Android Framework)
Infrastructure and UI implementation.
- **Data**: Room Database, DataStore, Repository implementations.
- **Services**: `DownloadService`, `ArchiveExtractorService`, `EmulatorLauncher`.
- **Workers**: Android `WorkManager` integration for background tasks.
- **UI**: Jetpack Compose based interface.

## Key Subsystems

### 1. Event Bus (The Nervous System)
A `SharedFlow`-based system that allows independent modules to communicate via events (`DownloadEvent`, `ScrapingEvent`, `GameLaunchEvent`) without tight coupling.

### 2. Job Manager (The Memory)
A persistent workflow engine that tracks long-running operations. It ensures that tasks like downloads and extractions survive app restarts and recover gracefully.

### 3. Metadata Engine (The Personality)
A modular provider system that enriches the library with artwork and descriptions.
- **Priority**: Hash-based matching (ScreenScraper) > Name-based fuzzy search (IGDB).
- **Identity**: Separation of conceptual `Game` from physical `GameFile`.

### 4. Emulator Launcher (The Hands)
A data-driven intent engine that handles launching games through various external emulators using secure `FileProvider` sharing.

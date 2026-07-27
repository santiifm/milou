# Changelog

## [1.0.5] - Build Fixes
### What's New!
- Fixed compilation errors in dependency injection modules.
- Added missing WorkManager and Hilt dependencies.
- Resolved issues with SevenZip initialization and DownloadProgressTracker.

## [0.5.0] - Metadata Engine Milestone
### Added
- **Game Identity Separation**: Decoupled conceptual Games from physical ROM files.
- **Metadata Scraper Engine**: Modular provider system supporting ScreenScraper (Hash) and IGDB (Name).
- **Offline Artwork Support**: Integrated `ImageManager` for local caching of covers and screenshots.
- **Database Migration**: `MIGRATION_3_4` added relational metadata storage.
- **Rich UI**: Updated library grid to display box art and game details.

## [0.4.0] - Persistent Workflows
### Added
- **Job Manager**: Room-backed system for tracking persistent background tasks.
- **Android WorkManager**: Integrated for reliable background execution.
- **Resilience**: Added `RECOVERABLE` status and startup recovery logic.

## [0.3.0] - Event Bus System
### Added
- **Centralized EventBus**: `SharedFlow`-based communication for decoupled services.
- **Real-time Updates**: Decoupled `DownloadProgressTracker` from `DownloadService`.

## [0.2.0] - Architecture Foundation
### Added
- **Domain Module**: Extracted core business logic into pure Kotlin `:domain`.
- **Clean Architecture**: Implemented Repository pattern and UseCases.

## [0.1.0] - Original Fork Baseline
- Initial migration of the original project foundation.

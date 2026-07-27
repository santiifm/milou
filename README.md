# Milou - Retro Gaming Library & Launcher

Milou is an offline-first retro gaming library and launcher for Android. It manages your game collection, downloads, metadata, and artwork, and launches games through compatible external emulators.

Based on the original Milou project by [santiifm](https://github.com/santiifm/milou).

## Key Features
- **Offline-First**: All game metadata, covers, and descriptions are stored locally for instant access.
- **Smart Discovery**: Automatically index and tag games from torrents, magnets, or local sources.
- **Persistent Workflows**: Integrated Job Manager ensures downloads and extractions survive app restarts.
- **Metadata Engine**: Rich game information and box art powered by IGDB and ScreenScraper.
- **Emulator Integration**: Launch games directly into RetroArch, DuckStation, PPSSPP, and more.
- **Clean Architecture**: Built with a modular, domain-driven design for stability and scalability.

## How It Works

Milou transforms the raw data from game sources into a structured personal library. It handles the complete lifecycle of a game:
1. **Discover**: Scrape sources to find games for your favorite consoles.
2. **Download**: High-speed downloads via HTTP or BitTorrent.
3. **Organize**: Automatic archive extraction and file management.
4. **Enrich**: Automatic metadata scraping for covers and rich descriptions.
5. **Play**: One-tap launching into your preferred emulator.

## Documentation
- [Architecture Overview](ARCHITECTURE.md): Deep dive into the multi-module design and subsystems.
- [Roadmap](ROADMAP.md): Current status and future development plans.
- [Changelog](CHANGELOG.md): History of major milestones and releases.

## Building

### Build Requirements
- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 34

### Build Steps
```bash
# Clone repository
git clone https://github.com/yourusername/milou.git
cd milou

# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :domain:test
./gradlew :app:testDebugUnitTest
```

## Tech Stack
- **Languages**: Kotlin (Pure JVM & Android)
- **UI**: Jetpack Compose, Material Design 3
- **Architecture**: Clean Architecture, MVI, Multi-Module
- **Database**: Room (SQLite) with FTS
- **Background Tasks**: Android WorkManager
- **Communication**: Kotlin Coroutines & Flow (SharedFlow EventBus)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit, Jsoup, libtorrent4j
- **Image Loading**: Coil

## Disclaimer
This app is for educational purposes only. Users are responsible for ensuring they have the legal right to download or play any games used with this software.

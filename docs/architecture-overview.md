# Milou Architecture Overview

## 1. Core Philosophy

Milou follows an offline-first Android appliance model:

```text
Sources
   ↓
Scraper Engine
   ↓
Local Database (Room)
   ↓
Discovery UI
   ↓
Download Engine
   ↓
Extraction + Organization
   ↓
Game Library
```

The app does not depend on a cloud backend. The phone or tablet becomes the user's personal retro game library server.

This matches the same philosophy used in other local-first product designs:

- local database as source of truth
- background services handle heavy work
- UI observes state through Flow
- modules communicate through repositories and services

---

## 2. Current Technical Stack

### Presentation Layer

Jetpack Compose is used as the UI framework.

```text
MainActivity
 └── NavHost
      ├── Home
      ├── Downloads
      ├── Sources
      ├── Settings
      └── Contact
```

The app uses:

- Compose UI
- Navigation Compose
- ViewModel
- StateFlow

The general pattern is:

```text
UI
 |
ViewModel
 |
Repository
 |
Room / Service
```

This is a modern and maintainable Android architecture.

---

## 3. Data Layer

### Room Database

The current persistence model includes entities for:

```text
Database
│
├── Manufacturer
├── Console
├── DownloadableFile
├── Tag
└── FTS Search Data
```

The use of full-text search integration suggests the app is already designed for larger libraries. Instead of relying on simple pattern matching, it can evolve toward a richer and faster discovery experience.

---

## 4. Scraping System

This is one of Milou's most distinctive components.

```text
Source URL
    |
    ↓
Jsoup Parser
    |
    ↓
Filename Analyzer
    |
    ↓
Metadata Extractor
    |
    ↓
Room Insert
```

The scraper extracts information such as:

- console identity
- tags
- filenames
- URLs
- torrent metadata

This allows the app to turn raw source listings into a navigable library structure.

---

## 5. Download Engine

The download engine is the heart of Milou.

```text
DownloadRepository

        |
        ↓

DownloadService
        |
        |
        ├── HTTP Downloader
        |
        ├── Torrent Engine
        |
        ├── Progress Tracker
        |
        ├── Archive Extractor
        |
        └── File Organizer
```

It uses:

- libtorrent4j
- Coroutines
- foreground service support

This makes it behave more like a background download manager than a regular UI-driven file downloader.

---

## 6. Torrent System

The torrent implementation is notably advanced.

```text
TorrentHandleRegistry

        |
        ↓

libtorrent Session

        |
        ↓

Torrent Handles
        |
        ├── Game A
        ├── Game B
        └── Game C
```

The registry helps prevent:

- duplicate torrent sessions
- memory leaks
- orphan handles

This is the correct approach for a long-running download-oriented app.

---

## 7. Storage Architecture

Milou uses Android's Storage Access Framework for user-selected destination folders.

```text
User selects folder

        ↓

Persist URI permission

        ↓

Milou manages:
/Games
   /Nintendo
      /NES
      /SNES

   /Sega
      /Genesis

   /Sony
      /PS1
```

The console-based organization feature makes the app feel more like a purpose-built appliance than a generic downloader.

---

## 8. Strengths

### 1. Strong Offline Design

Milou can work without:

- accounts
- servers
- subscriptions
- cloud databases

This is a strong advantage for a long-lived local media tool.

### 2. Clear Separation of Responsibilities

The app avoids putting scraping, downloading, extraction, and storage logic directly inside the screen layer.

Instead, responsibilities are split across:

- UI screens
- ViewModels
- Repositories
- Services

This makes the app easier to maintain and evolve.

### 3. Background Processing Is Well Structured

Milou already handles:

- foreground services
- notifications
- lifecycle awareness
- cancellation
- retry behavior

This is one of the strongest architectural parts of the current implementation.

---

## 9. Potential Future Improvements

### 1. Modularize the App

The project is currently centered around a single app module. A future structure could look like:

```text
Milou
├── app
├── core
│   ├── database
│   ├── network
│   ├── filesystem
│   └── common
├── feature-home
├── feature-download
├── feature-library
├── feature-settings
└── feature-sources
```

This would align better with modern Android multi-module architecture.

### 2. Add a Domain Layer

The current flow is roughly:

```text
ViewModel
 ↓
Repository
 ↓
DAO / Service
```

A future evolution could introduce use cases:

```text
ViewModel
 ↓
UseCase
 ↓
Repository
 ↓
Data Source
```

Examples:

- StartGameDownloadUseCase
- ScanSourcesUseCase
- OrganizeLibraryUseCase

### 3. Add Emulator Integration

A natural next step would be to connect the game library to an emulator launcher:

```text
Game Library
      ↓
Launch Game
      ↓
RetroArch / Emulator Intent
```

This would turn Milou from a downloader into a broader retro gaming platform.

### 4. Enhance Metadata

The current model is based largely on filenames and tags. A richer future architecture could include:

```text
Filename
 ↓
Parser
 ↓
Game Database API
 ↓
Artwork
 ↓
Description
 ↓
Screenshots
 ↓
Rating
```

### 5. Event-Driven Architecture

The app could benefit from a more explicit event model for background actions:

```text
GameDownloadedEvent
        ↓
 ├── Update Library
 ├── Refresh UI
 ├── Generate Metadata
 └── Notify User
```

---

## 10. Overall Assessment

Milou is architecturally closer to a native Android retro gaming ecosystem than a simple downloader.

### Current maturity

- Compose UI: Strong
- Dependency Injection: Strong
- Database layer: Strong
- Search capabilities: Strong
- Downloading: Advanced
- Torrent support: Advanced
- Offline support: Strong
- Modular architecture: Future improvement
- Domain layer: Future improvement
- Metadata enrichment: Future improvement
- Emulator integration: Major opportunity

The important takeaway is that Milou is already moving toward the same design philosophy seen in specialized local-first appliances:

- local-first
- background-driven
- state-observable
- focused on a single domain experience

---

## 11. Milou's Natural Evolution Path

Based on the current foundation, Milou is already close to a layered platform.

The current architecture:

```text
UI
 |
ViewModel
 |
Repository
 |
Database / Services
```

can gradually evolve into:

```text
Presentation Layer
        |
        ↓
Application Layer
        |
        ↓
Domain Layer
        |
        ↓
Infrastructure Layer
```

Example:

```text
Presentation

HomeScreen
DownloadScreen

Application

DownloadGameUseCase
ScanSourceUseCase
OrganizeLibraryUseCase

Domain

Game
Console
Library
DownloadTask

Infrastructure

Room
Torrent Engine
File System
Network
```

The advantage is that Milou's core logic becomes independent from Android frameworks.

---

## 12. Game Library as the Core Domain

Currently, the download system appears to be the center of the application.

However, the stronger long-term model is:

```text
                Game Library
                     |
        ┌────────────┼────────────┐
        ↓            ↓            ↓
   Discovery     Download     Launch
        |
        ↓
   Metadata
```

The download engine becomes just one capability.

The true product becomes:

> A personal retro game library manager.

This is similar to how:

- Steam is not a downloader; it is a game ecosystem.
- Plex is not a file scanner; it is a media library.
- Lightroom is not a file importer; it is a photo management system.

Milou has the same opportunity.

---

## 13. Introduce a Library State Machine

A useful addition would be a formal game lifecycle.

Example:

```text
Discovered
    |
    ↓
Queued
    |
    ↓
Downloading
    |
    ↓
Downloaded
    |
    ↓
Extracting
    |
    ↓
Installed
    |
    ↓
Playable
```

With failure paths:

```text
Downloading
      |
      ↓
   Failed
      |
      ↓
 Retry
```

This would unify:

- download status
- extraction status
- library availability

Instead of keeping separate states scattered across services.

---

## 14. Add a Local Event Bus

The event idea can become the backbone of Milou.

Example:

```text
GameDownloadedEvent

        |
        |
        ├── LibraryIndexer
        |
        ├── MetadataUpdater
        |
        ├── NotificationManager
        |
        └── UI Refresh
```

Benefits:

A download service does not need to know:

- who needs the data
- what UI exists
- what future features are added

A future feature such as an achievement tracker could simply subscribe to an event:

```text
GameInstalledEvent
          ↓
Track Played Games
```

---

## 15. Hardware Appliance Direction

This is where Milou becomes especially interesting.

The current design fits well with:

### Android handheld devices

Examples:

- Retroid Pocket
- Ayn Odin
- Anbernic Android devices

### Android TV / Living Room Console

```text
Android TV Box

      |
      ↓

Milou

      |
      ├── Library
      ├── Downloads
      ├── Emulator Launcher
      └── Controller Support
```

It could become:

> A personal retro console operating system.

---

## 16. Potential Module Architecture

A mature Milou project could eventually look like:

```text
milou/

├── app
│
├── core
│   ├── common
│   ├── database
│   ├── filesystem
│   ├── network
│   └── events
│
├── domain
│   ├── game
│   ├── library
│   ├── download
│   └── console
│
├── feature
│   ├── discovery
│   ├── downloads
│   ├── library
│   ├── settings
│   └── sources
│
├── data
│   ├── scraper
│   ├── torrent
│   ├── metadata
│   └── storage
│
└── launcher
    └── emulator integration
```

This is very close to how a commercial-grade Android product would be structured.

---

## 17. Comparison With Other Local-First Projects

The pattern across your projects is becoming clear:

```text
Project         Domain        Core Engine
ExtroPOS v2     Retail       Transaction Engine
Music Home      Audio         Media Library Engine
Milou           Gaming        Game Library Engine
```

All three share:

```text
Local Database
        |
        ↓
Domain Engine
        |
        ↓
Background Services
        |
        ↓
Appliance UI
```

The common architecture is not accidental.

You are essentially building single-purpose digital appliances rather than traditional CRUD applications.

---

## 18. Final Architectural Position

Milou's biggest opportunity is not adding more download features.

The strongest evolution path is:

```text
Retro Game Downloader
        ↓
Retro Game Library Manager
        ↓
Retro Gaming Appliance
        ↓
Personal Retro Console Platform
```

The foundation is already there:

- local database
- offline operation
- background processing
- metadata pipeline
- storage management
- domain-specific workflow

The missing pieces are mainly:

- stronger domain modeling
- library-centric architecture
- metadata enrichment
- emulator integration
- event-driven communication

From a software architecture perspective, Milou is already built closer to a platform than an application.

---

## 19. Milou's Core Bounded Contexts

The future architecture naturally separates into bounded contexts:

```text
Milou Platform

├── Discovery Context
│
│   Sources
│   Scraping
│   Search
│   Filtering
│
├── Library Context
│
│   Games
│   Consoles
│   Metadata
│   Collections
│
├── Download Context
│
│   Queue
│   Torrent
│   HTTP
│   Progress
│
├── Storage Context
│
│   Files
│   Paths
│   Organization
│
└── Launch Context
    |
    Emulator Integration
```

This is important because each context has a different responsibility.

For example, the Download context should not need to know that a file is for a SNES game. It should only know that it needs to retrieve a file. The Library context decides what that file represents.

---

## 20. The Game Entity Should Become the Center

Currently, the database seems file-oriented:

```text
DownloadableFile
      |
      ↓
Tags
      |
      ↓
Console
```

A future model could become game-oriented:

```text
Game
 |
 ├── Title
 ├── Console
 ├── Region
 ├── Release Year
 ├── Artwork
 ├── Description
 |
 └── Files
      |
      ├── ROM
      ├── Manual
      └── Patch
```

The relationship changes:

Before:

```text
File → Game
```

After:

```text
Game → Files
```

This is the same evolution that happened in media applications. A music app does not think in terms of raw MP3 files alone; it thinks in terms of albums and tracks. Milou should eventually think in terms of games and their associated files.

---

## 21. Introduce a Collection System

Once Game becomes the core entity, collections become natural.

Example:

```text
Library

├── Recently Added
│
├── Favorites
│
├── Completed
│
├── Playing
│
├── Arcade Classics
│
├── JRPG Collection
│
└── Childhood Games
```

The user experience shifts from:

"Find and download ROM"

to:

"Manage my personal retro library."

---

## 22. Add User Activity Tracking

With the event system, Milou can evolve into a personal gaming history tool.

Events could include:

```text
GameInstalledEvent

GameStartedEvent

GameCompletedEvent

GameFavoriteEvent
```

Stored locally as:

```text
GameActivity

├── gameId
├── playTime
├── lastPlayed
├── launchCount
└── status
```

Now Milou could answer questions such as:

- What games did I play this month?
- What game did I abandon?
- What console do I use most?

This turns the product into a personal gaming journal.

---

## 23. The Architecture Is Similar to Steam's Evolution

Steam's early identity:

```text
Game Downloader
```

Its current identity:

```text
Game Platform
```

The transition is:

```text
Download
   ↓
Install
   ↓
Library
   ↓
Community
   ↓
Personal Gaming History
```

Milou's possible path is:

```text
Scrape
   ↓
Download
   ↓
Library
   ↓
Launch
   ↓
Personal Retro Experience
```

The scale is different, but the architectural direction is similar.

---

## 24. Offline-First Gives Milou a Unique Identity

Many modern apps assume:

```text
Server
 |
API
 |
Client
```

Milou follows:

```text
Device
 |
Local Database
 |
Local Services
 |
User Experience
```

Advantages include:

- no account dependency
- works without internet
- user owns the data
- faster response
- easier backup
- long lifespan

For retro gaming especially, this makes sense because ROM libraries are personal collections.

---

## 25. Backup and Migration Should Become a First-Class Feature

Because the app is local-first, data portability becomes important.

A future backup design could look like:

```text
Milou Backup

├── library.json
├── metadata/
├── artwork/
├── settings.json
└── database.sqlite
```

Migration would work like this:

```text
Old Device

     ↓ Export

Backup Package

     ↓ Import

New Device
```

This is one of the biggest advantages of local-first applications.

---

## 26. The Appliance OS Concept

The strongest vision for Milou is probably not:

> An Android app for downloading retro games.

It is:

> A retro gaming appliance interface running on Android hardware.

The final architecture could look like this:

```text
                 Milou Experience

                       |
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓

    Library       Launcher       Manager

        ↓              ↓              ↓

    Metadata      Emulator       Storage

                       |
                       ↓

                 Local Database
```

The Android device becomes the console.

---

## 27. Relationship With Other Projects

The architectural pattern is becoming a recognizable design language:

### ExtroPOS

```text
Business Data
      ↓
Transaction Engine
      ↓
POS Interface
```

### Music Home

```text
Media Data
      ↓
Library Engine
      ↓
Audio Appliance Interface
```

### Milou

```text
Game Data
      ↓
Game Library Engine
      ↓
Gaming Appliance Interface
```

The common idea is:

> Data is not the product. The domain engine built around that data is the product.

---

## 28. Final Position

Milou's architecture has crossed the boundary between utility software and platform software.

The current version is essentially:

```text
Retro downloader
```

The future mature version could become:

```text
Personal retro gaming ecosystem
```

The next architectural milestone should probably not be "more download features."

It should be:

1. Make Game the central domain object.
2. Separate Library from Download.
3. Add event-driven communication.
4. Add emulator launching.
5. Build metadata and collection management.

At that point, Milou stops being an app that manages files.

It becomes a personal retro gaming operating environment.

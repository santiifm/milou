# Project Context

## File reviewed
- README.md

## Findings
- Milou is an Android app for discovering, downloading, and managing retro games.
- The app scrapes metadata from torrent files or magnet links and supports automatic indexing/tagging.
- Core features include search/filtering, download management, automatic archive extraction, source management, and console-specific paths.
- The stack includes Jetpack Compose, Room, Hilt, Jsoup, HttpURLConnection, libtorrentj4, Apache Commons Compress, Coroutines, and Flow.
- The project is organized around the Android app module under app/.
- The Gradle setup defines a single Android app module named :app with Google/MavenCentral repositories and the Foojay toolchain resolver.
- The app module uses Android application plugins, Compose support, KSP, Hilt, and Room, targeting Android SDK 36 with min SDK 29.
- Key dependencies include Jetpack Compose, Navigation, DataStore, Room, Gson, Jsoup, 7z support, Coroutines, and libtorrent4j for torrent functionality.
- MainActivity is the single-activity entry point and hosts a Compose NavHost with home, downloads, sources, settings, and contact screens.
- The activity wires in a Hilt-backed SourcesViewModel to load default sources and show scraping errors through an AlertDialog.
- The application class initializes notification channels, loads the native 7-Zip library, checks for updates, and clears stale cache directories during startup.
- Dependency injection is handled with Hilt, and the current network module provides a singleton Gson instance configured with lenient parsing.
- The database module wires Room, DAO providers, SettingsDataStore, settings/download repositories, and the archive extraction service into the app container.
- Preferences are stored through a Hilt-provided DataStore instance named settings_preferences.
- The Room database defines entities for manufacturers, consoles, downloadable files, tags, and FTS data, with a version 2 migration that adds torrent file index and magnet columns.
- The download repository is a lightweight façade over a DownloadService and exposes download state through a StateFlow.
- The download service is the app's core background engine; it manages torrent and HTTP downloads, progress tracking, archive extraction, concurrency limits, and foreground-service lifecycle transitions.
- The UI navigation is organized with a sealed NavRoutes definition that exposes the home, downloads, sources, settings, and contact screens.
- The home screen is the main discovery experience: it shows search UI, paginated game results, and allows users to start downloads from a list of results.
- The home view model manages search, tag and console filters, pagination, available console/tag data, and download-start validation before delegating to the download service.
- The download view model exposes download state to the UI and handles cancel, retry, and delete flows with confirmation prompts.
- The downloads screen displays active and completed downloads and presents confirmation dialogs for delete actions.
- The sources screen lets users manage manufacturers, consoles, and torrent/magnet URLs, rescan sources, and assign custom download folders through a document-tree picker.
- The settings screen exposes persistent user preferences for the download directory, concurrent downloads, speed limits, auto-unzip, and console-based organization.
- The contact screen is a lightweight informational page with portrait/landscape layout adaptation.
- The sources view model coordinates source management, scraping, rescan workflows, dialog state, and cleanup of torrent handles and copied torrent files.
- The downloadable-file repository maps DAO query results into app-facing models and serves as the main access point for search, tag categorization, and console/file counts.
- The database scraping service crawls source URLs, parses file listings, and inserts discovered games plus tags into the local database, using separate handling for HTTP and torrent sources.
- The torrent download subsystem performs selective file downloads and manages libtorrent handles so multiple downloads can share a torrent session without conflicting priorities.
- The torrent handle registry caches metadata, starts and stops the libtorrent session, and coordinates handle lifecycle so torrent downloads can be resumed or released cleanly.
- The file parsing utilities normalize scraped file names, extract tags from display names, build absolute download URLs, and optimize magnet URIs for libtorrent performance.
- Storage helpers use Android's Storage Access Framework to create directories and files for downloads and to validate that selected storage URIs are still writable.
- Console formatting utilities transform internal console IDs into display-friendly names and line-break-friendly labels for the UI.
- The download model defines status values and UI asset mappings for progress, copying, extraction, completion, failure, and stopped states.
- The downloadable-file-with-tags model pairs a stored file entity with its discovered tag list for list rendering and filtering.

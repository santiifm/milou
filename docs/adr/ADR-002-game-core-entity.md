# ADR-002: Why make Game the core entity?

## Context

Milou currently works primarily around downloadable files, tags, and console identifiers. This is sufficient for a simple downloader, but it does not represent the real product experience of a retro gaming platform. Users think in terms of games, not raw files.

As the app evolves, users will want to manage libraries by game, organize metadata, track play progress, attach artwork, and eventually launch titles through emulators. A file-centric model makes these experiences harder to express naturally.

## Decision

Make Game the core domain entity around which the application is modeled.

The Game entity will represent a logical title in the library, and it will own or reference associated files, metadata, and platform information. The system will move from a file-first model toward a game-first model.

This means the architecture should treat concepts such as:

- title
- console
- region
- release information
- metadata
- artwork
- associated files
- library state

as part of the core game model rather than as scattered properties of individual files.

## Alternatives considered

1. Keep DownloadableFile as the main entity
   - Matches the current implementation closely
   - But makes the app feel file-oriented rather than library-oriented
   - Makes collections, metadata, and launch workflows awkward

2. Introduce a separate Game entity only as a UI concept
   - Too superficial
   - Would not solve the underlying modeling issue

3. Build around a generic media-item model
   - Too abstract for the specific needs of retro gaming
   - Would miss the domain-specific concepts such as console and release metadata

## Consequences

Positive:

- The product becomes easier to describe and reason about as a game library platform
- Collections, metadata, launch flows, and user activity all become more natural to implement
- The domain model better reflects the user’s mental model
- The architecture becomes more scalable for future platform features

Negative:

- Requires a shift from the current file-oriented schema toward a more game-centric model
- Existing data storage and repository logic will need to be refactored
- Some migration work will be needed to map old file-based data into game-based structures

# ADR-003: Why separate Download from Library?

## Context

The current application ties discovery, downloading, extraction, and library organization together in a way that is practical for an early version, but it also makes the product feel like a file pipeline rather than a game platform. Downloading is an important capability, but it is not the same thing as owning and managing a library.

If download logic and library logic remain tightly coupled, the product will become harder to evolve. Features such as collections, metadata, emulator launching, and library browsing all need a stable, independent notion of a game in the library.

## Decision

Separate the Download context from the Library context.

The Download context will be responsible for:

- queueing work
- HTTP and torrent retrieval
- progress tracking
- extraction status
- transfer lifecycle

The Library context will be responsible for:

- representing games
- organizing collections
- tracking library state
- metadata and availability
- launch readiness

This separation makes the architecture clearer and keeps each context focused on its own responsibility.

## Alternatives considered

1. Keep download and library behavior in one flow
   - Simpler initially
   - But creates a mixed domain that becomes harder to evolve

2. Treat download as part of the UI workflow only
   - Would hide important lifecycle behavior and make background operations harder to reason about

3. Make the library responsible for downloads directly
   - Overloads the library context with infrastructure concerns
   - Makes the domain model less clean

## Consequences

Positive:

- The architecture becomes more modular and easier to understand
- Download behavior can evolve independently from library features
- Library features can be built around a stable domain model rather than transient file-transfer state
- Future features such as collections and emulator launch become simpler to add

Negative:

- Requires clearer boundaries between contexts and shared data contracts
- Some existing workflows will need to be refactored into separate subsystems
- Coordination between download and library state must be explicit and well-defined

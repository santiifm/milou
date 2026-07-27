# ADR-005: Why adopt multi-module architecture?

## Context

Milou is already growing beyond a simple app prototype. It contains several distinct concerns: UI, search, scraping, download orchestration, storage, torrent handling, and future emulator integration. As these concerns grow, the single-module structure becomes less ideal for maintainability and team clarity.

A monolithic app module can make it harder to isolate responsibilities, control dependencies, and scale the product over time. This is especially relevant because Milou is evolving toward a platform-like product rather than a single-purpose utility.

## Decision

Adopt a multi-module architecture for Milou.

The project should eventually be organized into modules such as:

- app
- core
- domain
- data
- feature-discovery
- feature-downloads
- feature-library
- feature-settings
- feature-sources
- launcher

This structure will allow the app to separate product features from shared infrastructure and make the architecture easier to evolve over time.

## Alternatives considered

1. Continue with a single app module
   - Simpler to start
   - But becomes harder to manage as responsibilities grow
   - Increases coupling between features and infrastructure

2. Split only the UI layer
   - Improves some maintainability but does not address the deeper architectural complexity

3. Introduce a multi-module structure too early
   - Adds build and organizational overhead before the project truly benefits from it

## Consequences

Positive:

- Clearer separation of responsibilities
- Better dependency boundaries between features and infrastructure
- Easier parallel development and refactoring
- Better foundation for future platform growth, emulator integration, and metadata systems

Negative:

- Requires initial effort to define module boundaries and dependency rules
- Build configuration becomes more structured and potentially more complex
- Some refactoring is needed to move existing code into the right modules

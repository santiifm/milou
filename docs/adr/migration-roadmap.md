# Milou Migration Roadmap

This roadmap translates ADR-001 through ADR-005 into a practical, incremental migration plan. The goal is to evolve Milou toward a more modular and domain-driven architecture without rewriting the app, breaking existing functionality, or making large-risk changes in a single step.

## Guiding principles

- Existing Milou must continue working
- No rewrite
- Current features must remain functional
- Small commits
- Each phase must leave the project buildable

---

## Phase 0 — Stabilize the baseline

### 1. Goal

Create a safe baseline before introducing structural changes.

### 2. Files/modules affected

- app module
- Gradle build files
- existing ViewModels, repositories, services
- test setup if present

### 3. New architecture introduced

No new architecture yet. This phase establishes a reliable baseline and identifies coupling boundaries.

### 4. Migration steps

- Add lightweight documentation and ADR references
- Ensure the current app builds and runs in its existing state
- Identify the main seams between UI, repositories, and services
- Add basic regression checks around core flows such as:
  - source loading
  - search
  - download start
  - archive extraction
- Capture current behavior before changing internals

### 5. Risks

- Hidden assumptions in current flows
- Hard-to-reproduce behavior in background download paths

### 6. Rollback strategy

- Keep all changes isolated to docs and non-functional cleanup
- If a regression appears, revert the phase without affecting runtime behavior

---

## Phase 1 — Introduce a thin domain layer

### 1. Goal

Move the first important business rules out of the UI layer and into a small domain layer without changing the app’s external behavior.

### 2. Files/modules affected

- app/src/main/java/com/santiifm/milou/ui/screens/home/HomeViewModel.kt
- app/src/main/java/com/santiifm/milou/ui/screens/sources/SourcesViewModel.kt
- app/src/main/java/com/santiifm/milou/ui/screens/download/DownloadViewModel.kt
- app/src/main/java/com/santiifm/milou/data/repository/*
- new domain package under app or a new domain module later

### 3. New architecture introduced

A minimal domain layer with simple domain models and use cases.

Examples:

- StartDownloadUseCase
- ScanSourcesUseCase
- OrganizeLibraryUseCase

### 4. Migration steps

- Create a new domain package for core concepts such as Game, Console, LibraryState, and DownloadTask
- Move simple validation rules from ViewModels into use cases
- Keep ViewModels as adapters between UI and use cases
- Leave repositories and services intact for the first pass
- Use the same database and service implementations underneath
- Refactor one feature at a time, such as download-start validation or source scan preparation

### 5. Risks

- Overmoving logic too early and introducing unnecessary abstraction
- Accidentally changing behavior while extracting logic

### 6. Rollback strategy

- Keep the old ViewModel code path available until the new use case is verified
- Revert the specific use case and keep the rest of the app intact

---

## Phase 2 — Make Game the central domain object

### 1. Goal

Shift the mental model from file-centric to game-centric without breaking existing data flows.

### 2. Files/modules affected

- app/src/main/java/com/santiifm/milou/data/local/entity/*
- app/src/main/java/com/santiifm/milou/data/model/*
- repositories for downloadable files and tags
- HomeViewModel and related UI list components
- database DAOs and room entities

### 3. New architecture introduced

A game-centric domain model layered on top of the current persistence model.

The app will introduce a domain concept of Game while continuing to support existing underlying file entities.

### 4. Migration steps

- Introduce a lightweight Game domain model
- Add adapter/mapping logic between current entities and the new domain model
- Keep existing Room entities and repository access paths working
- Update UI model mapping so the Home screen can consume a game-oriented view model representation
- Avoid changing the storage schema in one step; instead, introduce an adapter layer first
- Once mapping is stable, begin using the new model in the UI and application layer

### 5. Risks

- Data mapping mistakes between old entities and new game model
- UI assumptions that still expect file-centric fields

### 6. Rollback strategy

- Keep the old entity mappings available behind the adapter layer
- If the new model causes issues, switch the adapter back to the previous mapping without changing the database layer

---

## Phase 3 — Separate Library from Download

### 1. Goal

Separate the responsibilities of downloading and library management while preserving the existing user experience.

### 2. Files/modules affected

- DownloadService
- DownloadRepositoryImpl
- DownloadViewModel
- HomeViewModel
- any library-facing UI components
- new library-oriented service or repository boundaries

### 3. New architecture introduced

A clear separation between a Download context and a Library context.

Responsibilities become:

- Download context: queue, progress, torrent/http transfers, extraction lifecycle
- Library context: game availability, metadata, collections, state transitions

### 4. Migration steps

- Introduce a LibraryStateCoordinator or equivalent service that tracks game availability independently from download progress
- Keep existing download functionality intact but route state updates through a library-state abstraction
- Add a shared state model for download completion and library readiness
- Update the UI to consume the new state model without removing existing progress UI
- Continue using the existing DownloadService internally but make it implement the new boundary contract

### 5. Risks

- State duplication between download flow and library flow
- Temporary inconsistency between “downloaded” and “installed” state

### 6. Rollback strategy

- Keep the old download service path intact behind a compatibility adapter
- Revert to the prior direct state flow if the split causes instability

---

## Phase 4 — Introduce event-driven communication

### 1. Goal

Reduce direct coupling between subsystems while keeping the app functional.

### 2. Files/modules affected

- DownloadService
- SourcesViewModel
- HomeViewModel
- Notification or UI update code
- any future metadata/indexer services

### 3. New architecture introduced

A lightweight local event infrastructure for core domain events.

Examples:

- GameDownloadedEvent
- GameInstalledEvent
- GameStartedEvent
- GameFavoriteEvent

### 4. Migration steps

- Introduce a simple event bus or event dispatcher inside the app module
- Start with a small number of events, such as download completion and library update
- Replace the most obvious direct side effects with event publishing
- Keep a compatibility layer so existing ViewModels and services can still react to regular state updates
- Add only one or two event subscribers first to prove the model

### 5. Risks

- Hidden side effects from event ordering
- Difficult debugging if events are published before listeners are registered

### 6. Rollback strategy

- Keep direct calls in place as a fallback until the event flow is stable
- Disable the event bus behind a feature flag if needed

---

## Phase 5 — Introduce the first modular boundaries

### 1. Goal

Split the app into a few modules without rewriting or disrupting existing runtime behavior.

### 2. Files/modules affected

- Gradle build files
- app module
- new modules such as core, domain, data, feature-downloads, feature-library

### 3. New architecture introduced

A multi-module structure with clear dependency boundaries.

### 4. Migration steps

- Extract shared infrastructure code first: common utils, storage helpers, database abstractions, networking helpers
- Create a new core module for shared dependencies
- Create a domain module for business concepts and use cases
- Move repository and service implementations into a data module gradually
- Keep the app module as the integration layer that composes features
- Move one feature area at a time, such as downloads or sources, into its own feature module
- Keep imports and public APIs stable while moving code

### 5. Risks

- Dependency cycles between modules
- Build script complexity
- Slower local iteration while module boundaries are established

### 6. Rollback strategy

- Keep the app module as the integration point and avoid removing old code immediately
- If a module split causes issues, move the affected code back into the app module and keep the rest of the separation intact

---

## Phase 6 — Add library-centric features on top of the new foundation

### 1. Goal

Use the new architecture to add higher-value platform features without reworking the app.

### 2. Files/modules affected

- library-related UI screens and view models
- metadata and collection management features
- future emulator integration hooks

### 3. New architecture introduced

A library-centric product model with collections, metadata, and activity tracking.

### 4. Migration steps

- Introduce collection support and activity tracking as additive features
- Build these features on top of the new game-centric model and event system
- Keep download features functional while adding richer library behavior
- Introduce emulator launching as a later optional expansion, not a blocker for the migration

### 5. Risks

- Scope creep if too many features are added at once
- Feature interactions become more complex as the product grows

### 6. Rollback strategy

- Keep new library features behind feature flags if needed
- Disable the new feature without removing the architectural foundation

---

## Recommended order of execution

1. Stabilize the baseline
2. Introduce a thin domain layer
3. Make Game the central domain object
4. Separate Library from Download
5. Introduce event-driven communication
6. Introduce the first module boundaries
7. Add library-centric features on top of the new foundation

## Suggested commit strategy

- One small refactor per phase
- One feature or subsystem moved at a time
- Each commit should be understandable and reversible
- Avoid mixing architecture refactoring with new feature work

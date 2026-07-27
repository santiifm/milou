# Phase 1 Implementation Plan: Introduce a Thin Domain Layer

## Goal

Introduce a small, behavior-preserving domain layer around the app’s three most important flows:

1. discovering and filtering content from the library
2. starting downloads from the home screen
3. running source rescans and refreshes from the sources screen

The first phase should not rewrite existing behavior, change the database schema, or introduce a large abstraction layer. The goal is to move the first meaningful business rules out of the UI layer while leaving repositories and services in place.

---

## Phase 1 scope

### In scope

- Extracting filter/search policy and pagination decisions from HomeViewModel
- Moving download-start validation and download preflight rules into a use case
- Moving source-scan orchestration and rescan policy into a use case
- Keeping the current Room entities, repositories, services, and UI flow intact

### Out of scope

- Changing the Room schema
- Replacing the existing download engine
- Rewriting the UI to a new architecture
- Introducing a full event bus or multi-module architecture

---

## Target architecture for Phase 1

The app will use a very small domain layer under the app module:

```text
UI (Compose + ViewModel)
  ↓
Use cases / domain rules
  ↓
Current repositories and services
```

### New domain package

Create a new package:

- app/src/main/java/com/santiifm/milou/domain/

### New subpackages

- domain/model/
- domain/usecase/

### Domain concepts introduced in this phase

- SearchCriteria: the normalized filter state for library discovery
- DownloadPreflightResult: the decision produced before a download starts
- SourceScanRequest: the scan scope and options for a rescan/refresh
- SourceScanSummary: the result of a source scan operation

---

## Files to change

### 1. HomeViewModel

Current responsibility
- Owns search state, filter state, pagination state, console/tag selection, and download-start validation
- Talks directly to repositories and services

What should move to the domain layer
- Filter normalization and pagination policy
- The decision of whether a download can start, based on settings and storage access
- The composition of search/filter inputs into a single domain object

What should remain in the ViewModel
- Exposing StateFlow values to Compose
- Handling UI toasts and context-dependent actions
- Triggering the use case and updating state

New use case
- SearchLibraryUseCase
- StartDownloadUseCase

Implementation notes
- Replace direct repository calls for search/filter logic with the use case
- Keep the existing repository and service implementation underneath
- Preserve the current page size and offset behavior exactly

---

### 2. SourcesViewModel

Current responsibility
- Owns dialog state, selected manufacturer/console, rescan workflows, and source-management actions
- Coordinates scraping and state holder updates

What should move to the domain layer
- The orchestration rules for a full rescan, refresh, or initial import
- The decision of what to scan and how to summarize the result

What should remain in the ViewModel
- Managing dialog visibility and selected IDs
- Updating rescanStateHolder with UI-facing progress/messages
- Handling direct DAO writes for add/edit operations

New use case
- ScanSourcesUseCase

Implementation notes
- Keep the existing DatabaseScrapingService and DefaultSourcesLoader calls in place
- The use case should return a summary object with counts and a list of processed consoles
- The ViewModel should still update the rescan state holder and error messages

---

### 3. DownloadViewModel

Current responsibility
- Exposes download state and routes cancel/retry/delete actions to the repository
- Manages confirmation state for delete actions

What should move to the domain layer
- Delete policy: whether an action needs confirmation based on status and delete intent
- Basic download action routing so the ViewModel becomes a thin presenter

What should remain in the ViewModel
- The confirmation dialog state
- UI-specific state flows

New use case
- ManageDownloadUseCase

Implementation notes
- Keep the repository contract intact
- The use case can return a simple decision object indicating whether confirmation is required

---

## New classes to add

### Domain models

1. app/src/main/java/com/santiifm/milou/domain/model/SearchCriteria.kt
   - Contains query, consoleIds, tags, tagMode, sortAsc, and pagination metadata

2. app/src/main/java/com/santiifm/milou/domain/model/DownloadPreflightResult.kt
   - Contains success/failure state and a user-facing message

3. app/src/main/java/com/santiifm/milou/domain/model/SourceScanRequest.kt
   - Contains scan scope: full rescan, refresh console, initial import

4. app/src/main/java/com/santiifm/milou/domain/model/SourceScanSummary.kt
   - Contains processed console count, file count, tag count, and an optional error list

### Use cases

1. app/src/main/java/com/santiifm/milou/domain/usecase/SearchLibraryUseCase.kt
   - Accepts the current filter state from the ViewModel
   - Returns the first page of results plus pagination metadata

2. app/src/main/java/com/santiifm/milou/domain/usecase/StartDownloadUseCase.kt
   - Validates the selected download directory and storage access
   - Returns a DownloadPreflightResult
   - Delegates the actual start action to DownloadService through the existing repository

3. app/src/main/java/com/santiifm/milou/domain/usecase/ScanSourcesUseCase.kt
   - Orchestrates a source scan using the existing scraping service
   - Returns a SourceScanSummary

4. app/src/main/java/com/santiifm/milou/domain/usecase/ManageDownloadUseCase.kt
   - Encapsulates the delete/retry/cancel policy and returns a simple decision object for the ViewModel

---

## Refactoring steps

### Commit 1 — Introduce the domain package and the first model

- Create the domain package and add SearchCriteria
- Add a minimal unit test around SearchCriteria normalization if test infrastructure is already available
- No behavior change in the app

### Commit 2 — Extract search and filter logic from HomeViewModel

- Add SearchLibraryUseCase
- Update HomeViewModel to use the use case for the initial search and the load-more path
- Keep the same repository and DAO layer underneath
- Verify that the Home screen still shows the same results and filters

### Commit 3 — Extract download-start preflight from HomeViewModel

- Add StartDownloadUseCase
- Move download directory validation into the use case
- Keep the actual startDownload call delegated to the existing download service/repository
- Verify that the app still shows the same toast behavior when the directory is not configured

### Commit 4 — Extract source-scan orchestration from SourcesViewModel

- Add ScanSourcesUseCase
- Move the scan loop and summary logic out of the ViewModel
- Keep the ViewModel responsible for updating rescanStateHolder and UI state
- Verify that full rescan, refresh console, and initial import still work

### Commit 5 — Thin download-action policy in DownloadViewModel

- Add ManageDownloadUseCase
- Move the delete confirmation decision into the use case
- Keep the UI dialog state in the ViewModel
- Verify that cancel, retry, and delete flows remain unchanged

---

## Behavioral compatibility rules

To keep the migration safe, the following rules must hold during Phase 1:

- No database migration
- No UI redesign
- No change to the visible text shown to users except possibly for internal consistency
- No change to the default settings values
- Existing repositories and services stay as the implementation backbone
- The new domain layer is additive and should be introduced behind the current architecture rather than replacing it

---

## How to verify each step

After each commit:

1. Run the Android app build
2. Exercise the following flows manually:
   - Home search and filtering
   - Starting a download from the home screen
   - Full rescan from sources
   - Refreshing a single console
   - Cancel/retry/delete on the downloads screen
3. Compare the behavior to the baseline from Phase 0

If any behavior changes unexpectedly, stop and revert the last commit before continuing.

---

## Risks and mitigation

### Risk: moving too much logic too early

Mitigation:
- Keep the use cases small and focused on one decision or orchestration flow
- Leave repository and service APIs unchanged

### Risk: accidentally changing behavior while extracting logic

Mitigation:
- Preserve the current method signatures and state values first
- Only move logic after the current behavior is understood and captured

### Risk: Android-specific code leaking into the domain layer

Mitigation:
- Keep domain models and use cases free from Compose, Context, and Android framework types
- Use simple data classes and plain Kotlin types only

---

## Definition of done for Phase 1

Phase 1 is complete when:

- The app still builds successfully
- The home search/filter flow still behaves the same
- Download starting still works with the same validation behavior
- Source rescans still work with the same progress/error reporting
- The codebase has a small but real domain layer that can be extended in later phases

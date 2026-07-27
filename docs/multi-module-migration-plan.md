# Multi-module migration plan for Milou

## 1. Current dependency graph (before migration)

The current project is a single Android app module with all responsibilities in one place.

```text
app module
├── UI layer
│   └── MainActivity, Compose screens, ViewModels, navigation
├── Application layer
│   └── Hilt modules, startup/bootstrap code
├── Domain-like behavior
│   └── search/filter rules, download validation, source-scan orchestration
├── Data/infrastructure layer
│   └── repositories, Room DAOs/entities, DataStore, scraping services, download services
└── Shared utilities
    └── storage, parsing, console formatting, constants
```

### What the current code implies

- ViewModels depend on repositories and services.
- Repositories depend on DAOs, services, and storage helpers.
- Services depend on repositories, other services, Room, DataStore, networking, archive handling, and torrent infrastructure.
- Hilt wiring is centralized in the app module.

This is workable, but it makes boundaries hard to protect and makes feature work more coupled to infrastructure.

---

## 2. Target dependency direction

The migration should follow a clean layered dependency rule:

```text
core
  ↑
domain
  ↑
data
  ↑
feature modules
  ↑
app (composition root)
```

### Dependency rules

- Core should not depend on domain, data, or features.
- Domain should depend only on core.
- Data should depend on domain and core.
- Feature modules should depend on domain, data, and core.
- The app module should be the composition root and should depend on features, not the other way around.

This keeps business rules independent from Android and infrastructure concerns.

---

## 3. What moves to which module

### A. Core module

Purpose:
- Hold platform-neutral primitives and shared utilities.
- Be the lowest-level dependency in the architecture.

What should move:
- Pure Kotlin utilities that have no Android, Compose, Room, or Hilt dependency.
- Shared constants and common value objects.
- Basic parsing helpers and normalization logic that do not require Context.
- Cross-cutting primitives such as result wrappers, error types, and lightweight extensions.

Why it moves:
- These pieces are reusable and should not be coupled to Android or app-specific behavior.
- They are the safest first step because moving them does not change runtime behavior.

Dependency direction:
- Core -> none of the other modules.

Risks:
- Pulling in Android-specific helpers too early can make the core module harder to keep pure.
- Some current utility classes are mixed with app-specific behavior and may need a small refactor before they can move.

---

### B. Domain module

Purpose:
- Own business rules and application intent.
- Remain free from Android, Compose, and persistence concerns.

What should move:
- Use cases such as search/filter orchestration, download preflight validation, and source-scan orchestration.
- Domain models such as SearchCriteria, DownloadPreflightResult, SourceScanRequest, and SourceScanSummary.
- Any business rules currently expressed inside ViewModels.

Why it moves:
- These rules describe what Milou should do, not how the UI or Android framework should do it.
- Moving them out of ViewModels makes the app easier to test and reason about.

Dependency direction:
- Domain -> core.
- Domain should not depend on data or UI modules.

Risks:
- Over-moving logic too early can create abstraction that is not yet needed.
- The biggest risk is accidentally changing behavior while extracting logic from ViewModels.

---

### C. Data module

Purpose:
- Own infrastructure details and persistence.
- Act as the bridge between the domain and the real-world implementations.

What should move:
- Room database setup, DAOs, and entities.
- Repository implementations.
- DataStore-backed settings access.
- Scraping services and download services.
- Torrent and archive-related infrastructure code.
- Storage Access Framework helpers and Android-specific file management code.

Why it moves:
- These classes implement persistence and platform integration, so they belong below the domain layer.
- They are the most coupled to Android and external systems and should not be spread across feature modules.

Dependency direction:
- Data -> domain and core.
- Data may depend on Android libraries and Room.

Risks:
- This is the largest migration step and will likely touch the most files.
- The download and scraping subsystems are behavior-sensitive and must be moved carefully.
- Build wiring can become more complex due to Hilt and Room setup across modules.

---

### D. Feature modules

Purpose:
- Own user-facing behavior for a single capability area.
- Keep the UI and navigation for a feature close to the feature itself.

What should move:
- Feature-specific screens and ViewModels.
- Navigation graphs for those features.
- Feature-specific composables and state holders.

Suggested initial feature split:
- feature-discovery: home/search experience
- feature-downloads: download list and controls
- feature-sources: source management and rescan flows
- feature-settings: settings UI later

Why it moves:
- Feature modules reduce coupling between product areas and make it easier to evolve one area without touching the whole app.
- The current app already has clear feature areas in the UI layer.

Dependency direction:
- Feature modules -> domain, data, core.
- Feature modules should not depend on other feature modules unless there is a true shared composition need.

Risks:
- Moving UI code too early can increase churn without delivering enough architectural gain.
- Navigation and shared state must be coordinated carefully so the app still behaves the same.

---

## 4. Recommended migration sequence

The migration should be incremental and compile-safe.

### Phase A — Prepare the build structure

Goals:
- Introduce empty modules without moving code yet.
- Make the app build with the new module graph.

Actions:
- Create the modules: core, domain, data, feature-discovery, feature-downloads, feature-sources.
- Add the Android library and Kotlin plugin setup for each module.
- Wire the modules in settings.gradle.kts.
- Add module dependencies from the app module to the new modules.

Why this step first:
- It creates a safe scaffold without changing behavior.
- It allows the team to verify the build after each structural change.

Expected outcome:
- The app still builds and the module graph is visible.

---

### Phase B — Move the core layer

Actions:
- Move pure Kotlin utilities and shared constants into core.
- Leave the rest of the app intact.

Why this step second:
- It is the lowest-risk extraction and establishes the foundation for the domain layer.

Expected outcome:
- Core becomes the shared low-level dependency without changing runtime behavior.

---

### Phase C — Extract the domain layer

Actions:
- Move search/filter rules, download preflight rules, and source-scan orchestration into domain use cases.
- Keep the current repositories and services as the implementation underneath.

Why this step third:
- The domain layer is the first real architectural boundary and should sit above infrastructure.

Expected outcome:
- ViewModels become thinner and more UI-focused, while business rules become explicit and testable.

---

### Phase D — Move data/infrastructure code into the data module

Actions:
- Move repositories, Room components, DataStore access, scraping, downloads, storage helpers, and torrent logic into the data module.
- Keep public interfaces stable so the rest of the app does not need to change.

Why this step fourth:
- This is the largest boundary, so it should happen after the domain layer is in place.

Expected outcome:
- Infrastructure becomes encapsulated in a dedicated module and feature code depends on it through abstractions.

---

### Phase E — Split feature modules

Actions:
- Move feature-specific UI packages and ViewModels into feature modules.
- Keep navigation and composition in the app module at first if needed.

Why this step last:
- Feature movement is more visible and more disruptive, so it should happen after the shared layers are stable.

Expected outcome:
- The app becomes a composition shell that assembles features instead of owning them all directly.

---

## 5. Module-by-module migration checklist

### core
- [ ] Introduce module and configure Kotlin-only build
- [ ] Move pure utilities and constants
- [ ] Ensure no Android or Compose dependency leaks in

### domain
- [ ] Introduce module and configure Kotlin-only build
- [ ] Move use cases and domain models
- [ ] Keep dependency on core only

### data
- [ ] Introduce Android library module
- [ ] Move repository implementations, Room database, DataStore, services
- [ ] Keep domain-facing interfaces stable

### feature modules
- [ ] Introduce feature modules for discovery, downloads, and sources
- [ ] Move the relevant screens and ViewModels
- [ ] Keep navigation and app composition in the app module initially

---

## 6. Risks and mitigation

### Risk 1: changing behavior while moving code

Mitigation:
- Move code in small steps.
- Keep public interfaces stable.
- Build after each step.

### Risk 2: accidental dependency leakage

Mitigation:
- Enforce the dependency direction in Gradle and review imports carefully.
- Keep Android and Compose dependencies out of core and domain.

### Risk 3: Hilt and Room wiring becoming harder across modules

Mitigation:
- Keep Hilt modules close to the infrastructure they provide.
- Do not split too many modules at once.
- Use the app module as the top-level composition root.

### Risk 4: too much churn too early

Mitigation:
- Start with core and domain, then data, then features.
- Do not move UI screens until the shared layers are stable.

---

## 7. Definition of done for the migration plan

This migration plan is complete when:

- The app can be split into core, domain, data, and feature modules without a rewrite.
- Each module has a clear responsibility and dependency boundary.
- The dependency direction is explicit and enforceable.
- The app remains buildable after each migration step.

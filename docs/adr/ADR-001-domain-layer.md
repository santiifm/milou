# ADR-001: Why introduce a domain layer?

## Context

Milou already has a strong UI, data, and service layer, but much of the app’s behavior is still expressed through ViewModels, repositories, and service classes. As the product evolves into a more complete retro gaming platform, business rules such as library organization, download orchestration, and source processing will become more complex.

If this logic remains tightly coupled to Android-specific UI and infrastructure code, the system becomes harder to evolve, test, and reason about. The current architecture is good for a first version, but it risks becoming a collection of highly coupled behaviors rather than a coherent platform.

## Decision

Introduce a domain layer that contains the core business rules and concepts of Milou independently from Android UI and infrastructure concerns.

The domain layer will own concepts such as:

- Game
- Console
- Library
- DownloadTask
- SourceScan
- LibraryState

This layer will be used by application use cases and will remain free from Flutter/Android-specific dependencies. In practice, this means moving business logic out of screens and into use cases and domain models that can be tested without UI or framework dependencies.

## Alternatives considered

1. Keep the existing ViewModel-centric approach
   - Simpler for now
   - But business logic will continue to spread across UI and service layers
   - Harder to test and evolve as the app grows

2. Put most business logic in repositories only
   - Repositories are useful for data access, but they are not ideal as the only place for business rules
   - This tends to make repositories too large and too coupled to many responsibilities

3. Introduce a domain layer only later, once the app is larger
   - Delays architectural clarity
   - Increases the cost of future refactoring

## Consequences

Positive:

- Business rules become clearer and easier to test
- The application gains a more stable and maintainable foundation
- The core product logic becomes more reusable across future features
- The system becomes better prepared for emulator integration, library management, and metadata workflows

Negative:

- Requires a refactoring effort to move existing logic out of UI and service layers
- Some existing code will need to be restructured around use cases and domain models
- Initial development may feel slower while the architecture is being clarified

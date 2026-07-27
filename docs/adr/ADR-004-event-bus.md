# ADR-004: Why introduce an EventBus?

## Context

Milou's current architecture relies on direct service and view-model coordination. This works for a small number of features, but as the product grows, more components will need to react to lifecycle events such as download completion, extraction completion, library update, or launch readiness.

Without a shared event model, the system risks turning into a chain of direct calls and scattered side effects. This makes the app harder to extend and harder to keep consistent when new features are introduced.

## Decision

Introduce an event-driven communication mechanism, such as a local EventBus or similar domain event infrastructure.

This will allow Milou to publish domain events such as:

- GameDownloadedEvent
- GameInstalledEvent
- GameStartedEvent
- GameFavoriteEvent

Different parts of the system can subscribe to these events without requiring direct coupling. For example, a library indexer, metadata updater, notification manager, or UI refresh handler can react to a completed download without the downloader needing to know about them explicitly.

## Alternatives considered

1. Keep direct service calls everywhere
   - Simpler in the short term
   - But creates tight coupling and scattered side effects

2. Use only repository-level state updates
   - Helpful for simple state propagation
   - Not enough for decoupled cross-cutting concerns and future extensibility

3. Use a full reactive framework for all communication
   - More complex than needed for the current scope
   - An event bus offers a simpler path for a local-first app

## Consequences

Positive:

- Reduces direct coupling between subsystems
- Makes it easier to add new features without rewriting existing flow logic
- Improves extensibility for notifications, indexing, metadata, and UI updates
- Supports a more platform-like architecture over time

Negative:

- Requires a clear event model and naming convention
- Event handlers must be well designed to avoid hidden side effects
- Some debugging may become less obvious if behavior is distributed across multiple subscribers

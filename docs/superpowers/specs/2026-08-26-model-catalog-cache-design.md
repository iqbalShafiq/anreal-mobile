# Model Catalog Cache and Send Readiness Design

**Status:** Approved for implementation

**Date:** 2026-08-26

## Goal

Make the model and reasoning selector immediately useful on cold start by showing the last locally cached catalog and selection, while keeping every send grounded in a successfully fetched server catalog.

The cache is scoped to the installation. It is not keyed by account because this client currently treats the Room database as an installation-local cache and the requested behavior does not require multi-account isolation.

## Current behavior and problem

`ChatViewModel` starts `ChatRepository.loadCatalog()` in a background coroutine. The repository currently calls `/api/models` directly and does not persist the result. The selected model and reasoning effort are persisted only in `AppPreferencesRepository` (DataStore on Android and an in-memory stub on iOS). A send can therefore race the initial catalog request, use an empty or stale selection, and has no deterministic retry gate after a failed catalog fetch. Invalid persisted selections are currently cleared or dropped without a dedicated model-unavailable dialog, and unsupported reasoning efforts are not reconciled to the nearest lower option.

## Design decisions

### 1. Extend the existing Chat repository boundary

The existing `ChatRepository` remains the single domain-facing owner of chat data. Its catalog API will gain two responsibilities:

- observe the last locally cached catalog for immediate state restoration;
- refresh the catalog from the server and atomically replace the local cache on success.

`OfflineFirstChatRepository` will compose the existing remote data source with Room-backed local catalog storage. `StubChatRepository` and test fakes will implement the same behavior in memory. No separate `ChatCatalogRepository` or cross-feature module will be introduced.

### 2. Room stores normalized catalog data and selection

The Room database will add installation-scoped tables for:

- model rows (`id`, display label, context window, ordering);
- reasoning effort rows (`key`, display label, description, ordering);
- model-to-effort rows (`modelId`, `effortKey`, ordering);
- one singleton catalog metadata/selection row (`lastSuccessfulRefreshEpochMillis`, `selectedModelId`, `selectedReasoningEffort`).

The model-to-effort relation is explicit rather than serialized JSON so the cache remains queryable, migration-safe, and easy to replace atomically. The cache is replaced in one local transaction-like suspend operation: delete old catalog rows, upsert the new normalized rows, then upsert metadata. Empty successful server catalogs are valid and replace the previous cache.

The selected model and effort are written to the same Room metadata row whenever the user changes them or reconciliation changes them. Existing `AppPreferencesRepository` values remain a migration source for one release: on first catalog reconciliation, if the Room selection is empty, the legacy values seed the selection; after that, Room is the source of truth for chat selection. Existing preference setters may continue to receive updates during the transition so other callers do not regress.

### 3. Cache-first startup with a shared refresh gate

On `ChatViewModel` initialization:

1. Observe the cached catalog and cached selection. If present, publish models, reasoning options, and selection immediately with `catalogLoading = false` (or refresh-loading state while content remains visible).
2. Start exactly one background server refresh.
3. Apply reconciliation only after a successful server response.

The ViewModel owns a nullable `catalogRefreshJob` and a mutex/`Deferred`-style shared result so concurrent startup, retry, and send requests await the same refresh rather than issuing duplicate `/api/models` calls. A completed success is reusable for the current process; a send after a failed refresh starts a new refresh.

The cache is a display/readiness optimization, not authorization. A cached catalog never bypasses the server refresh required by a send when the current server result is unknown or the previous refresh failed.

### 4. Send readiness contract

Before creating the optimistic user message or calling `sendMessage`, every new-message path calls `ensureCatalogReadyForSend()`:

- if a successful refresh has already completed and the selected model is valid, continue;
- if startup refresh is still running, await it;
- if no successful refresh exists or the last refresh failed, force one refresh and await it;
- if refresh still fails, leave the draft untouched, do not append an optimistic message, do not call `sendMessage`, and emit a user-facing `ChatEvent.ShowMessage` explaining that the model could not be loaded and can be retried;
- if refresh succeeds but reconciliation reports that the persisted model was removed, do not send and keep the unavailable-model dialog visible until the user dismisses it or chooses a model.

Queue steering for an already active run does not need a model catalog because it uses `/api/chat/steer` and the active run’s existing model. If a queued item falls back to a new `sendMessage` after `NO_ACTIVE_RUN`, it uses the same readiness gate before sending.

### 5. Deterministic reconciliation

Reconciliation is a pure domain function with a testable result:

- Resolve the requested model from the persisted Room selection, falling back to the legacy preference only during migration.
- If the requested model id is not in the server catalog, set both selected model and selected effort to `null`, persist both nulls, and return `modelUnavailable = true` with the removed id/label for dialog copy.
- If the model exists, preserve its effort when the effort is supported by that model.
- If the effort is not supported, select the greatest available effort whose canonical rank is lower than the requested rank. If no lower effort exists, leave effort null rather than silently moving upward. The canonical rank is `none < minimal < low < medium < high < xhigh < max`; unknown keys fall back to catalog order and never outrank a known key.
- The global reasoning-effort list shown in the sheet remains the server catalog list; the selected model’s allowed keys filter the actual choices.

The unavailable-model dialog is shown only for a removed model. An effort downgrade is silent but reflected in the selected chip and persisted selection. If the server model has no reasoning options, the selected effort is null.

### 6. State, actions, and copy

`ChatState` will expose:

- whether the current catalog is served from cache while a refresh is running;
- an unavailable-model dialog payload (removed model label/id) rather than a generic Boolean;
- an explicit send-blocked/loading condition only if needed by the existing composer affordance.

`ChatAction` will gain a dismiss action for the unavailable-model dialog. Choosing a model also clears the dialog payload. The dialog uses the existing M3 dialog wrapper and semantic colors, with sentence-case copy equivalent to: “This model is no longer available. Choose another model to continue.” It has one dismiss/confirm action that returns the user to the model selector; it does not silently pick a different model.

Catalog fetch failures continue to render the model sheet’s retryable error state. Send failures use a dedicated copy equivalent to: “The model list could not be loaded. Check your connection and try again.” All user-facing strings are added through `AnrealCopy` and remain `UiText` values.

### 7. Compatibility and migration

Room schema version increments from 3 to 4 with an explicit auto-migration/schema export for the new tables. Existing sessions, messages, and queues are untouched. Existing DataStore model/effort keys are read once as a fallback and cleared or synchronized after Room selection is established; no destructive preference reset is performed. iOS continues to compile with the existing database actual and in-memory preferences implementation.

`DESIGN.md` will document that the model catalog and last valid selection are installation-local Room cache data, that cached options are allowed during refresh, and that send must await a successful live catalog refresh.

## Data flow

```text
Room catalog Flow ──> ChatViewModel state (instant options/selection)
          │
          └──── startup refresh ──> /api/models
                                  │ success
                                  ├─ normalize + reconcile selection
                                  ├─ replace Room cache
                                  └─ update state / optional unavailable dialog

Composer send ──> ensureCatalogReadyForSend()
                    ├─ await shared refresh
                    ├─ refresh after prior failure
                    ├─ block + ShowMessage on failure
                    └─ sendMessage only after valid live catalog
```

## Testing strategy

1. Pure reconciliation tests cover valid selection, removed model, effort downgrade (`xhigh` → `high`), no lower effort, unknown effort ordering, and empty model options.
2. Room/local mapping tests cover normalized model/effort/relation rows, cache replacement, empty catalog, and selection persistence.
3. Repository tests verify a successful refresh writes the cache and a failed refresh leaves the last good cache visible.
4. ViewModel tests verify cached first paint, startup refresh, model-unavailable dialog state, silent effort downgrade, send waiting for an in-flight refresh, retry after a prior failure, and no remote `sendMessage` call after a second refresh failure.
5. Existing chat, data, and database unit tests plus Android/iOS compilation and the debug APK build remain required.

## Non-goals

- No account-specific catalog partitioning.
- No new server endpoint or API contract.
- No background periodic refresh service; startup, explicit retry, and send readiness are the refresh triggers.
- No automatic replacement of a removed model with an arbitrary server model.
- No UI redesign of the existing model/reasoning sheet beyond the new dialog and loading/error states.

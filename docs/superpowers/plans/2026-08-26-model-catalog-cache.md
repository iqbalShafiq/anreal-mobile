# Model Catalog Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the model catalog and last valid selection in the installation-local Room database, restore it immediately on startup, reconcile it against the live server catalog, and gate every new send on a successful live catalog refresh.

**Architecture:** Keep `ChatRepository` as the domain boundary. Add normalized Room catalog tables and a focused `RoomModelCatalogLocalDataSource`; `OfflineFirstChatRepository` refreshes and caches `/api/models`, while `ChatViewModel` observes the cache, owns one shared refresh gate, reconciles selections through a pure domain function, and renders the unavailable-model dialog. Existing DataStore model keys remain a one-time migration fallback and are synchronized during the transition.

**Tech Stack:** Kotlin Multiplatform, Kotlin coroutines/Flow, Room 3 + bundled SQLite, Koin, Material 3 Compose, existing `Result`/`UiText`/MVI patterns, JUnit 5 + Turbine/AssertK-style tests.

**Spec:** `docs/superpowers/specs/2026-08-26-model-catalog-cache-design.md`

## Global Constraints

- Use Room 3 only (`androidx.room3`); increment `AnrealDatabase` from version 3 to 4 and preserve existing sessions, messages, and queues.
- Feature code uses semantic `MaterialTheme.colorScheme.*`, M3 components, `AnrealSpacing`, `AnrealMotion`, and `UiText`; no hardcoded colors or new glass implementation.
- Keep `ChatRepository` in `feature:chat:domain`; data implementations remain in `feature:chat:data`; presentation depends on domain/core only.
- Keep MVI state/actions/events in `ChatViewModel.kt`; do not parse or reconcile catalog data in composables.
- `sendMessage` must not be called until a live catalog refresh succeeds for the current readiness cycle; cached data may render but cannot authorize a send after a failed/unknown refresh.
- Persist installation-local cache only; do not add account identifiers or a new server endpoint.
- All new user-facing copy is sentence case through `AnrealCopy` and `UiText`.
- Preserve iOS `commonMain` compilation and existing stub/development behavior.
- Every new non-trivial domain/data/ViewModel behavior gets tests before implementation; finish with full tests, Android/iOS compile, and debug APK build.

## File map

| File | Responsibility |
|---|---|
| `feature/chat/domain/.../ChatCatalog.kt` | Catalog cache snapshot, reconciliation result, canonical effort ranking, pure reconciliation function. |
| `feature/chat/domain/.../ChatRepository.kt` | Cached-catalog observation, server refresh, and selection persistence contract. |
| `core/database/.../ModelCatalogEntity.kt` | Room entities and domain mapping for normalized model/effort/relation/metadata rows. |
| `core/database/.../ModelCatalogDao.kt` | Room queries, flows, selection upsert, and catalog replacement primitives. |
| `core/database/.../AnrealDatabase.kt` | Version 4 entities, auto-migration, and DAO accessor. |
| `core/database/.../DatabaseModule.*.kt` | Koin binding for the new DAO on Android and iOS. |
| `feature/chat/data/.../RoomModelCatalogLocalDataSource.kt` | Room-to-domain snapshot mapping and cache replacement orchestration. |
| `feature/chat/data/.../OfflineFirstChatRepository.kt` | Remote refresh + local catalog persistence, while retaining existing chat behavior. |
| `feature/chat/data/.../ChatDataModule.kt` | Koin binding and constructor wiring for the catalog local source. |
| `feature/chat/data/.../StubChatRepository.kt` | In-memory cached catalog behavior for development. |
| `feature/chat/presentation/.../ChatViewModel.kt` | Cache-first startup, shared refresh gate, selection migration/reconciliation, send readiness, dialog state/actions. |
| `feature/chat/presentation/.../component/ModelUnavailableDialog.kt` | M3 dialog for a removed persisted model with preview. |
| `feature/chat/presentation/.../ChatScreen.kt` | Mount the dialog from `ChatState`. |
| `core/presentation/.../AnrealCopy.kt` | Dialog and send-refresh error keys/copy. |
| `DESIGN.md` | Document installation-local catalog cache and send readiness rule. |
| `feature/chat/domain/src/commonTest/.../ChatCatalogTest.kt` | Pure reconciliation tests. |
| `core/database/src/androidHostTest/.../ModelCatalogDaoTest.kt` | Room cache replacement and selection persistence tests. |
| `feature/chat/data/src/commonTest/.../RoomModelCatalogLocalDataSourceTest.kt` | Normalized row/domain mapping tests with a fake DAO or test database seam. |
| `feature/chat/presentation/src/commonTest/.../ChatViewModelTest.kt` | Startup cache, refresh, dialog, downgrade, send gate, retry, and failure tests. |
| `feature/chat/presentation/src/commonTest/.../FakeChatRepository.kt` | Fake cache Flow, refresh controls, and send-call assertions. |

### Task 1: Add pure catalog reconciliation

**Files:**
- Modify: `feature/chat/domain/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/domain/ChatCatalog.kt`
- Create: `feature/chat/domain/src/commonTest/kotlin/co/ratmo/anreal/feature/chat/domain/ChatCatalogTest.kt`

**Interfaces:**
- Produces `data class CachedModelCatalog(val catalog: ModelCatalog, val selectedModelId: String?, val selectedReasoningEffort: String?, val lastSuccessfulRefreshEpochMillis: Long?)`.
- Produces `data class CatalogSelectionResolution(val selectedModelId: String?, val selectedReasoningEffort: String?, val modelUnavailable: Boolean, val unavailableModelId: String?, val unavailableModelLabel: String?)`.
- Produces `fun reconcileCatalogSelection(catalog: ModelCatalog, requestedModelId: String?, requestedReasoningEffort: String?): CatalogSelectionResolution`.

- [ ] **Step 1: Write failing tests** for: a valid model/effort is preserved; no persisted model selects the first server model; a removed model clears both values and reports its id/label; `xhigh` falling out of a model with `high` and `low` selects `high`; no lower effort clears the effort; `none`/null stays null; unknown effort keys use catalog order without outranking known keys.
- [ ] **Step 2: Run the focused domain test** with `.\gradlew.bat :feature:chat:domain:test --tests '*ChatCatalogTest' --console=plain`; verify the new tests fail because the resolution types/function do not exist.
- [ ] **Step 3: Implement the resolution types and pure function**. Use canonical ranks `none=0`, `minimal=1`, `low=2`, `medium=3`, `high=4`, `xhigh=5`, `max=6`; preserve an exact supported effort; otherwise choose the greatest available known rank strictly below the requested rank, or the greatest lower catalog-order candidate for unknown keys; return null when no lower option exists.
- [ ] **Step 4: Re-run the focused test** and then `.\gradlew.bat :feature:chat:domain:test --console=plain`; verify all domain tests pass.
- [ ] **Step 5: Commit** with `git add feature/chat/domain && git commit -m "feat: add catalog selection reconciliation"`.

### Task 2: Add normalized Room catalog storage and schema migration

**Files:**
- Create: `core/database/src/commonMain/kotlin/co/ratmo/anreal/core/database/ModelCatalogEntity.kt`
- Create: `core/database/src/commonMain/kotlin/co/ratmo/anreal/core/database/ModelCatalogDao.kt`
- Modify: `core/database/src/commonMain/kotlin/co/ratmo/anreal/core/database/AnrealDatabase.kt`
- Modify: `core/database/src/androidMain/kotlin/co/ratmo/anreal/core/database/DatabaseModule.android.kt`
- Modify: `core/database/src/iosMain/kotlin/co/ratmo/anreal/core/database/DatabaseModule.ios.kt`
- Create: `core/database/src/androidHostTest/kotlin/co/ratmo/anreal/core/database/ModelCatalogDaoTest.kt`

**Interfaces:**
- `ModelCatalogModelEntity(id: String, label: String, contextWindowTokens: Int, position: Int)`.
- `ModelCatalogEffortEntity(key: String, label: String, description: String?, position: Int)`.
- `ModelCatalogModelEffortEntity(modelId: String, effortKey: String, position: Int)`.
- `ModelCatalogMetadataEntity(id: Int = 1, lastSuccessfulRefreshEpochMillis: Long?, selectedModelId: String?, selectedReasoningEffort: String?)`.
- `ModelCatalogDao` exposes Flow queries for all four row sets, `replaceCatalog(...)`, and `upsertSelection(modelId, reasoningEffort)`.

- [ ] **Step 1: Write the Android host DAO test** against an in-memory `AnrealDatabase`: replace a two-model catalog, assert model/effort/relation rows and metadata are present; replace with an empty catalog and assert old rows disappear; upsert selection and assert metadata changes without touching catalog rows.
- [ ] **Step 2: Run `.\gradlew.bat :core:database:testDebugUnitTest --tests '*ModelCatalogDaoTest' --console=plain`** and confirm the test fails because the entities/DAO/database accessor are absent.
- [ ] **Step 3: Implement the four Room entities, mapper extensions, DAO queries, and replacement primitives**. Keep table names explicit (`model_catalog_models`, `model_catalog_efforts`, `model_catalog_model_efforts`, `model_catalog_metadata`), use `@Upsert`, delete catalog rows before inserts, and keep selection metadata in the singleton row.
- [ ] **Step 4: Update `AnrealDatabase` to version 4**, include the four entities, add `AutoMigration(from = 3, to = 4)`, expose `modelCatalogDao()`, and bind it in both platform database modules.
- [ ] **Step 5: Run the DAO test plus `.\gradlew.bat :core:database:testDebugUnitTest --console=plain`**; verify schema generation and all database tests pass.
- [ ] **Step 6: Commit** with `git add core/database && git commit -m "feat: persist model catalog in Room"`.

### Task 3: Connect Room cache to the chat data/domain boundary

**Files:**
- Modify: `feature/chat/domain/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/domain/ChatRepository.kt`
- Create: `feature/chat/data/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/data/RoomModelCatalogLocalDataSource.kt`
- Modify: `feature/chat/data/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/data/OfflineFirstChatRepository.kt`
- Modify: `feature/chat/data/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/data/ChatDataModule.kt`
- Modify: `feature/chat/data/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/data/StubChatRepository.kt`
- Modify: `feature/chat/presentation/src/commonTest/kotlin/co/ratmo/anreal/feature/chat/presentation/FakeChatRepository.kt`
- Create/modify tests under `feature/chat/data/src/commonTest/kotlin/co/ratmo/anreal/feature/chat/data/`

**Interfaces:**
- Replace `suspend fun loadCatalog()` with `fun observeCachedCatalog(): Flow<CachedModelCatalog?>` and `suspend fun refreshCatalog(): Result<ModelCatalog, ChatError>`.
- Add `suspend fun persistCatalogSelection(modelId: String?, reasoningEffort: String?)`.
- `RoomModelCatalogLocalDataSource` maps DAO rows into a `CachedModelCatalog`, replaces a `ModelCatalog` while preserving current selection metadata, and updates selection metadata.

- [ ] **Step 1: Add repository/data tests** that assert a successful remote refresh is observable from the local snapshot, a failed refresh leaves the last successful snapshot unchanged, and selection persistence updates the snapshot metadata.
- [ ] **Step 2: Run `.\gradlew.bat :feature:chat:data:test --console=plain`** and confirm failures identify the new contract and local data source as missing.
- [ ] **Step 3: Implement `RoomModelCatalogLocalDataSource`** using `ModelCatalogDao`; combine the four DAO flows into one snapshot, return null when metadata and model rows are absent, map relation rows into each model’s `reasoningEfforts`, and preserve global effort order.
- [ ] **Step 4: Update `OfflineFirstChatRepository`** so `refreshCatalog()` calls `remote.loadCatalog()`, writes the normalized catalog only on success, and returns the server catalog; expose the local Flow and selection writer. Keep existing session/message behavior untouched.
- [ ] **Step 5: Update `StubChatRepository`, `FakeChatRepository`, and Koin `chatDataModule`**. The stub should expose its built-in catalog immediately and treat refresh as a successful in-memory cache replacement; the fake should allow a deferred refresh result and record send calls for later gating tests.
- [ ] **Step 6: Run `.\gradlew.bat :feature:chat:data:test --console=plain` and `.\gradlew.bat :feature:chat:presentation:compileAndroidMain --console=plain`** to catch all interface and Koin wiring errors.
- [ ] **Step 7: Commit** with `git add feature/chat/domain feature/chat/data && git commit -m "feat: add cached catalog repository flow"`.

### Task 4: Make ChatViewModel cache-first and send-safe

**Files:**
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/ChatViewModel.kt`
- Modify: `feature/chat/presentation/src/commonTest/kotlin/co/ratmo/anreal/feature/chat/presentation/ChatViewModelTest.kt`
- Modify: `feature/chat/presentation/src/commonTest/kotlin/co/ratmo/anreal/feature/chat/presentation/FakeChatRepository.kt`

**Interfaces:**
- Add `data class ModelUnavailableUi(val modelId: String, val label: String)` to presentation UI models.
- Add `ChatState.catalogFromCache: Boolean` and `ChatState.modelUnavailable: ModelUnavailableUi?`.
- Add `ChatAction.OnDismissModelUnavailable`.
- Add private suspend methods `observeCachedCatalog()`, `refreshCatalog(force: Boolean)`, `applyCatalog(catalog, requestedModelId, requestedReasoningEffort, showUnavailableDialog)`, and `ensureCatalogReadyForSend(): Boolean`.

- [ ] **Step 1: Add failing ViewModel tests** for: cached catalog appears before a deferred refresh completes; live refresh clears `catalogFromCache`; removed model clears both selections and exposes `modelUnavailable`; unsupported `xhigh` downgrades to `high`; a send waits until a startup refresh Deferred completes; a failed startup refresh is retried by send; a second failure emits `ShowMessage` and leaves `fake.sentOptions` null/draft intact.
- [ ] **Step 2: Run the focused test** with `.\gradlew.bat :feature:chat:presentation:test --tests '*ChatViewModelTest' --console=plain`; verify the new tests fail because cache observation/gating/dialog state are not implemented.
- [ ] **Step 3: Add cache observation in `init`** before startup refresh. Immediately map a non-null `CachedModelCatalog` into state and migrate legacy `AppPreferences` selection only when the Room snapshot has no selection. Keep cached options visible while `catalogLoading` is true and mark `catalogFromCache = true`.
- [ ] **Step 4: Implement a single shared refresh Deferred guarded by `Mutex`**. Startup calls `refreshCatalog(force = true)`, retry calls force true, and send calls force true only when no successful live refresh exists. Keep prior cache on failure, set `catalogError`, and mark the live readiness false. Clear the shared Deferred after completion so a later failure can retry.
- [ ] **Step 5: On refresh success, call `reconcileCatalogSelection`, persist the resolved selection through `ChatRepository.persistCatalogSelection` and legacy preference setters, update models/efforts/selection atomically in state, and populate `modelUnavailable` only when the model was removed. Do not auto-select an arbitrary replacement after a removed model.
- [ ] **Step 6: Wire model/effort selection actions** to persist both Room and legacy preference values; selecting a model preserves the current effort only when supported and otherwise applies the nearest lower effort rule. Dismiss clears only the dialog payload; choosing a new model also clears it.
- [ ] **Step 7: Call `ensureCatalogReadyForSend()` at the start of `sendText()` before optimistic UI/cache writes. If it returns false, leave the draft untouched and do not call the remote send. Keep queue steering unchanged, but the `NO_ACTIVE_RUN` fallback naturally re-enters `sendText()` and therefore the gate.
- [ ] **Step 8: Run focused ViewModel tests, all chat presentation tests, and `.\gradlew.bat :feature:chat:presentation:compileKotlinIosArm64 --console=plain`**; fix all coroutine cancellation, state race, and interface issues.
- [ ] **Step 9: Commit** with `git add feature/chat/presentation && git commit -m "feat: gate chat sends on live catalog"`.

### Task 5: Add the unavailable-model dialog and copy

**Files:**
- Create: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/component/ModelUnavailableDialog.kt`
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/ChatScreen.kt`
- Modify: `core/presentation/src/commonMain/kotlin/co/ratmo/anreal/core/presentation/AnrealCopy.kt`
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/preview/ChatPreviewFixtures.kt`
- Modify/create screenshot tests under `feature/chat/presentation/src/androidHostTest/`

**Interfaces:**
- `ModelUnavailableDialog(model: ModelUnavailableUi, onAction: (ChatAction) -> Unit)` renders an `AlertDialog` with title/body/confirm action from `AnrealCopy` and the removed model label as `{0}`.

- [ ] **Step 1: Add a screenshot/preview state** with `modelUnavailable = ModelUnavailableUi("removed", "Old model")` and assert the dialog mounts in `ChatScreen`.
- [ ] **Step 2: Run the affected screenshot/compile test** and confirm the missing dialog/copy symbols fail compilation.
- [ ] **Step 3: Add copy keys and sentence-case strings**: a model-unavailable title, body with `{0}`, a “Choose model” action, and a send-refresh failure message. Keep dialog colors/default typography semantic and use existing M3 `AlertDialog`/`TextButton` conventions.
- [ ] **Step 4: Mount the dialog after other chat dialogs in `ChatScreen`**, pass the UI payload, and add light/dark previews. The confirm/dismiss action returns the user to the existing model sheet trigger path without inventing a new navigation route.
- [ ] **Step 5: Run `.\gradlew.bat :feature:chat:presentation:compileAndroidMain --console=plain` and the chat screenshot tests**; verify accessibility labels and visual state.
- [ ] **Step 6: Commit** with `git add feature/chat/presentation core/presentation && git commit -m "feat: explain unavailable chat models"`.

### Task 6: Update design documentation and migration notes

**Files:**
- Modify: `DESIGN.md`
- Modify: `docs/superpowers/specs/2026-08-26-model-catalog-cache-design.md` only if implementation reveals a contract correction

- [ ] **Step 1: Add the final contract to `DESIGN.md`** near the model-sheet/persistence rules: catalog and last selection are installation-local Room cache data, cached options render while refreshing, live refresh is mandatory before a new send, removed models show the explicit dialog, and missing efforts downgrade only downward.
- [ ] **Step 2: Run `rg -n "model catalog|catalog|reasoning|send" DESIGN.md docs/superpowers/specs/2026-08-26-model-catalog-cache-design.md`** and manually verify the docs do not contradict the implementation or AGENTS.md.
- [ ] **Step 3: Commit** with `git add DESIGN.md docs/superpowers/specs && git commit -m "docs: document catalog cache readiness"`.

### Task 7: Full verification and handoff

**Files:**
- No new source files; verify the complete change set and generated Room schema under `core/database/schemas/`.

- [ ] **Step 1: Run focused regression tests**: `.\gradlew.bat :feature:chat:domain:test :feature:chat:data:test :feature:chat:presentation:test :core:database:testDebugUnitTest --console=plain`.
- [ ] **Step 2: Run the required compile checks**: `.\gradlew.bat :feature:chat:presentation:compileAndroidMain :feature:chat:presentation:compileKotlinIosArm64 --console=plain`.
- [ ] **Step 3: Run the full test suite** with `.\gradlew.bat test --console=plain`; verify no warnings-as-errors or unrelated regressions.
- [ ] **Step 4: Build the debug APK** with `.\gradlew.bat :app:assembleDebug --console=plain`.
- [ ] **Step 5: Inspect `git diff --check`, `git status --short`, and the generated Room schema**; confirm only intended files are changed and the pre-existing `gradle.properties` modification is preserved.
- [ ] **Step 6: If a device is connected, install with `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk` and manually exercise: cached selector first paint, model removal dialog, effort downgrade, send while refresh is pending, and send after a forced refresh failure.
- [ ] **Step 7: Commit any generated schema/verification-only source adjustments** with a focused message, then report test/build/device evidence and any remaining limitations.


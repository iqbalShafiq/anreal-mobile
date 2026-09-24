# Workspace Scope, Artifacts, Tasks & Schedules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task (NATIVE ONLY — user forbids subagents). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adopt backend sites-scope browser, `artifact_focus` stream event, artifacts read API, tasks CRUD, and schedules CRUD into `:feature:workspace` (browser) + minimal `:feature:chat` stream/session wiring.

**Architecture:** Extend `feature:workspace:{domain,data,presentation}` (models + `WorkspaceRepository`/`KtorWorkspaceRepository` + browser sections) and `feature:chat` (focus event + reducer state, `sessionId` site field, `GET session`). Cross-feature via `:app` lambdas only. Sequential slices, each independently testable.

**Tech Stack:** Kotlin Multiplatform, Ktor Client + KotlinX Serialization, Koin, DataStore/FileKit (existing), Material 3 Expressive, JUnit 5 + Turbine + AssertK + MockEngine, Roborazzi.

**Spec:** `docs/superpowers/specs/2026-09-24-workspace-scope-artifacts-tasks-design.md`

## Global Constraints

- Import types; never fully-qualified names in expressions; `import foo.Bar as Baz` only on simple-name clash.
- Prefer `when (result)` / `onSuccess` / `onFailure` over `as Result.Success`.
- `_state.update { it.copy(...) }` only; UI models end in `Ui`; domain errors through `toUiText()`; dynamic-only strings stay `String`.
- User-facing strings are resources (`AnrealCopy` pattern); Symbols Rounded; semantic `colorScheme`/`typography`; `AnrealSpacing`/4dp; `AnrealMotion` via `graphicsLayer`; never `Modifier.blur`; never hardcode `#E8A317`/black fills; 48.dp targets; reduced motion/transparency.
- Never inject `CoroutineDispatcher` unless unit-tested AND off-Main; ViewModel tests use `Dispatchers.setMain(UnconfinedTestDispatcher())`; fakes over mocks.
- Rethrow `CancellationException`; expected failures → `Result.Error`; decode non-2xx first; 4xx user-safe, 5xx generic; wrap network errors as `WorkspaceError.Network` / `ChatError.Network` per module.
- `commonMain` iOS-compilable; Android WebView/FileKit real, iOS TODO stubs.
- Versions only in `gradle/libs.versions.toml`; no invented endpoints; Bearer existing; preview URLs never send `Authorization`.
- Warnings are errors; no unused params/imports; no unexplained `!!`.
- Presentation must not depend on `core:data`; features never depend on each other; DI assembled in `:app` only; `koinViewModel()` in Roots only.
- Unit tests per touched module: `.\gradlew.bat :feature:workspace:data:testAndroidHostTest :feature:workspace:presentation:testAndroidHostTest :feature:chat:domain:testAndroidHostTest :feature:chat:data:testAndroidHostTest --console=plain`. Compile: `...:compileAndroidMain ...:compileKotlinIosArm64 --console=plain`.
- NATIVE execution only. Commit ONLY task files; never touch `gradle.properties`/`design-resarch/`.

## Review Focus

- Scope browser opened with a session the server no longer knows → expect user-safe `Session not found` + retry affordance, never a crash or spinner forever. Pinned by Task 2's `scope_unknown_session_shows_retry` test.
- `artifactFocus` arrives with an 8th unknown type → expect silent `Unknown` (no event, no error UI). Pinned by Task 1's `focus_unknown_type_maps_unknown` test.
- Task editor Save tapped with zero changes → expect client-side block (`Nothing to update` never hits network). Pinned by Task 3's `update_empty_blocked_client_side` test.
- Schedule created with past `runAt` → expect server decides; client shows server message verbatim. Pinned by Task 4's `schedule_past_run_shows_server_message` test.
- Caption edit on a deleted image (404) → expect error event, editor stays open with text preserved. Pinned by Task 5's `caption_404_keeps_editor` test.

---

## File Structure

**Workspace domain** (`feature/workspace/domain/.../domain/`): extend `WorkspaceModels.kt` (`WorkspaceTask`, `TaskSubtask`, `WorkspaceSchedule`, `ArtifactItem`, `ScopeSiteEntry`) + `WorkspaceRepository.kt` (new methods).
**Workspace data** (`feature/workspace/data/.../data/`): extend `WorkspaceDtos.kt` (DTOs + `toDomain()` mappers) + `KtorWorkspaceRepository.kt` (methods, `mapWorkspaceError()`); extend `StubWorkspaceRepository.kt`.
**Workspace presentation** (`.../presentation/`): extend `WorkspaceScreen.kt` (new sections), `WorkspaceViewModel.kt` (state/actions), `WorkspaceGraph.kt` (no new routes needed — `WorkspaceRoute(section)` already parametric; add `WorkspaceSection` entries), new `component/TasksSheets.kt`, `SchedulesSheets.kt`, `ScopeSitesPanel.kt`, `ArtifactsBrowser.kt` as needed.
**Chat**: `domain/stream/ChatModels.kt` (+`ArtifactFocus` event, `pendingArtifactFocus` state), `domain/stream/ChatStreamParser.kt` (dispatch), `domain/stream/ChatReducer.kt` (set/clear), `data/.../SitesDtos.kt` (+`sessionId`), `data/.../KtorChatRemoteDataSource.kt` (+`getSession`), `domain/.../ChatRepository.kt` (+`getSession` if absent — read first), presentation focus-forward event + `:app` lambda.

---

### Task 1: Focus event + sessionId + GET session (chat)

**Files:**
- Modify: `feature/chat/domain/.../domain/stream/ChatModels.kt`, `.../stream/ChatStreamParser.kt`, `.../stream/ChatReducer.kt`
- Modify: `feature/chat/data/.../data/SitesDtos.kt` (`SessionSiteEntryDto.sessionId = ""` + mapper), `.../data/KtorChatRemoteDataSource.kt` (+`getSession`)
- Modify: `feature/chat/domain/.../domain/ChatRepository.kt` (+`getSession` — read file first; add only if absent), `.../data/OfflineFirstChatRepository.kt` + stubs if the interface grows
- Test: `feature/chat/domain/.../stream/ArtifactFocusStreamTest.kt`, extend `KtorChatRemoteDataSourceTest.kt` or new `ChatSessionDetailTest.kt`

**Interfaces:**
- Consumes: Task 6 (first plan) helpers — reuse `stableSiteUrls`.
- Produces: `ChatStreamEvent.ArtifactFocus`, `ChatThreadState.pendingArtifactFocus`, `ChatSessionDetail(sessionId,projectId,title,updatedAt)` for Tasks 2, 6.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun focus_valid_maps() {
    val event = parseStreamLine("""{"type":"data","name":"artifactFocus","data":{"artifactId":"t1","artifactType":"task","label":"Fix login"}}""")
    val focus = (event as StreamEnvelope.Event).event as ChatStreamEvent.ArtifactFocus
    assertThat(focus.artifactType).isEqualTo("task")
}

@Test
fun focus_unknown_type_maps_unknown() {
    val event = parseStreamLine("""{"type":"data","name":"artifactFocus","data":{"artifactId":"x","artifactType":"teleporter"}}""")
    assertThat((event as StreamEnvelope.Event).event is ChatStreamEvent.Unknown).isTrue()
}

@Test
fun focus_extra_key_maps_unknown() {
    val event = parseStreamLine("""{"type":"data","name":"artifactFocus","data":{"artifactId":"x","artifactType":"task","hax":1}}""")
    assertThat((event as StreamEnvelope.Event).event is ChatStreamEvent.Unknown).isTrue()
}

@Test
fun session_entry_old_payload_defaults_session_id() {
    val dto = Json.decodeFromString<SessionSiteEntryDto>(
        """{"siteId":"s1","version":1,"stableVersion":1,"status":"ready","downloadUrl":"/api/sites/s1/v1/download","updatedAt":"t"}""",
    )
    assertThat(dto.toEntry().sessionId).isEqualTo("")
}
```

(Adjust `parseStreamLine` envelope shape to the real one verified in the first plan's Task 8 — outer `stream_event` envelopes.)

- [ ] **Step 2: Run to verify fail**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest :feature:chat:data:testAndroidHostTest --console=plain`
Expected: FAIL `Unresolved reference: ArtifactFocus`.

- [ ] **Step 3: Minimal implementation**

```kotlin
// ChatModels.kt — in ChatStreamEvent, before Unknown:
data class ArtifactFocus(val artifactId: String, val artifactType: String, val label: String? = null) : ChatStreamEvent
// ChatThreadState += :
val pendingArtifactFocus: ArtifactFocus? = null,
```

Parser — in `parseDataEvent`'s `when (event.string("name"))`, add branch (strict keys exactly `artifactId,artifactType,label`):

```kotlin
"artifactFocus" -> {
    val data = event["data"] as? JsonObject ?: return ChatStreamEvent.Unknown(type)
    if (data.keys != setOf("artifactId", "artifactType") && data.keys != setOf("artifactId", "artifactType", "label")) {
        return ChatStreamEvent.Unknown(type)
    }
    val artifactType = data.string("artifactType") ?: return ChatStreamEvent.Unknown(type)
    if (artifactType !in setOf("document", "image", "web_bundle", "site", "task", "schedule", "session")) {
        return ChatStreamEvent.Unknown(type)
    }
    ChatStreamEvent.ArtifactFocus(
        artifactId = data.string("artifactId") ?: return ChatStreamEvent.Unknown(type),
        artifactType = artifactType,
        label = data.string("label"),
    )
}
```

Reducer:

```kotlin
is ChatStreamEvent.ArtifactFocus -> advanced.copy(
    pendingArtifactFocus = ArtifactFocusFocus(artifactId = event.artifactId, artifactType = event.artifactType, label = event.label),
)
```

Name the domain hold type `ArtifactFocusState(artifactId, artifactType, label)` in `ChatModels.kt` (avoids clash with the event class). Task 6 clears it after forwarding.

DTO + session:

```kotlin
// SessionSiteEntryDto += : val sessionId: String = "",
// toEntry += : sessionId = sessionId,
```

```kotlin
@Serializable
data class ChatSessionDetailDto(val sessionId: String, val projectId: String? = null, val title: String = "", val updatedAt: String = "")
data class ChatSessionDetail(val sessionId: String, val projectId: String?, val title: String, val updatedAt: String)
fun ChatSessionDetailDto.toDetail() = ChatSessionDetail(sessionId, projectId, title, updatedAt)

// KtorChatRemoteDataSource:
suspend fun getSession(id: String): Result<ChatSessionDetail, ChatError> =
    httpClient.get<ChatSessionDetailDto>(route = "/api/chat/sessions/$id").map { it.toDetail() }.mapNetwork()
```

Repository: read `ChatRepository.kt` first — add `getSession` there + pass-through in `OfflineFirstChatRepository` + stubs only if the interface lacks it.

- [ ] **Step 4: Run to verify pass**

Run: same as Step 2.
Expected: PASS, zero warnings, existing parser/reducer suites green.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/domain/src/commonMain/.../stream/ChatModels.kt feature/chat/domain/src/commonMain/.../stream/ChatStreamParser.kt feature/chat/domain/src/commonMain/.../stream/ChatReducer.kt feature/chat/domain/src/commonTest/.../stream/ArtifactFocusStreamTest.kt feature/chat/data/src/commonMain/.../data/SitesDtos.kt feature/chat/data/.../KtorChatRemoteDataSource.kt [repository files if touched] [session test file]
git commit -m "feat(chat): artifact focus event, site session id, get session"
```

### Task 2: Sites scope source + workspace browser entry

**Files:**
- Modify: `feature/workspace/domain/.../domain/WorkspaceModels.kt` (`ScopeSiteEntry`, `SiteStatus` reuse chat's? NO — workspace has no chat dep; define local `ScopeSiteStatus { Queued, Running, Ready, Failed }`), `.../WorkspaceRepository.kt` (`listScopeSites(sessionId)`, `openSiteOrigin` stays in chat/VM — browser calls back via lambda)
- Modify: `feature/workspace/data/.../data/WorkspaceDtos.kt` (`ScopeSiteEntryDto` + `toScopeEntry()`), `.../KtorWorkspaceRepository.kt` (`GET /api/sites` with `sessionId` query), `StubWorkspaceRepository.kt`
- Modify: `feature/workspace/presentation/.../WorkspaceScreen.kt` (Sites section entry), `WorkspaceViewModel.kt` (sites state: loading/list/error/retry), new `component/ScopeSitesPanel.kt` (cards + Preview/Download/origin/continue actions as callbacks)
- Test: `KtorWorkspaceRepositoryTest.kt` (+scope 200/400/404), VM test (+`scope_unknown_session_shows_retry`)

**Interfaces:**
- Consumes: Task 1 `ChatSessionDetail` + `getSession` (for origin-open, wired in Task 6).
- Produces: `ScopeSiteEntry`, `listScopeSites` for Task 6 origin flow.

- [ ] **Step 1: Write failing tests** — scope 200 maps entries incl. `sessionId`; 400 keeps message; 404 maps NOT_FOUND; VM unknown-session shows retry (fake repo returning 404).

- [ ] **Step 2: Run to verify fail**

Run: `.\gradlew.bat :feature:workspace:data:testAndroidHostTest :feature:workspace:presentation:testAndroidHostTest --console=plain`
Expected: FAIL unresolved `listScopeSites`.

- [ ] **Step 3: Minimal implementation**

```kotlin
// Domain:
enum class ScopeSiteStatus { Queued, Running, Ready, Failed }
data class ScopeSiteEntry(
    val siteId: String, val sessionId: String = "", val version: Int,
    val stableVersion: Int?, val status: ScopeSiteStatus,
    val previewUrl: String?, val downloadUrl: String, val updatedAt: String,
)
// Repository += :
suspend fun listScopeSites(sessionId: String): Result<List<ScopeSiteEntry>, WorkspaceError>
```

```kotlin
// DTOs:
@Serializable
data class ScopeSiteEntryDto(
    val siteId: String, val sessionId: String = "", val version: Int,
    val stableVersion: Int? = null, val status: String = "queued",
    val previewUrl: String? = null, val downloadUrl: String = "", val updatedAt: String = "",
)
@Serializable
data class ScopeSitesDto(val sites: List<ScopeSiteEntryDto> = emptyList())
fun ScopeSiteEntryDto.toScopeEntry(): ScopeSiteEntry = ScopeSiteEntry(
    siteId, sessionId, version, stableVersion,
    when (status) { "running" -> ScopeSiteStatus.Running; "ready" -> ScopeSiteStatus.Ready; "failed" -> ScopeSiteStatus.Failed; else -> ScopeSiteStatus.Queued },
    previewUrl, downloadUrl, updatedAt,
)
// Repo:
override suspend fun listScopeSites(sessionId: String): Result<List<ScopeSiteEntry>, WorkspaceError> =
    httpClient.get<ScopeSitesDto>(route = "/api/sites", queryParameters = mapOf("sessionId" to sessionId))
        .map { dto -> dto.sites.map { it.toScopeEntry() } }
        .mapError { WorkspaceError.Network(it) }
```

(Verify `mapWorkspaceError` helper name/shape in `KtorWorkspaceRepository.kt` first — mirror neighbors, e.g. `.mapWorkspaceError()` vs `.mapError { WorkspaceError.Network(it) }`.)

VM + panel: sites section state (`loading/list/error`), `OnSitesRetry`; panel cards reuse chat `SiteBuildPanel` visual language (Surface extraLarge, steps NOT needed — browser shows status + Preview/Download/origin/continue buttons only; history stays in chat). Preview = WebView sandbox reuse (`SitePreviewWebView` is chat-internal `expect` — do NOT import across features; duplicate the 25-line actual per platform OR hoist to `core:presentation` ONLY if a second consumer appears — v1: duplicate with a comment, same as plan allows).

- [ ] **Step 4: Run to verify pass** — same command. Expected: PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat(workspace): sites scope source and browser entry"`.

### Task 3: Tasks CRUD

**Files:** Same workspace files + new `component/TasksSheets.kt`; tests in repo test + `WorkspaceViewModelTest.kt`.

**Interfaces:** Consumes: scope-via-`sessionId` convention (active session id threaded from VM state — read `WorkspaceViewModel.kt` for how projectId/session context is held first).

- [ ] **Step 1: Failing tests** — DTO unknown-keys + defaults; create 201 maps `{id,title,status}`; patch empty → client-side block test (`update_empty_blocked_client_side`: VM `OnTaskSave` with no changes makes NO network call — assert fake repo untouched); delete 404; toggle subtask maps.

- [ ] **Step 2: Fail run** — workspace data+presentation tests. Expected: FAIL unresolved `listTasks`.

- [ ] **Step 3: Implementation**

```kotlin
// Domain:
enum class TaskStatus { Inbox, Doing, Done }
data class TaskSubtask(val id: String, val title: String, val done: Boolean)
data class WorkspaceTask(
    val id: String, val title: String, val status: TaskStatus = TaskStatus.Inbox,
    val description: String? = null, val subtasks: List<TaskSubtask> = emptyList(),
    val sourceSessionId: String? = null, val dueAt: String? = null,
)
// Repository += :
suspend fun listTasks(sessionId: String): Result<List<WorkspaceTask>, WorkspaceError>
suspend fun createTask(sessionId: String, title: String, description: String?, subtasks: List<String>, dueAt: String?): Result<WorkspaceTask, WorkspaceError>
suspend fun updateTask(sessionId: String, id: String, status: TaskStatus?, title: String?, description: String?, addSubtasks: List<String>, toggleSubtasks: List<Pair<String, Boolean>>, removeSubtasks: List<String>): Result<WorkspaceTask, WorkspaceError>
suspend fun deleteTask(sessionId: String, id: String): EmptyResult<WorkspaceError>
```

DTOs mirror backend zod exactly (`addSubtasks/toggleSubtasks[{id,done}]/removeSubtasks`, `description` nullable on update, status wire `inbox|doing|done`). Client pre-validation mirrors service limits (title 1..200, description ≤2000, ≤50 subtasks). VM blocks empty updates before network.

UI: Tasks section list + editor modal (title/description/due/subtask checklist + progress `done/total` + status segmented Inbox/Doing/Done), delete confirm. Previews all states.

- [ ] **Step 4: Pass run** — same commands. Expected: PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat(workspace): tasks crud"`.

### Task 4: Schedules CRUD

Same files + `component/SchedulesSheets.kt`. `WorkspaceSchedule(id,title,prompt,freq Once|Daily|Weekly,nextRunAt?,status)`; repo `listSchedules(sessionId)`, `createSchedule(sessionId,title,prompt,freq,runAt?)`, `cancelSchedule(sessionId,id)` (DELETE → `{ok:true}`, server cancels job). Tests: list/create/400-invalid/404 + `schedule_past_run_shows_server_message` (server message surfaced verbatim via `toUiText()` DynamicString path). UI: list + editor (title/prompt/freq select/runAt) + cancel confirm. Steps mirror Task 3 (fail → impl → pass → commit `feat(workspace): schedules crud`).

### Task 5: Artifacts list/detail + caption

Same files + `component/ArtifactsBrowser.kt`. `ArtifactItem(type: ArtifactType(document|image|web_bundle|site|task|schedule|session), id, title?, caption?, payload: JsonObject? = null)` — keep payload generic (`JsonObject`) for v1, typed accessors only where UI needs them. Repo: `listArtifacts(sessionId, type?, q?)`, `getArtifact(sessionId, type, id)`, `updateImageCaption(sessionId, imageId, caption)`. `DocumentDto` += `kind="source"`, `citationMap?=null`; image meta mapper fallback `caption.ifBlank { prompt }` + test. Tests: type-filter 400, missing-type 400, 404s, `caption_404_keeps_editor` (VM keeps editor text on 404). UI: filter chips + search + detail per type (document → existing preview reuse; image → caption edit; site/web_bundle → preview/download reuse via callbacks; task/schedule → deep-link into Tasks/Schedules sections via callbacks, not new screens). Commit `feat(workspace): artifacts browser`.

### Task 6: Focus forwarding + origin-open + verification

**Files:** `feature/chat/presentation/.../ChatViewModel.kt` (forward + clear `pendingArtifactFocus` as one-shot event), `:app`/`shared/App.kt` (`onArtifactFocus` lambda → workspace/session nav), workspace VM origin-open (`getSession` via chat repository — cross-feature READ goes through the `:app`-provided lambda/callback, never a direct import), continue-in-new-session draft prefill.

**Interfaces:** Consumes: Tasks 1, 2, 5.

- [ ] **Step 1: Failing tests** — reducer sets `pendingArtifactFocus`; VM emits focus event once then clears (second collect → null); origin-open resolves projectId via fake session source.
- [ ] **Step 2–4:** Implement: `ChatEvent.ArtifactFocus(type,id)` (mirror existing one-shot channel); `:app` routes by type; workspace `openSiteOrigin(site)` = `getSession(site.sessionId)` → navigate canonical session; on 404 → `Site is no longer in this scope` user-safe; continue-new-session = create session (origin projectId) + composer draft prefill WITHOUT autosend (if composer has no draft-prefill state, add minimal `prefillDraft(text)` that leaves Send disabled until edit — verify composer shape first).
- [ ] **Step 5:** Full verification: touched-module units + `compileAndroidMain` + `compileKotlinIosArm64` green (except known pre-existing share failure — assert sole failure, do not fix); regression (old payloads, public preview, workspace tabs untouched). Commit `feat: artifact focus routing and origin open`.

## Self-Review

- Spec coverage: §1 arch → file map above (§2 contracts → Tasks 1–5 DTOs; §3 flows → Tasks 2, 6; §4 UI → Tasks 2–5 panels; §5 tests → per-task cases + Review Focus pins). Reports/charts/freeze read paths: covered by Task 5's generic `ArtifactItem.payload` + `getArtifact` (typed views only where UI needs them — no unimplemented-spec-requirement).
- Placeholder scan: every step names exact code/commands; no TBD/TODO; `mapWorkspaceError` shape flagged read-first (single lookup, not a placeholder).
- Type consistency: `ScopeSiteEntry` (workspace) vs `SessionSiteEntry` (chat) intentionally distinct (module boundaries); `TaskStatus`/`ArtifactType` string wires match backend enums; `pendingArtifactFocus` cleared post-emit in Task 6 (set in Task 1).
- Review Focus: all five lines pinned to owning-task tests above.

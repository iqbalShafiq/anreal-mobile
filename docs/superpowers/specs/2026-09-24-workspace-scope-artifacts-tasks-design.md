# Adoption Design — Workspace Scope: Sites-Browser, Artifact Focus, Artifacts, Tasks, Schedules

Date: 2026-09-24 | Backend branch: `feat/workspace-artifacts` (moving target — design now per user decision, re-verify contracts at implementation) | Mobile branch: `codex/complete-api-integration`
Status: sections 1–5 approved in chat; contracts below read from backend working tree 2026-09-24.

## 0. Outcome & constraints

- Outcome: adopt everything new since mobile's skills/MCP/sites adoption (baseline backend `0f14cc0`): sites scope registry + browser, `artifact_focus` stream event, artifacts read API, tasks CRUD, schedules CRUD — all inside the existing module layout, native execution, no subagents.
- Backend deltas verified unchanged (no mobile work): skills/MCP modules, `capabilities` counts, `metadata.skillIds/mcpServerIds`, steer/queue API, share fork. Model rename `gpt-5.6-luna`→`gpt-6-luna` touches only mobile tests + a stub label (catalog is server-driven).
- Constraints: same as before (`AGENTS.md`, `DESIGN.md`, android-* skills, chat-owned stream parser + pure reducer, features never depend on each other, workspace owns the cohesive browser, Bearer auth, DTO≠domain≠UI, `toUiText()`, M3 Expressive tokens, `commonMain` iOS-compilable, warnings-are-errors). New: presentation must not depend on `core:data` (verified ban holds); execution is NATIVE ONLY (user constraint — plan handoff must use executing-plans, never subagent-driven).
- Success: unit green per touched module, iOS compile, staging vs branch backend (task/schedule created from chat appear in browser; scope list cross-session; focus opens the right detail; site preview/download still green).
- Non-goals: push notification for schedule runs, offline-first artifact cache, client-side encryption, new `:feature:*` modules, autoSend-prefill parity beyond a manual composer draft (web's `autoSend:false` is a localStorage-draft behavior; mobile equivalent is an unsent composer draft — see §3).

## 1. Architecture (approach A, approved)

- No new `:feature:*` module. `:feature:workspace:{domain,data,presentation}` owns all browser surfaces (tasks, sites-scope, schedules, artifacts) extending the existing `WorkspaceScreen` tabs+search+cards+dialogs pattern. `:feature:chat` owns only: `artifact_focus` parse + reducer state, `sessionId` on site DTO, `GET session` source, site build panel (unchanged home), and focus forwarding.
- Domain (`feature:workspace:domain`): `WorkspaceTask(id,title,status,inbox|doing|done,description?,subtasks[{id,title,done}],sourceSessionId?,dueAt?,createdAt?,updatedAt?)`, `WorkspaceSchedule(id,title,prompt,freq once|daily|weekly,nextRunAt?,status)`, `ArtifactItem(type document|image|web_bundle|site|task|schedule|session + id + payload)`, `ScopeSiteEntry(siteId,sessionId,version,stableVersion,status,previewUrl,downloadUrl,updatedAt)`; interfaces `TasksRemoteDataSource`, `SchedulesRemoteDataSource`, `ArtifactsRemoteDataSource`, `ScopeSitesRemoteDataSource`. Single-source → DataSource, no Repository (YAGNI).
- Domain (`feature:chat:domain`): `ChatStreamEvent.ArtifactFocus(artifactId, artifactType, label?)`; `ChatThreadState.pendingArtifactFocus: ArtifactFocus? = null` (consumed-on-forward, cleared after emit — fire-and-forget like backend's publisher).
- Cross-feature: chat NEVER imports workspace. `:app`/`shared/App.kt` assembles `onArtifactFocus(type: String, id: String)` lambda → workspace navigation (detail/browser) or session navigation for `session` type. Origin-open uses chat's session source + workspace scope check.
- DI: `workspaceDataModule`/`workspacePresentationModule` additions; chat additions for session detail; all assembled in `:app` only.

## 2. Data contracts (read from backend 2026-09-24; re-verify at implementation — branch moves)

- Sites: `SessionSiteEntryDto` += `sessionId: String = ""`; `GET /api/sites?sessionId=` (400 `sessionId is required`, 404 session) → `{sites: [...]}` scoped `userId:projectId|standalone`, cap 50; `by-session` unchanged. `GET /api/chat/sessions/{id}` → `{sessionId,projectId,title,updatedAt}`, 404 `CHAT_SESSION_NOT_FOUND`.
- Tasks: `GET /api/tasks?sessionId=` → `{items}` (404 `SESSION_NOT_FOUND` when scoped session unknown); `POST /api/tasks {sessionId, title 1..200, description? ≤2000, addSubtasks? ≤50×200, dueAt? datetime-offset}` → 201 `{id,title,status}` (400 readable, 404 session); `PATCH /api/tasks/{id} {sessionId + ≥1 of status|title|description?|addSubtasks|toggleSubtasks[{id,done}]|removeSubtasks}` (empty → 400 `Nothing to update.`, unknown → 404 `TASK_NOT_FOUND`); `DELETE /api/tasks/{id}?sessionId=` → `{ok:true}` (400 missing sessionId, 404).
- Schedules: `GET /api/schedules?sessionId=` → `{items}` (take 100, created desc); `POST {sessionId,title,prompt 1..4000,freq once|daily|weekly,runAt?}` → 201 `{id,title,freq,nextRunAt,status}`; `DELETE /api/schedules/{id}?sessionId=` → `{ok:true}` (server cancels job; status→cancelled).
- Artifacts (read + caption): `GET /api/artifacts?sessionId=&type?=&q?=` → `{items}` (unknown type → 400); `GET /api/artifacts/{id}?type=&sessionId=` → `{artifact}` (type required → 400, else 404 `ARTIFACT_NOT_FOUND`); `PATCH /api/artifacts/images/{id} {caption, sessionId}` → updated (400/404 `IMAGE_NOT_FOUND`). Reports/charts/freeze (`POST /api/reports`, `GET /api/reports/{id}`, `POST /api/charts/snapshot`, `POST /api/web-bundles/freeze`) are agent-driven; mobile needs only read paths for v1 — VERIFY shapes at implementation.
- Stream: `artifactFocus{artifactId ≤120, artifactType ∈ 7, label? ≤200}` strict keys → `data.name="artifactFocus"`; unknown type → `Unknown` (never crash).
- DTO extras: `DocumentDto` += `kind="source"`, `citationMap?=null`; image meta += `caption=""` with mapper fallback `caption.ifBlank(prompt)`.
- Scope rule (server): every tasks/schedules/artifacts/scope-sites call carries the CURRENT `sessionId`; server resolves project scope. Mobile passes active session id through; unknown session → user-safe 404 handling (offer retry, not a crash).

## 3. Data flow

- Focus: reducer sets `pendingArtifactFocus` on `artifactFocus` events → ChatViewModel emits one-shot event → `:app` lambda routes: `site` → workspace sites browser highlight (or chat panel if same session build active); `task`/`schedule` → workspace modal/panel; `document`/`image`/`web_bundle` → artifacts detail; `session` → navigate that session. Then clear. No persistence (hint, not state).
- Sites-scope: workspace browser loads `GET /api/sites?sessionId=activeSession` on open + manual refresh; per-card Preview (sandbox WebView, public URL, no auth), Download (auth bytes → FileKit), origin-open (`GET session` → projectId → canonical session nav; re-check membership, else `Site is no longer in this scope`), continue-in-new-session (create session with origin projectId + prefill composer draft WITHOUT autosend — mobile needs an unsent-draft composer state; if absent, create disabled-Send draft state, never auto-send). History + rollback stay in chat build panel only.
- Tasks/schedules: browser CRUD direct; refresh after relevant chat turns (manual refresh button is the v1 mechanism; no socket). Create carries active `sessionId` (scope anchor). Subtask toggle → `toggleSubtasks`; status → inbox/doing/done segmented control.
- Artifacts: list with `type?` filter chips + `q` search field; detail per type (document summary/preview reuse, image with caption edit, web_bundle/site → preview/download reuse); reports/charts read-only views (shapes verified at implementation).

## 4. UI/UX (web parity: SitesBrowser, tasks-modal/panel, schedules-panel; adapted to M3 Expressive)

- Workspace gains Entries (tabs or rail entries mirroring web sidebar): Tasks (list + editor modal + subtask checklist + progress + due), Sites (scope cards + actions above), Schedules (list + editor + cancel), Artifacts (filter chips + search + detail). Reuse `WorkspaceScreen` list/grid + `AnrealSearchField` + card + `AlertDialog` + skeleton/error/empty; drill-down 160ms where detail nests.
- No new composer rows (all agent-driven, no toggles). Chat panel unchanged except focus forwarding (invisible).
- Every screen: loading/empty/error/populated + in-flight previews light/dark (`@AnrealPreviews`); Roborazzi PNGs for new chrome; tokens only (no blur/hex); Symbols Rounded; 48.dp; reduced motion/transparency.
- Copy: user-facing resources; scope errors (`Session not found`, `Site is no longer in this scope`) user-safe; 5xx generic.

## 5. Errors, tests, verification

- Decode non-2xx first; preserve message/code/status/details; 4xx shown, 5xx generic via `toUiText()`.
- Cases: scope/session 400+404; task empty-update 400, unknown task 404; schedule invalid freq/runAt 400; artifact unknown-type 400, missing type 400, not-found 404s; focus unknown-type → `Unknown` (no error UI).
- Tests: MockEngine per endpoint (CRUD, 400s, 404s), DTO unknown-keys + defaults (`caption` fallback, `sessionId` default), reducer focus valid/invalid + consume-clear, VM focus-route + origin-open + scope-merge, ViewModel tests with fakes + Turbine.
- Verification: touched-module units green, `compileAndroidMain` + `compileKotlinIosArm64`, staging vs branch backend checklist (§0), regression (old payloads parse, chat without ids, public preview logged-out, existing workspace tabs untouched).
- Order (approved): 1) focus parser + sessionId DTO + GET session, 2) sites-scope + browser, 3) tasks, 4) schedules, 5) artifacts/reports/charts. Slices sequential, each independently testable; native execution only.

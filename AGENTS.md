# AGENTS.md — Anreal Mobile

Rules for humans and coding agents. Follow this file and `DESIGN.md` on every change. Follow the Android skills in `~/.grok/skills/android-*`. If a change fights these rules, change the approach, not the rules.

Anreal is the **Kotlin Multiplatform client** of `chat-with-document`. Android ships. iOS compiles; platform actuals may be TODOs.

---

## 1. Product boundaries

- This repo is a **client**. No agent runtime, no Prisma, no Anvia React.
- Talk to the existing Hono API. Do not invent endpoints.
- Auth is Better Auth email/password with the Bearer plugin. Read `set-auth-token` after sign-in/sign-up, persist the token through the encrypted `SessionTokenStore`, and send `Authorization: Bearer <token>`. A legacy `Set-Cookie` parser may remain only as a migration fallback; authenticated mobile requests do not send browser cookies.
- Display name: **Anreal**. Visual language: DocChat (aurora, frost, amber *seed*).

---

## 2. Module layout

```
:app
:iosApp
:build-logic
:core:domain
:core:data
:core:database          ← Room 3 KMP (sessions, messages)
:core:presentation
:core:design-system
:feature:<name>:domain
:feature:<name>:data
:feature:<name>:presentation
```

Shipped feature modules: `auth`, `chat`, `workspace`.
Account / Settings is a **screen inside** `feature:chat:presentation` (`AccountRoute`), not `:feature:settings` yet. Projects, documents, and images share the cohesive `:feature:workspace:{domain,data,presentation}` browser; session-scoped attachment and context actions remain owned by chat.

### Dependency rules

| Layer | May depend on |
|---|---|
| presentation | own domain, `core:domain`, `core:presentation`, `core:design-system` |
| data | own domain, `core:domain`, `core:data` |
| domain | `core:domain` only |
| `:app` | everything (wires Koin + NavHost) |

- Features **never** depend on each other.
- Shared models (`User`, session ids, document ids, model catalog types) live in `core:domain`.
- Cross-feature navigation is a **lambda callback** assembled in `:app`.

A concern becomes its own `:core:*` module only when it has a non-trivial API. One helper stays in an existing core module.

---

## 3. Libraries (Kotlin-first, KMP)

| Concern | Library |
|---|---|
| DI | Koin (`singleOf` / `viewModelOf`) |
| HTTP | Ktor Client + KotlinX Serialization |
| Prefs | DataStore Preferences 1.2.1; `DataStoreSessionTokenStore` + Tink AEAD / Android Keystore for the session cookie. Not `security-crypto`. iOS Keychain is a TODO stub. |
| Env | `anreal.environment` in `gradle.properties` / `local.properties` → `BuildConfig.ENVIRONMENT` → `AppEnvironment`. **Development** uses in-process stub auth + chat. Staging / production hit `BASE_URL`. |
| Nav | Type-safe Navigation Compose (`@Serializable` routes) |
| Images | Coil 3 |
| Logging | Kermit |
| Theme | Material 3 Expressive + MaterialKolor |
| Glass | Haze (only inside `:core:design-system`) |
| Icons | Material Symbols Rounded (`com.composables`) — **not** `material-icons-extended` |
| Markdown | multiplatform-markdown-renderer |
| Dates | KotlinX Datetime |
| Immutable lists in state | `kotlinx.collections.immutable` |
| Files | FileKit on Android; iOS TODO |
| Tests | JUnit 5, Turbine, AssertK, coroutines-test, fakes |
| Screenshots | Roborazzi |

Local cache is **Room 3** (`androidx.room3` + bundled SQLite) in `:core:database`. Do not add Room 2.x. No Anvia Kotlin SDK — chat owns a JSONL parser + reducer.

Versions live in `gradle/libs.versions.toml` only.

---

## 4. Presentation (MVI)

Every screen:

1. `data class <Screen>State`
2. `sealed interface <Screen>Action`
3. `sealed interface <Screen>Event`
4. `<Screen>ViewModel` — `StateFlow` + `Channel` events, `onAction`
5. Same file: `<Screen>Root` (`koinViewModel`, `ObserveAsEvents`) and `<Screen>Screen` (state + onAction only)

- Update with `_state.update { it.copy(...) }`.
- UI models end in `Ui`. Domain errors map through `toUiText()`.
- Dynamic strings that are never resources stay `String`.
- Process-death-critical form fields go through `SavedStateHandle`.
- Do not inject `CoroutineDispatcher` unless the class is unit-tested *and* hops off Main.
- `@Stable` only when state contains `List`/`Map`/interfaces. Prefer immutable collections.

### Screen files stay thin

A `*Screen.kt` file holds only Root, Screen, and that screen’s previews. Feature-owned pieces (drawer, thread, composer, dialogs) live in `presentation/component/`. Group by visual unit — a row that only the drawer uses stays with the drawer; do not force one composable per file. Shared preview fixtures live in `presentation/preview/`.

Each extracted composable previews every state it can show (`@AnrealPreviews` + `AnrealPreview`). Screen previews still cover the assembled states (loading, empty, error, populated, plus in-flight / conflict where they exist).

### UX states are mandatory

A screen that only handles the happy path is incomplete. `State` must be able to represent **loading, empty, error, populated**, plus action-in-flight and success where the screen mutates. See `DESIGN.md` §8.

Each `Screen` needs previews for light/dark and at least populated + one of loading/empty/error.

---

## 5. Data

- One source → `*LocalDataSource` / `*RemoteDataSource`. Multiple sources → `*Repository`.
- Implementations are named for what they wrap: `KtorChatDataSource`, `DataStoreSessionStore`. Never `*Impl`.
- DTO ≠ domain ≠ UI model. Mappers are extension functions next to the DTO.
- Decode non-2xx bodies before mapping errors. Preserve the server `error`/`message`, `code`, HTTP status, and primitive detail fields in `DataError.Network`; do not reduce every response to its status code. User-safe 4xx messages may be shown, while 5xx messages stay generic.
- `HttpClientFactory.create(engine)` so tests inject `MockEngine`.
- Expected failures return `Result.Error`. Catch at the layer that owns the exception. Rethrow `CancellationException`.

Chat streaming:

- Parser + **pure reducer** live in `feature:chat:domain`. This is the protocol/headless layer.
- ViewModels apply reducer output. Composables do not parse JSONL.
- Extend the reducer for tools/images/queue. Do not fork a second stream pipeline.
- The composer **stays editable while a run is streaming**. A filled composer queues (`POST /api/chat/steer`); an empty one Stops. Queue mutations live in `feature:chat:domain/queue` and persist in Room. Hold is in-memory. Never lock the field the way stock `@anvia/react-ui` does.

Model + reasoning are **one** composer trigger and **one** sheet. The label concatenates the model with the effort when effort ≠ None (e.g. `GPT Luna 5.6 Xhigh`).

Do **not** add a `:core:chat-ui` / Anvia-style headless module until a second feature needs the same unstyled thread/composer/tool primitives. Until then, slot composables stay in `feature:chat:presentation/component/` (`ThreadPane`, `ComposerBar`, later tool cards). Do not clone `@anvia/react-ui`.

---

## 6. Design and motion

Read `DESIGN.md` before writing UI.

- Semantic color only: `MaterialTheme.colorScheme.*`.
- Type: `MaterialTheme.typography.*`. Space: `AnrealSpacing` / 4.dp grid.
- Components: M3 Expressive. Glass: design-system wrappers only.
- **Never** `Modifier.blur` / self `BlurEffect` for frost.
- **Never** hardcode `#E8A317` or near-black glass fills in a feature.
- Icons: Material Symbols Rounded. `contentDescription` from string resources, or `null` if decorative.
- Motion: `AnrealMotion` tokens. `transform`/`opacity` via `graphicsLayer`. No `scale(0)`, no `ease-in`, no layout-property animation, no per-token fade on the stream.
- Reduced motion, reduced transparency, and 48.dp targets ship with the component.

---

## 7. Navigation

- `@Serializable` `data object` / `data class` routes in the feature presentation module.
- One `NavGraphBuilder.<feature>Graph(...)` per feature.
- Intra-feature: `NavController`. Inter-feature: callbacks.
- Pass IDs, not whole objects.
- Transitions live on the root `NavHost` in `shared` (`anrealEnter` / `anrealExit`). Classify with `classifyNavMotion`. Do not leave NavHost on the default crossfade.
- Compose splash (aurora + mark + `Anreal v<versionName>` + “Presented by Ratmo.co”) is not a route. It holds until session resolve + `durationSplash`, then fades to boarding or chat. Android 12+ uses `Theme.Anreal.Splash`.
- Signed-out start is **boarding** (feature carousel + email + Create account). Login and Register sit above it.
- Signed-in start is a **New chat** draft, not the most recent session. Rejoin a session only when a run is still `running`.
- **Boarding → Login / Register** is a one-way vertical **pager**: boarding is replaced by the form and both move **up**. Login and Register have no back affordance and system back must not reveal boarding. **Login ↔ Register** is the same replace-current strip (login → register up, register → login down). No fade. Hide the IME before this navigate.
- **Auth → Chat** slides forward (right → left). Logout / pop to **boarding** is the reverse.
- **Chat → Account** is the same horizontal push. Account is opened from the left-drawer account footer (the whole row). The Account screen uses a compact section switcher with 160ms directional content motion; Logout is fixed in a floating bottom dock on that screen, never in the drawer menu.
- One `AnrealAtmosphere` wraps the `NavHost`. Nested `AnrealAtmosphere` calls are passthrough so aurora does not remount mid-transition.

---

## 8. DI

- One Koin module per feature layer that has bindings: `authDataModule`, `authPresentationModule`.
- Assemble in `:app` `Application` only.
- `koinViewModel()` only in Root composables.

---

## 9. Testing

- Unit-test every ViewModel and every non-trivial reducer/parser/validator.
- Fakes over mocks.
- `Dispatchers.setMain(UnconfinedTestDispatcher())` in ViewModel tests.
- Robot pattern when a screen has 3+ UI tests.
- Chat reducer tests use recorded JSONL fixtures.

---

## 10. iOS

- `commonMain` must compile for iOS.
- Platform actuals that need real iOS work (`encrypted storage`, `file picker`, wallpaper dynamic color, LaTeX WebView) are **TODO** stubs that compile.
- Do not write Swift features.

---

## 11. Kotlin style

- Import types. **Never** write a fully-qualified name in an expression (`co.ratmo.anreal.feature.chat.domain.stream.ChatRole.User`). `ChatRole.User` after an import is the default.
- `import foo.Bar as Baz` only when two imported types share a simple name. Do not alias “just in case”.
- Prefer `when (result)` / `onSuccess` / `onFailure` over `as Result.Success`.
- Compose-owned chrome (`DrawerState`, `LazyListState`, `ScrollState`) stays in the Screen. Do not mirror it into ViewModel state.

---

## 12. Quality bar

- No compile errors. Warnings are errors (`allWarningsAsErrors` in convention plugins).
- No unused parameters, no unexplained `!!`, no unused imports.
- User-facing strings are resources.
- `BASE_URL` from BuildKonfig / `local.properties`. Debug default is the machine LAN IP (not `10.0.2.2` — there is no emulator on the primary machine).
- Auth forms: `windowSoftInputMode=adjustNothing`. Do **not** `imePadding()` a centered form (that recenters and leaves a hole). Shift the form block with `rememberImeFocusShift` so the focused field sits just above the keyboard.
- Auth fields are single-line: use `Next` between steps and `Done` to submit the final field; never offer a newline. The chat composer is multi-line: Enter inserts a newline and the visible Send / Queue button submits. Single-field mutations such as rename use `Done` to confirm.

### Debug compile & install

- Debug build command is **always** `.\gradlew.bat :app:assembleDebug --console=plain` run directly in PowerShell (use `.\` prefix; do not wrap in `cmd /c` + `Start-Process` — that hides output and can hang the agent shell). Run it from the repo root with a generous timeout; it is fast when incremental.
- Fast compile check without an APK: `.\gradlew.bat :feature:chat:presentation:compileAndroidMain :feature:chat:presentation:compileKotlinIosArm64 --console=plain` (Android + iOS commonMain coverage).
- Install to the connected device: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk`.

### Visual QA (no emulator)

1. Compose `@Preview` (light, dark, states).
2. Roborazzi PNGs when chrome or a Screen changes — agents can read the images.
3. Physical device + `android screen capture` only if one is connected.

A single happy-path preview is not verification. Exercise loading, empty, error, and the populated path.

---

## 13. GitHub workflow

- Public repo: `iqbalShafiq/anreal-mobile`.
- One issue per slice. Do not start Milestone 3 until Chat core (parser, sessions, send/resume) is tested.
- Keep this file and `DESIGN.md` updated when a decision changes.

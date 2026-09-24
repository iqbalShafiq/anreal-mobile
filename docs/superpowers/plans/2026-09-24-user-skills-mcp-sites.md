# User Skills, MCP & Static Sites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adopt backend Skills, MCP servers, and Static Sites into `feature:chat` (DTO → DataSource → wiring → UI → tests), chat-owned, no new modules.

**Architecture:** Extend `feature:chat:{domain,data,presentation}` only. Single-source → `*RemoteDataSource` (no Repository, YAGNI). Stream events extend the existing parser + pure reducer. Selection persists via DataStore store. Management UI = list + drill-down editor inside chat presentation.

**Tech Stack:** Kotlin Multiplatform, Ktor Client + KotlinX Serialization, Koin (`singleOf`/`viewModelOf`), DataStore Preferences, Room untouched, Material 3 Expressive, JUnit 5 + Turbine + AssertK + `kotlinx-coroutines-test` + MockEngine, Roborazzi for chrome changes.

**Spec:** `docs/superpowers/specs/2026-09-23-user-skills-mcp-sites-design.md`

## Global Constraints

- Import types; never fully-qualified names in expressions; `import foo.Bar as Baz` only on simple-name clash.
- Prefer `when (result)` / `onSuccess` / `onFailure` over `as Result.Success`.
- State updates only via `_state.update { it.copy(...) }`; UI models end in `Ui`; domain errors map through `toUiText()`; dynamic-only strings stay `String`.
- User-facing strings are resources; icons Material Symbols Rounded; semantic `colorScheme`/`typography` only; `AnrealSpacing`/4dp; `AnrealMotion` tokens via `graphicsLayer`; never `Modifier.blur`; never hardcode `#E8A317` or near-black fills; 48.dp targets; reduced motion/transparency ship.
- Never inject `CoroutineDispatcher` unless unit-tested AND hops off Main; ViewModel tests use `Dispatchers.setMain(UnconfinedTestDispatcher())`.
- Rethrow `CancellationException`; expected failures return `Result.Error`; decode non-2xx before mapping; 4xx user-safe shown, 5xx generic.
- `commonMain` must compile for iOS; Android WebView/FileKit real, iOS stubs TODO that compile.
- Versions only in `gradle/libs.versions.toml`; no new endpoints; Bearer auth existing; preview URL public never sends `Authorization`.
- Warnings are errors; no unused params/imports; no unexplained `!!`.
- Composer stays editable while streaming; model + reasoning stay one trigger + one sheet.
- MCP DTOs never model secrets; token fields `password`, never prefilled.
- Run unit tests per touched module: `.\gradlew.bat :feature:chat:data:testAndroidHostTest :feature:chat:domain:testAndroidHostTest --console=plain`. Fast compile: `.\gradlew.bat :feature:chat:presentation:compileAndroidMain :feature:chat:presentation:compileKotlinIosArm64 --console=plain`. Debug APK: `.\gradlew.bat :app:assembleDebug --console=plain`.

## Review Focus

- Skill editor submitted with frontmatter `name:` mismatch → expect form-level readable error naming `bodyMd`, save blocked client-side before network.
- MCP save tapped with changed URL and no fresh test → expect save blocked with re-test guard, no network save call.
- Stream `site_build_ready` arrives for a different `siteId` than current build → expect panel resets to the new site, old preview never shown.
- `GET by-session` returns `[]` while a build is running → expect panel keeps streaming state, never flashes empty.
- Public preview URL opened logged-out → expect WebView loads with no `Authorization` header attached.

---

## File Structure

**Create (domain `.../feature/chat/domain/src/commonMain/.../domain/`):**
- `Skills.kt` — `SkillStatus`, `Skill`, `SkillIssue`, `SkillsRemoteDataSource`, `SKILL_NAME_RE`, `validateSkillInput(): Map<String,String>`.
- `McpServers.kt` — `McpAuthType`, `McpTool`, `McpServer`, `McpTestResult`, `McpRemoteDataSource`, `validateMcpInput(): Map<String,String>`.
- `StaticSites.kt` — `SiteStatus`, `SiteBuildPhase`, `SessionSiteEntry`, `SiteVersionEntry`, `SiteBuildState`, `SitesRemoteDataSource`, `EnhancementSelectionStore`, `intersectWithCatalog`, `applySiteBuildEvent`, `applySiteVersionEvent`, `stableSiteUrls`.
- `stream/SiteBuildEvents.kt` — `SiteBuildProgress`, `SiteBuildReady` payloads (or fold into `ChatModels.kt` if smaller).

**Create (data `.../feature/chat/data/src/commonMain/.../data/`):**
- `SkillsDtos.kt` — `SkillDto`, `SkillBodyDto`, `toSkill()`; `KtorSkillsDataSource`.
- `McpDtos.kt` — `McpServerDto` (`allowedToolsJson`), `McpTestResultDto`, `toMcpServer()`; `KtorMcpDataSource`.
- `SitesDtos.kt` — `SessionSitesDto`, `SessionSiteEntryDto`, `toSessionSiteEntry()`; `KtorSitesDataSource`.
- `DataStoreEnhancementSelectionStore.kt` — DataStore impl of `EnhancementSelectionStore`.

**Create (tests):**
- `feature/chat/domain/.../SkillsTest.kt`, `McpServersTest.kt`, `StaticSitesTest.kt`, `stream/SiteBuildStreamTest.kt` (JSONL fixtures inline).
- `feature/chat/data/.../SkillsDtosTest.kt`, `KtorSkillsDataSourceTest.kt`, `KtorMcpDataSourceTest.kt`, `KtorSitesDataSourceTest.kt`.
- Presentation: extend `ChatViewModelTest.kt` or add `EnhancementSelectionTest.kt`; previews in component files.

**Modify:**
- `.../data/ChatDtos.kt` — `CapabilitiesDto` += counts; `ChatRequestMetadataDto` += `skillIds`/`mcpServerIds`; `toCapabilities()`; `toMetadataDto` in `KtorChatRemoteDataSource.kt`.
- `.../domain/ChatCatalog.kt` — `ChatCapabilities` += counts; `ChatRunOptions` += `skillIds`/`mcpServerIds`.
- `.../domain/stream/ChatModels.kt` — `ChatStreamEvent.SiteBuildProgress`, `SiteBuildReady`; `ChatThreadState` += `siteBuild: SiteBuildState? = null`, `siteVersions: List<SiteVersionEntry> = emptyList()`.
- `.../domain/stream/ChatStreamParser.kt` — `parseDataEvent` dispatches `siteBuildProgress`/`siteBuildReady`.
- `.../domain/stream/ChatReducer.kt` — reduce the two site events via pure helpers.
- `.../data/ChatDataModule.kt` — `singleOf` new sources + selection store (stub vs real via `AppConfig.environment.stubApi` like neighbors).
- `.../presentation/ChatViewModel.kt`, `ChatScreen.kt`, `component/ComposerSheets.kt` — selection state, rows, panel, poll/retry/rollback.
- `.../presentation/component/SkillsSheets.kt`, `McpSheets.kt`, `SiteBuildPanel.kt` — management + panel UI.
- `.../presentation/ChatGraph.kt`, `ChatPresentationModule.kt` — routes + viewmodels.

---

### Task 1: Capabilities + metadata delta

**Files:**
- Modify: `feature/chat/data/.../data/ChatDtos.kt:146-173,476-481`
- Modify: `feature/chat/domain/.../domain/ChatCatalog.kt:142-157`
- Modify: `feature/chat/data/.../data/KtorChatRemoteDataSource.kt:511-523`
- Test: `feature/chat/data/.../data/CapabilitiesMetadataTest.kt` (new)

**Interfaces:**
- Consumes: nothing new.
- Produces: `ChatCapabilities(userSkillsCount, userMcpCount)`, `ChatRunOptions(skillIds, mcpServerIds)` for Tasks 8–10.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun capabilities_old_payload_still_parses_with_zero_counts() = runTest {
    val dto = Json.decodeFromString<CapabilitiesDto>(
        """{"webSearchAvailable":true,"deepResearchAvailable":false,"imageGenerationAvailable":true,"context7Configured":false}""",
    )
    val capabilities = dto.toCapabilities()
    assertThat(capabilities.userSkillsCount).isEqualTo(0)
    assertThat(capabilities.userMcpCount).isEqualTo(0)
}

@Test
fun capabilities_new_counts_map() = runTest {
    val dto = Json.decodeFromString<CapabilitiesDto>(
        """{"webSearchAvailable":false,"deepResearchAvailable":false,"imageGenerationAvailable":false,"context7Configured":false,"userSkillsCount":3,"userMcpCount":1,"futureFlag":true}""",
    )
    val capabilities = dto.toCapabilities()
    assertThat(capabilities.userSkillsCount).isEqualTo(3)
    assertThat(capabilities.userMcpCount).isEqualTo(1)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: FAIL — `CapabilitiesDto` has no `userSkillsCount`, `Unresolved reference`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
@Serializable
data class CapabilitiesDto(
    val webSearchAvailable: Boolean = false,
    val deepResearchAvailable: Boolean = false,
    val imageGenerationAvailable: Boolean = false,
    val context7Configured: Boolean = false,
    val userSkillsCount: Int = 0,
    val userMcpCount: Int = 0,
)

@Serializable
data class ChatRequestMetadataDto(
    val sessionId: String,
    val documentIds: List<String>,
    val modelId: String,
    val reasoningEffort: String?,
    val webSearchEnabled: Boolean,
    val imageGenerationEnabled: Boolean,
    val deepResearchEnabled: Boolean,
    val imageGenSettings: ImageGenSettingsDto?,
    val skillIds: List<String> = emptyList(),
    val mcpServerIds: List<String> = emptyList(),
)
```

Domain (`ChatCatalog.kt`):

```kotlin
data class ChatCapabilities(
    val webSearchAvailable: Boolean = false,
    val deepResearchAvailable: Boolean = false,
    val imageGenerationAvailable: Boolean = false,
    val context7Configured: Boolean = false,
    val userSkillsCount: Int = 0,
    val userMcpCount: Int = 0,
)

data class ChatRunOptions(
    val model: String? = null,
    val reasoningEffort: String? = null,
    val webSearchEnabled: Boolean = false,
    val deepResearchEnabled: Boolean = false,
    val imageGenerationEnabled: Boolean = false,
    val imageGenSettings: ImageGenSettings? = null,
    val documentIds: List<String> = emptyList(),
    val skillIds: List<String> = emptyList(),
    val mcpServerIds: List<String> = emptyList(),
)
```

Mapper + `toMetadataDto` additions:

```kotlin
fun CapabilitiesDto.toCapabilities(): ChatCapabilities = ChatCapabilities(
    webSearchAvailable = webSearchAvailable,
    deepResearchAvailable = deepResearchAvailable,
    imageGenerationAvailable = imageGenerationAvailable,
    context7Configured = context7Configured,
    userSkillsCount = userSkillsCount,
    userMcpCount = userMcpCount,
)
```

```kotlin
return ChatRequestMetadataDto(
    ...
    imageGenSettings = imageSettings?.takeIf { imageGenerationEnabled }?.toDto(),
    skillIds = skillIds.distinct().take(20),
    mcpServerIds = mcpServerIds.distinct().take(5),
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/data/src/commonMain/.../data/ChatDtos.kt feature/chat/domain/.../domain/ChatCatalog.kt feature/chat/data/.../KtorChatRemoteDataSource.kt feature/chat/data/src/commonTest/.../CapabilitiesMetadataTest.kt
git commit -m "feat(chat): capabilities counts and metadata skill/mcp ids"
```

### Task 2: Skills domain + client validation

**Files:**
- Create: `feature/chat/domain/.../domain/Skills.kt`
- Test: `feature/chat/domain/.../SkillsTest.kt`

**Interfaces:**
- Consumes: `core:domain` `Result`, `Error`.
- Produces: `Skill`, `SkillsRemoteDataSource`, `validateSkillInput` for Tasks 3, 8, 11.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun validate_rejects_bad_slug_and_mismatched_frontmatter() {
    val errors = validateSkillInput(
        name = "Bad Name",
        description = "Useful",
        bodyMd = "---\nname: other\ndescription: Useful\n---\nbody",
    )
    assertThat(errors.containsKey("name")).isTrue()
    assertThat(errors.containsKey("bodyMd")).isTrue()
}

@Test
fun validate_accepts_matching_frontmatter_with_quotes() {
    val errors = validateSkillInput(
        name = "release-notes",
        description = "Draft notes",
        bodyMd = "---\nname: \"release-notes\"\ndescription: \"Draft notes\"\n---\n# body",
    )
    assertThat(errors.isEmpty()).isTrue()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: validateSkillInput`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result

enum class SkillStatus { Active, Invalid, Draft }

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val bodyMd: String = "",
    val isEnabled: Boolean = true,
    val status: SkillStatus = SkillStatus.Active,
    val version: Int = 1,
)

interface SkillsRemoteDataSource {
    suspend fun listSkills(): Result<List<Skill>, DataError.Network>
    suspend fun getSkill(id: String): Result<Skill, DataError.Network>
    suspend fun createSkill(name: String, description: String, bodyMd: String): Result<Skill, DataError.Network>
    suspend fun updateSkill(id: String, name: String, description: String, bodyMd: String, markReviewed: Boolean): Result<Skill, DataError.Network>
    suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network>
    suspend fun deleteSkill(id: String): Result<Unit, DataError.Network>
}

val SKILL_NAME_RE = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

fun validateSkillInput(name: String, description: String, bodyMd: String): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    val trimmedName = name.trim()
    if (trimmedName.isEmpty() || trimmedName.length > 64 || !SKILL_NAME_RE.matches(trimmedName)) {
        errors["name"] = "Lowercase letters, numbers, hyphens."
    }
    if (description.trim().isEmpty() || description.trim().length > 1024) {
        errors["description"] = "One line the agent uses to decide fit."
    }
    val body = bodyMd.trim()
    if (body.isEmpty() || body.length > 16000) errors["bodyMd"] = "Starts with --- frontmatter."
    else {
        val fence = extractFrontmatter(body)
        if (fence == null) errors["bodyMd"] = "Starts with --- frontmatter."
        else {
            if (unquote(fence["name"]) != trimmedName) errors["bodyMd"] = "Frontmatter name must equal the skill name."
            else if (unquote(fence["description"]) != description.trim()) errors["bodyMd"] = "Frontmatter description must match."
        }
    }
    return errors
}

private fun extractFrontmatter(body: String): Map<String, String>? {
    val lines = body.lines()
    if (lines.firstOrNull()?.trim() != "---") return null
    val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
    if (end < 0) return null
    return lines.drop(1).take(end).mapNotNull { line ->
        val key = line.substringBefore(":").trim()
        val value = line.substringAfter(":", "").trim()
        if (key.isEmpty() || value.isEmpty()) null else key to value
    }.toMap()
}

private fun unquote(value: String?): String? {
    if (value == null) return null
    return value.removeSurrounding("\"").removeSurrounding("'")
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/domain/src/commonMain/.../domain/Skills.kt feature/chat/domain/src/commonTest/.../SkillsTest.kt
git commit -m "feat(chat): skills domain model and client validation"
```

### Task 3: KtorSkillsDataSource + tests + DI

**Files:**
- Create: `feature/chat/data/.../data/SkillsDtos.kt` (DTOs + mapper + `KtorSkillsDataSource`)
- Test: `feature/chat/data/.../KtorSkillsDataSourceTest.kt`
- Modify: `feature/chat/data/.../data/ChatDataModule.kt` (register source + stub branch)

**Interfaces:**
- Consumes: `SkillsRemoteDataSource` (Task 2), `httpClient.get/post/put/patch/delete` (`core:data`).
- Produces: bound `SkillsRemoteDataSource` for Tasks 8, 11.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun list_skills_maps_rows() = runTest {
    val source = skillsSource(
        path = "/api/skills",
        body = """[{"id":"s1","userId":"u1","name":"release-notes","description":"Draft notes","bodyMd":"---\nname: release-notes\n---","isEnabled":true,"status":"active","version":1,"createdAt":"t","updatedAt":"t"}]""",
    )
    when (val result = source.listSkills()) {
        is Result.Success -> assertThat(result.data.single().name).isEqualTo("release-notes")
        is Result.Error -> error("expected success")
    }
}

@Test
fun create_skill_400_keeps_server_message() = runTest {
    val source = skillsSource(
        path = "/api/skills",
        status = HttpStatusCode.BadRequest,
        body = """{"error":"Frontmatter name must equal the skill name","issues":[{"path":"bodyMd","message":"Frontmatter name must equal the skill name"}],"code":"INVALID_SKILL"}""",
    )
    val result = source.createSkill("brief", "Write a brief", "body")
    val error = (result as Result.Error).error
    assertThat(error.serverMessage).isEqualTo("Frontmatter name must equal the skill name")
}

@Test
fun delete_foreign_id_maps_not_found() = runTest {
    val source = skillsSource(
        path = "/api/skills/foreign",
        status = HttpStatusCode.NotFound,
        body = """{"error":"Skill not found","code":"SKILL_NOT_FOUND"}""",
    )
    val result = source.deleteSkill("foreign")
    assertThat((result as Result.Error).error.kind).isEqualTo(DataError.Network.Kind.NOT_FOUND)
}
```

Helper mirrors the existing `source()` in `KtorChatRemoteDataSourceTest`:

```kotlin
private fun skillsSource(path: String, status: HttpStatusCode = HttpStatusCode.OK, body: String): KtorSkillsDataSource {
    val engine = MockEngine { request ->
        check(request.url.encodedPath == path)
        respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return KtorSkillsDataSource(HttpClientFactory.create(engine, InMemorySessionTokenStore(), "http://127.0.0.1:3001"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: KtorSkillsDataSource`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.patch
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.put
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.feature.chat.domain.Skill
import co.ratmo.anreal.feature.chat.domain.SkillStatus
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable

@Serializable
data class SkillDto(
    val id: String,
    val userId: String = "",
    val name: String,
    val description: String = "",
    val bodyMd: String = "",
    val isEnabled: Boolean = true,
    val status: String = "active",
    val version: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class SkillBodyDto(val name: String, val description: String, val bodyMd: String)

@Serializable
data class SkillReviewDto(val name: String, val description: String, val bodyMd: String, val markReviewed: Boolean = false)

@Serializable
data class SkillEnabledDto(val isEnabled: Boolean)

fun SkillDto.toSkill(): Skill = Skill(
    id = id, name = name, description = description, bodyMd = bodyMd,
    isEnabled = isEnabled, status = when (status) {
        "draft" -> SkillStatus.Draft
        "invalid" -> SkillStatus.Invalid
        else -> SkillStatus.Active
    }, version = version,
)

class KtorSkillsDataSource(private val httpClient: HttpClient) : SkillsRemoteDataSource {
    override suspend fun listSkills(): Result<List<Skill>, DataError.Network> =
        httpClient.get<List<SkillDto>>(route = "/api/skills").map { dtos -> dtos.map { it.toSkill() } }

    override suspend fun getSkill(id: String): Result<Skill, DataError.Network> =
        httpClient.get<SkillDto>(route = "/api/skills/$id").map { it.toSkill() }

    override suspend fun createSkill(name: String, description: String, bodyMd: String): Result<Skill, DataError.Network> =
        httpClient.post<SkillBodyDto, SkillDto>(route = "/api/skills", body = SkillBodyDto(name, description, bodyMd)).map { it.toSkill() }

    override suspend fun updateSkill(id: String, name: String, description: String, bodyMd: String, markReviewed: Boolean): Result<Skill, DataError.Network> =
        httpClient.put<SkillReviewDto, SkillDto>(route = "/api/skills/$id", body = SkillReviewDto(name, description, bodyMd, markReviewed)).map { it.toSkill() }

    override suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network> =
        httpClient.patch<SkillEnabledDto, SkillDto>(route = "/api/skills/$id/enabled", body = SkillEnabledDto(isEnabled)).map { it.toSkill() }

    override suspend fun deleteSkill(id: String): Result<Unit, DataError.Network> =
        httpClient.delete(route = "/api/skills/$id").asEmptyResult()
}
```

DI (`ChatDataModule.kt`): add `single<SkillsRemoteDataSource> { if (stub) StubSkillsDataSource() else KtorSkillsDataSource(get()) }` following the `AccountSettingsDataSource` pattern; add a tiny `StubSkillsDataSource` returning `Result.Success(emptyList())` / `Result.Error(DataError.Network.UNKNOWN)` next to `StubAccountSettingsDataSource`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/data/src/commonMain/.../data/SkillsDtos.kt feature/chat/data/src/commonTest/.../KtorSkillsDataSourceTest.kt feature/chat/data/src/commonMain/.../data/ChatDataModule.kt
git commit -m "feat(chat): skills dtos and ktor data source"
```

### Task 4: MCP domain + DTOs + mapper

**Files:**
- Create: `feature/chat/domain/.../domain/McpServers.kt`
- Test: `feature/chat/domain/.../McpServersTest.kt` + `feature/chat/data/.../McpDtosTest.kt`
- Create DTO part inside `feature/chat/data/.../data/McpDtos.kt` (DTOs + `toMcpServer()` only; source in Task 5)

**Interfaces:**
- Consumes: Task 1 counts.
- Produces: `McpServer`, `McpRemoteDataSource`, `validateMcpInput` for Tasks 5, 8, 12.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun mcp_mapper_reads_allowed_tools_json_and_flags() {
    val dto = Json.decodeFromString<McpServerDto>(
        """{"id":"m1","userId":"u1","name":"docs","url":"https://mcp.example.com/mcp","authType":"bearer","allowedToolsJson":["search_docs"],"toolsJson":[{"name":"search_docs","description":"Search"}],"isEnabled":true,"status":"untested","hasCredentials":true,"hasHeaders":false}""",
    )
    val server = dto.toMcpServer()
    assertThat(server.allowedTools).isEqualTo(listOf("search_docs"))
    assertThat(server.hasCredentials).isTrue()
    assertThat(server.status).isEqualTo(McpStatus.Untested)
}

@Test
fun validate_rejects_authorization_header_and_http_url() {
    val errors = validateMcpInput(
        name = "docs", url = "http://mcp.example.com/mcp", authType = "bearer",
        headers = listOf("Authorization" to "x"),
    )
    assertThat(errors.containsKey("url")).isTrue()
    assertThat(errors.containsKey("headers")).isTrue()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: McpServerDto`.

- [ ] **Step 3: Write minimal implementation**

Domain:

```kotlin
package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result

enum class McpAuthType { None, Bearer }
enum class McpStatus { Ok, Untested, Error }

data class McpTool(val name: String, val description: String)
data class McpServer(
    val id: String,
    val name: String,
    val url: String,
    val authType: McpAuthType = McpAuthType.None,
    val allowedTools: List<String> = emptyList(),
    val tools: List<McpTool> = emptyList(),
    val isEnabled: Boolean = true,
    val status: McpStatus = McpStatus.Untested,
    val lastError: String? = null,
    val hasCredentials: Boolean = false,
    val hasHeaders: Boolean = false,
)

data class McpTestResult(val ok: Boolean, val tools: List<McpTool> = emptyList(), val error: String? = null)

interface McpRemoteDataSource {
    suspend fun listServers(): Result<List<McpServer>, DataError.Network>
    suspend fun createServer(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>): Result<McpServer, DataError.Network>
    suspend fun updateServer(id: String, name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?): Result<McpServer, DataError.Network>
    suspend fun setServerEnabled(id: String, isEnabled: Boolean): Result<McpServer, DataError.Network>
    suspend fun deleteServer(id: String): Result<Unit, DataError.Network>
    suspend fun testConnection(url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>, serverId: String?): Result<McpTestResult, DataError.Network>
}

fun validateMcpInput(name: String, url: String, authType: String, headers: List<Pair<String, String>>): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (name.trim().isEmpty() || name.trim().length > 64) errors["name"] = "Short label used for tool prefixes."
    if (!isPublicHttps(url)) errors["url"] = "Public https Streamable HTTP endpoint."
    val seen = mutableSetOf<String>()
    headers.forEach { (headerName, value) ->
        if (headerName.equals("authorization", ignoreCase = true)) errors["headers"] = "Use the auth type, not an authorization header."
        else if (!seen.add(headerName.lowercase())) errors["headers"] = "Duplicate header."
        else if (value.isEmpty()) errors["headers"] = "Header value required."
    }
    if (headers.size > 16) errors["headers"] = "At most 16 custom headers."
    return errors
}

fun isPublicHttps(rawUrl: String): Boolean {
    val parsed = runCatching { io.ktor.http.Url(rawUrl.trim()) }.getOrNull() ?: return false
    if (parsed.protocol.name != "https") return false
    val host = parsed.host.lowercase()
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return false
    if (host == "0.0.0.0" || host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("127.") || host == "::1") return false
    return true
}
```

DTOs (`McpDtos.kt`):

```kotlin
@Serializable
data class McpToolDto(val name: String, val description: String = "")

@Serializable
data class McpServerDto(
    val id: String,
    val userId: String = "",
    val name: String,
    val url: String,
    val authType: String = "none",
    val allowedToolsJson: List<String> = emptyList(),
    val toolsJson: List<McpToolDto>? = null,
    val isEnabled: Boolean = true,
    val status: String = "untested",
    val lastError: String? = null,
    val hasCredentials: Boolean = false,
    val hasHeaders: Boolean = false,
)

@Serializable
data class McpTestResultDto(val ok: Boolean, val tools: List<McpToolDto>? = null, val error: String? = null)

fun McpServerDto.toMcpServer(): McpServer = McpServer(
    id = id, name = name, url = url,
    authType = if (authType == "bearer") McpAuthType.Bearer else McpAuthType.None,
    allowedTools = allowedToolsJson,
    tools = toolsJson.orEmpty().map { McpTool(it.name, it.description) },
    isEnabled = isEnabled,
    status = when (status) { "ok" -> McpStatus.Ok; "error" -> McpStatus.Error; else -> McpStatus.Untested },
    lastError = lastError, hasCredentials = hasCredentials, hasHeaders = hasHeaders,
)
```

Note: `isPublicHttps` uses `io.ktor.http.Url` (ktor-http is already a transitive dep via ktor client; data-layer `validateMcpInput` lives in domain — domain has NO ktor dep. Move `isPublicHttps` to `McpDtos.kt` in data, keep domain `validateMcpInput` URL check as syntactic (scheme/host string check). Adjust in implementation step: domain checks `trim().startsWith("https://")` + private-host substrings; data-layer `KtorMcpDataSource` relies on server authoritative validation.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest :feature:chat:data:testAndroidHostTest --console=plain`
Expected: PASS (after placing URL check correctly per note).

- [ ] **Step 5: Commit**

```bash
git add feature/chat/domain/src/commonMain/.../domain/McpServers.kt feature/chat/data/src/commonMain/.../data/McpDtos.kt feature/chat/domain/src/commonTest/.../McpServersTest.kt feature/chat/data/src/commonTest/.../McpDtosTest.kt
git commit -m "feat(chat): mcp domain, dtos and mapper"
```

### Task 5: KtorMcpDataSource + tests + DI

**Files:**
- Modify: `feature/chat/data/.../data/McpDtos.kt` (append `KtorMcpDataSource`)
- Test: `feature/chat/data/.../KtorMcpDataSourceTest.kt`
- Modify: `feature/chat/data/.../data/ChatDataModule.kt`

**Interfaces:**
- Consumes: `McpRemoteDataSource` (Task 4).
- Produces: bound `McpRemoteDataSource` for Tasks 8, 12.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun test_ok_true_maps_tools() = runTest {
    val source = mcpSource(
        path = "/api/mcp-servers/test",
        body = """{"ok":true,"tools":[{"name":"search_docs","description":"Search the docs"}]}""",
    )
    val result = source.testConnection("https://mcp.example.com/mcp", McpAuthType.None, null, emptyList(), null)
    val data = (result as Result.Success).data
    assertThat(data.ok).isTrue()
    assertThat(data.tools.single().name).isEqualTo("search_docs")
}

@Test
fun test_ok_false_is_success_result_with_error() = runTest {
    val source = mcpSource(
        path = "/api/mcp-servers/test",
        body = """{"ok":false,"error":"MCP URL must use public https (http and private hosts are blocked)"}""",
    )
    val result = source.testConnection("http://mcp.example.com/mcp", McpAuthType.None, null, emptyList(), null)
    val data = (result as Result.Success).data
    assertThat(data.ok).isFalse()
}

@Test
fun create_400_duplicate_name_keeps_message() = runTest {
    val source = mcpSource(
        path = "/api/mcp-servers",
        status = HttpStatusCode.BadRequest,
        body = """{"error":"An MCP server with this name already exists","code":"INVALID_MCP_SERVER"}""",
    )
    val result = source.createServer("docs", "https://mcp.example.com/mcp", McpAuthType.None, null, emptyList())
    assertThat((result as Result.Error).error.serverMessage).isEqualTo("An MCP server with this name already exists")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: KtorMcpDataSource`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
@Serializable
data class McpHeaderDto(val name: String, val value: String)

@Serializable
data class McpUpsertDto(
    val name: String, val url: String, val authType: String,
    val token: String? = null, val headers: List<McpHeaderDto>? = null,
)

@Serializable
data class McpTestDto(
    val url: String, val authType: String,
    val token: String? = null, val headers: List<McpHeaderDto>? = null, val serverId: String? = null,
)

@Serializable
data class McpEnabledDto(val isEnabled: Boolean)

class KtorMcpDataSource(private val httpClient: HttpClient) : McpRemoteDataSource {
    override suspend fun listServers(): Result<List<McpServer>, DataError.Network> =
        httpClient.get<List<McpServerDto>>(route = "/api/mcp-servers").map { dtos -> dtos.map { it.toMcpServer() } }

    override suspend fun createServer(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>): Result<McpServer, DataError.Network> =
        httpClient.post<McpUpsertDto, McpServerDto>(route = "/api/mcp-servers", body = upsert(name, url, authType, token, headers)).map { it.toMcpServer() }

    override suspend fun updateServer(id: String, name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?): Result<McpServer, DataError.Network> =
        httpClient.put<McpUpsertDto, McpServerDto>(route = "/api/mcp-servers/$id", body = upsert(name, url, authType, token, headers)).map { it.toMcpServer() }

    override suspend fun setServerEnabled(id: String, isEnabled: Boolean): Result<McpServer, DataError.Network> =
        httpClient.patch<McpEnabledDto, McpServerDto>(route = "/api/mcp-servers/$id/enabled", body = McpEnabledDto(isEnabled)).map { it.toMcpServer() }

    override suspend fun deleteServer(id: String): Result<Unit, DataError.Network> =
        httpClient.delete(route = "/api/mcp-servers/$id").asEmptyResult()

    override suspend fun testConnection(url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>, serverId: String?): Result<McpTestResult, DataError.Network> =
        httpClient.post<McpTestDto, McpTestResultDto>(
            route = "/api/mcp-servers/test",
            body = McpTestDto(url, authType.name.lowercase(), token?.takeIf { it.isNotEmpty() }, headers.map { McpHeaderDto(it.first, it.second) }.takeIf { it.isNotEmpty() }, serverId),
        ).map { dto -> McpTestResult(dto.ok, dto.tools.orEmpty().map { McpTool(it.name, it.description) }, dto.error) }

    private fun upsert(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?) = McpUpsertDto(
        name = name, url = url, authType = authType.name.lowercase(),
        token = token?.takeIf { it.isNotEmpty() },
        headers = headers?.map { McpHeaderDto(it.first, it.second) },
    )
}
```

No `allowedTools`/`tools` sent on create/update: review tools are chosen in the editor AFTER test; server accepts omission. (If backend requires them, Task 12 editor passes them — wire then.)

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/data/src/commonMain/.../data/McpDtos.kt feature/chat/data/src/commonTest/.../KtorMcpDataSourceTest.kt feature/chat/data/src/commonMain/.../data/ChatDataModule.kt
git commit -m "feat(chat): mcp ktor data source and test endpoint"
```

### Task 6: Static Sites domain + pure event helpers

**Files:**
- Create: `feature/chat/domain/.../domain/StaticSites.kt`
- Test: `feature/chat/domain/.../StaticSitesTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `SiteBuildState`, `SiteVersionEntry`, `SitesRemoteDataSource`, `EnhancementSelectionStore`, `applySiteBuildEvent/applySiteVersionEvent`, `intersectWithCatalog`, `stableSiteUrls` for Tasks 7–9, 13.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun ready_event_marks_stable_and_sorts() {
    val versions = applySiteVersionEvent(
        current = listOf(SiteVersionEntry("s1", 1, ready = true, stable = true)),
        eventSiteId = "s1", eventVersion = 2, phase = SiteBuildPhase.Ready,
        previewUrl = "/api/sites/s1/v2/preview/index.html", downloadUrl = "/api/sites/s1/v2/download",
    )
    assertThat(versions.map { it.version }).isEqualTo(listOf(1, 2))
    assertThat(versions.first { it.version == 2 }.stable).isTrue()
    assertThat(versions.first { it.version == 1 }.stable).isFalse()
}

@Test
fun different_site_id_resets_state() {
    val build = applySiteBuildEvent(
        current = SiteBuildState("s1", 1, SiteBuildPhase.Building, "Membangun halaman."),
        eventSiteId = "s2", eventVersion = 1, phase = SiteBuildPhase.Starting, message = "Menunggu antrean build.",
    )
    assertThat(build?.siteId).isEqualTo("s2")
    assertThat(build?.previewUrl).isNull()
}

@Test
fun selection_intersects_catalog() {
    assertThat(intersectWithCatalog(listOf("a", "b", "ghost"), listOf("a", "b"))).isEqualTo(listOf("a", "b"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: applySiteVersionEvent`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

enum class SiteStatus { Queued, Running, Ready, Failed }
enum class SiteBuildPhase { Starting, Planning, Building, Bundling, Preview, Ready, Failed }

data class SessionSiteEntry(
    val siteId: String, val version: Int, val stableVersion: Int?,
    val status: SiteStatus, val previewUrl: String?, val downloadUrl: String, val updatedAt: String,
)

data class SiteVersionEntry(
    val siteId: String, val version: Int,
    val ready: Boolean, val failed: Boolean, val stable: Boolean,
    val previewUrl: String?, val downloadUrl: String,
)

data class SiteBuildState(
    val siteId: String, val version: Int, val phase: SiteBuildPhase,
    val message: String, val previewUrl: String? = null, val downloadUrl: String? = null,
)

interface SitesRemoteDataSource {
    suspend fun sitesBySession(sessionId: String): Result<List<SessionSiteEntry>, DataError.Network>
    suspend fun downloadSite(siteId: String, version: Int): Result<ByteArray, DataError.Network>
    suspend fun retrySite(siteId: String): Result<SiteBuildState, DataError.Network>
    suspend fun rollbackSite(siteId: String, version: Int): Result<Int, DataError.Network>
}

interface EnhancementSelectionStore {
    fun observeSkillIds(): Flow<List<String>>
    fun observeMcpServerIds(): Flow<List<String>>
    suspend fun saveSkillIds(ids: List<String>)
    suspend fun saveMcpServerIds(ids: List<String>)
}

fun intersectWithCatalog(selected: List<String>, catalogIds: List<String>): List<String> {
    val catalog = catalogIds.toSet()
    return selected.filter { it in catalog }.distinct()
}

fun applySiteBuildEvent(
    current: SiteBuildState?, eventSiteId: String, eventVersion: Int,
    phase: SiteBuildPhase, message: String, previewUrl: String? = null, downloadUrl: String? = null,
): SiteBuildState {
    if (current == null || current.siteId != eventSiteId) {
        return SiteBuildState(eventSiteId, eventVersion, phase, message, previewUrl, downloadUrl)
    }
    if (phase == SiteBuildPhase.Ready && message.isBlank()) {
        return current.copy(version = eventVersion, phase = phase, message = "Situs siap diunduh.", previewUrl = previewUrl ?: current.previewUrl, downloadUrl = downloadUrl ?: current.downloadUrl)
    }
    return current.copy(version = eventVersion, phase = phase, message = message, previewUrl = previewUrl ?: current.previewUrl, downloadUrl = downloadUrl ?: current.downloadUrl)
}

fun applySiteVersionEvent(
    current: List<SiteVersionEntry>, eventSiteId: String, eventVersion: Int,
    phase: SiteBuildPhase, previewUrl: String?, downloadUrl: String?,
): List<SiteVersionEntry> {
    val base = if (current.any { it.siteId != eventSiteId }) emptyList() else current
    val existing = base.firstOrNull { it.version == eventVersion }
    val updated = if (existing == null) {
        SiteVersionEntry(eventSiteId, eventVersion, ready = phase == SiteBuildPhase.Ready, failed = phase == SiteBuildPhase.Failed, stable = phase == SiteBuildPhase.Ready, previewUrl = previewUrl, downloadUrl = downloadUrl ?: "")
    } else {
        existing.copy(ready = phase == SiteBuildPhase.Ready, failed = phase == SiteBuildPhase.Failed, stable = if (phase == SiteBuildPhase.Ready) true else existing.stable, previewUrl = previewUrl ?: existing.previewUrl, downloadUrl = downloadUrl ?: existing.downloadUrl)
    }
    val merged = (base.filterNot { it.version == eventVersion } + updated).sortedBy { it.version }
    return if (updated.stable) merged.map { it.copy(stable = it.version == eventVersion) } else merged
}

fun stableSiteUrls(siteId: String, version: Int): Pair<String, String> =
    "/api/sites/$siteId/v$version/preview/index.html" to "/api/sites/$siteId/v$version/download"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/domain/src/commonMain/.../domain/StaticSites.kt feature/chat/domain/src/commonTest/.../StaticSitesTest.kt
git commit -m "feat(chat): static sites domain and pure event helpers"
```

### Task 7: KtorSitesDataSource + tests + DI

**Files:**
- Create: `feature/chat/data/.../data/SitesDtos.kt` (DTOs + mapper + `KtorSitesDataSource`)
- Test: `feature/chat/data/.../KtorSitesDataSourceTest.kt`
- Modify: `feature/chat/data/.../data/ChatDataModule.kt`

**Interfaces:**
- Consumes: `SitesRemoteDataSource` (Task 6), `httpClient.get/getBytes/post/postForResponse`.
- Produces: bound `SitesRemoteDataSource` for Tasks 8, 13.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun by_session_maps_entries() = runTest {
    val source = sitesSource(
        path = "/api/sites/by-session/abc",
        body = """{"sites":[{"siteId":"s1","version":2,"stableVersion":1,"status":"ready","previewUrl":"/api/sites/s1/v2/preview/index.html","downloadUrl":"/api/sites/s1/v2/download","updatedAt":"t"}]}""",
    )
    val entry = (source.sitesBySession("abc") as Result.Success).data.single()
    assertThat(entry.version).isEqualTo(2)
    assertThat(entry.stableVersion).isEqualTo(1)
}

@Test
fun retry_non_failed_maps_conflict() = runTest {
    val source = sitesSource(
        path = "/api/sites/s1/retry",
        status = HttpStatusCode.Conflict,
        body = """{"error":"only failed builds can be retried"}""",
    )
    val result = source.retrySite("s1")
    assertThat((result as Result.Error).error.kind).isEqualTo(DataError.Network.Kind.CONFLICT)
}

@Test
fun download_bytes_success() = runTest {
    val source = sitesSourceBytes(path = "/api/sites/s1/v1/download", bytes = byteArrayOf(1, 2, 3))
    val result = source.downloadSite("s1", 1)
    assertThat((result as Result.Success).data.size).isEqualTo(3)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: KtorSitesDataSource`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.getBytes
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.postForResponse
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.feature.chat.domain.SessionSiteEntry
import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.SiteBuildState
import co.ratmo.anreal.feature.chat.domain.SiteStatus
import co.ratmo.anreal.feature.chat.domain.SitesRemoteDataSource
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable

@Serializable
data class SessionSiteEntryDto(
    val siteId: String, val version: Int, val stableVersion: Int? = null,
    val status: String = "queued", val previewUrl: String? = null,
    val downloadUrl: String = "", val updatedAt: String = "",
)

@Serializable
data class SessionSitesDto(val sites: List<SessionSiteEntryDto> = emptyList())

@Serializable
data class SiteRetryDto(val siteId: String, val version: Int, val status: String = "queued")

@Serializable
data class SiteRollbackRequestDto(val version: Int)

@Serializable
data class SiteManifestDto(val siteId: String, val stableVersion: Int? = null, val version: Int = 0)

fun SessionSiteEntryDto.toEntry(): SessionSiteEntry = SessionSiteEntry(
    siteId = siteId, version = version, stableVersion = stableVersion,
    status = when (status) { "running" -> SiteStatus.Running; "ready" -> SiteStatus.Ready; "failed" -> SiteStatus.Failed; else -> SiteStatus.Queued },
    previewUrl = previewUrl, downloadUrl = downloadUrl, updatedAt = updatedAt,
)

class KtorSitesDataSource(private val httpClient: HttpClient) : SitesRemoteDataSource {
    override suspend fun sitesBySession(sessionId: String): Result<List<SessionSiteEntry>, DataError.Network> =
        httpClient.get<SessionSitesDto>(route = "/api/sites/by-session/$sessionId")
            .map { dto -> dto.sites.map { it.toEntry() } }

    override suspend fun downloadSite(siteId: String, version: Int): Result<ByteArray, DataError.Network> =
        httpClient.getBytes(route = "/api/sites/$siteId/v$version/download")

    override suspend fun retrySite(siteId: String): Result<SiteBuildState, DataError.Network> =
        httpClient.postForResponse<SiteRetryDto>(route = "/api/sites/$siteId/retry")
            .map { dto -> SiteBuildState(dto.siteId, dto.version, SiteBuildPhase.Starting, "Mengulang build.") }

    override suspend fun rollbackSite(siteId: String, version: Int): Result<Int, DataError.Network> =
        httpClient.post<SiteRollbackRequestDto, SiteManifestDto>(route = "/api/sites/$siteId/rollback", body = SiteRollbackRequestDto(version))
            .map { dto -> dto.stableVersion ?: version }
}
```

Preview is a URL string for WebView — never fetched manually, no traversal test client-side (server returns 400; surfaced via `DataError.Network` if ever called).

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:data:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/data/src/commonMain/.../data/SitesDtos.kt feature/chat/data/src/commonTest/.../KtorSitesDataSourceTest.kt feature/chat/data/src/commonMain/.../data/ChatDataModule.kt
git commit -m "feat(chat): static sites ktor data source"
```

### Task 8: Stream parser + reducer for site events

**Files:**
- Modify: `.../domain/stream/ChatModels.kt` (events + state fields)
- Modify: `.../domain/stream/ChatStreamParser.kt` (`parseDataEvent`)
- Modify: `.../domain/stream/ChatReducer.kt` (reduce site events)
- Test: `.../domain/stream/SiteBuildStreamTest.kt` (parser + reducer, inline JSONL frames)

**Interfaces:**
- Consumes: pure helpers (Task 6).
- Produces: `ChatThreadState.siteBuild/siteVersions` for Tasks 9, 13.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun parser_maps_site_build_progress() {
    val event = parseStreamLine("""{"type":"data","name":"siteBuildProgress","data":{"siteId":"s1","version":1,"phase":"building","message":"Membangun hero."}}""")
    val progress = (event as StreamEnvelope.Event).event as ChatStreamEvent.SiteBuildProgress
    assertThat(progress.payload.phase).isEqualTo(SiteBuildPhase.Building)
}

@Test
fun parser_maps_site_build_ready() {
    val event = parseStreamLine("""{"type":"data","name":"siteBuildReady","data":{"siteId":"s1","version":1,"previewUrl":"/api/sites/s1/v1/preview/index.html","screenshotUrl":null,"downloadUrl":"/api/sites/s1/v1/download"}}""")
    val ready = (event as StreamEnvelope.Event).event as ChatStreamEvent.SiteBuildReady
    assertThat(ready.payload.downloadUrl).isEqualTo("/api/sites/s1/v1/download")
}

@Test
fun reducer_applies_progress_then_ready() {
    val start = ChatThreadState()
    val afterProgress = start.reduce(progressEnvelope())
    assertThat(afterProgress.siteBuild?.phase).isEqualTo(SiteBuildPhase.Building)
    val afterReady = afterProgress.reduce(readyEnvelope())
    assertThat(afterReady.siteBuild?.phase).isEqualTo(SiteBuildPhase.Ready)
    assertThat(afterReady.siteVersions.single().stable).isTrue()
}
```

Check `parseStreamLine` exact name/signature in `ChatStreamParser.kt` before writing (subagent reported `parseStreamLine/parseStreamLines`; confirm arity: single line → `StreamEnvelope?`).

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: SiteBuildProgress`.

- [ ] **Step 3: Write minimal implementation**

`ChatModels.kt`:

```kotlin
data class SiteBuildProgress(val siteId: String, val version: Int, val phase: SiteBuildPhase, val message: String)
data class SiteBuildReady(val siteId: String, val version: Int, val previewUrl: String?, val downloadUrl: String)

// in ChatStreamEvent:
data class SiteBuildProgressEvent(val payload: SiteBuildProgress) : ChatStreamEvent
data class SiteBuildReadyEvent(val payload: SiteBuildReady) : ChatStreamEvent

// in ChatThreadState:
val siteBuild: SiteBuildState? = null,
val siteVersions: List<SiteVersionEntry> = emptyList(),
```

Name the event classes `SiteBuildProgress`/`SiteBuildReady` directly if no clash with domain payloads — keep payload types in `StaticSites.kt` out of the event names: use `ChatStreamEvent.SiteBuildProgress(val siteId, val version, val phase: SiteBuildPhase, val message: String)` and `ChatStreamEvent.SiteBuildReady(val siteId, val version, val previewUrl: String?, val downloadUrl: String?)`. Simpler, no wrapper clash.

`ChatStreamParser.kt` — extend `parseDataEvent`:

```kotlin
private fun parseDataEvent(event: JsonObject, type: String): ChatStreamEvent {
    return when (event.string("name")) {
        "deepResearchProgress" -> parseDeepResearch(event, type)
        "siteBuildProgress" -> {
            val data = event["data"] as? JsonObject ?: return ChatStreamEvent.Unknown(type)
            val phase = sitePhase(data.string("phase")) ?: return ChatStreamEvent.Unknown(type)
            ChatStreamEvent.SiteBuildProgress(
                siteId = data.string("siteId") ?: return ChatStreamEvent.Unknown(type),
                version = data.int("version") ?: return ChatStreamEvent.Unknown(type),
                phase = phase,
                message = data.string("message").orEmpty(),
            )
        }
        "siteBuildReady" -> {
            val data = event["data"] as? JsonObject ?: return ChatStreamEvent.Unknown(type)
            ChatStreamEvent.SiteBuildReady(
                siteId = data.string("siteId") ?: return ChatStreamEvent.Unknown(type),
                version = data.int("version") ?: return ChatStreamEvent.Unknown(type),
                previewUrl = data.string("previewUrl"),
                downloadUrl = data.string("downloadUrl").orEmpty(),
            )
        }
        else -> ChatStreamEvent.Unknown(type)
    }
}
```

Extract existing deep-research body into `parseDeepResearch(event, type)` unchanged; `sitePhase` maps `starting|planning|building|bundling|preview|ready|failed`.

`ChatReducer.kt` — add branches:

```kotlin
is ChatStreamEvent.SiteBuildProgress -> advanced.copy(
    siteBuild = applySiteBuildEvent(advanced.siteBuild, event.siteId, event.version, event.phase, event.message),
    siteVersions = applySiteVersionEvent(advanced.siteVersions, event.siteId, event.version, event.phase, previewUrl = null, downloadUrl = null),
)
is ChatStreamEvent.SiteBuildReady -> advanced.copy(
    siteBuild = applySiteBuildEvent(advanced.siteBuild, event.siteId, event.version, SiteBuildPhase.Ready, "", event.previewUrl, event.downloadUrl),
    siteVersions = applySiteVersionEvent(advanced.siteVersions, event.siteId, event.version, SiteBuildPhase.Ready, event.previewUrl, event.downloadUrl),
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/domain/src/commonMain/.../stream/ChatModels.kt feature/chat/domain/src/commonMain/.../stream/ChatStreamParser.kt feature/chat/domain/src/commonMain/.../stream/ChatReducer.kt feature/chat/domain/src/commonTest/.../stream/SiteBuildStreamTest.kt
git commit -m "feat(chat): site build stream events and reducer"
```

### Task 9: Selection store + ChatViewModel wiring

**Files:**
- Create: `feature/chat/data/.../data/DataStoreEnhancementSelectionStore.kt`
- Modify: `feature/chat/presentation/.../presentation/ChatViewModel.kt` (capabilities counts, selection default-all-enabled on first catalog load, persist last selection, pass ids into `ChatRunOptions`, site poll fallback + retry/rollback actions)
- Test: extend `ChatViewModelTest.kt` with fakes (`FakeSkillsSource`, `FakeSitesSource`, `FakeSelectionStore`)

**Interfaces:**
- Consumes: sources (Tasks 3, 5, 7), `EnhancementSelectionStore` (Task 6), metadata ids (Task 1), thread site state (Task 8).
- Produces: `ChatState.skillsSelection/siteBuilds` for Task 10/13.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun first_catalog_load_defaults_selection_to_all_enabled() = runTest {
    val viewModel = chatViewModel(skills = listOf(Skill("s1", "a", "d", status = SkillStatus.Active, isEnabled = true)))
    viewModel.onAction(ChatAction.OnCapabilitiesLoaded(...))
    assertThat(viewModel.state.value.selectedSkillIds).isEqualTo(listOf("s1"))
}
```

Shape the test to the real `ChatViewModel`/`ChatAction` API — read `ChatViewModel.kt` + `ChatViewModelTest.kt` setup first (fakes over mocks, `Dispatchers.setMain(UnconfinedTestDispatcher())`); mirror existing capability/catalog test blocks.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:chat:presentation:testAndroidHostTest --console=plain`
Expected: FAIL — `Unresolved reference: selectedSkillIds` (or action missing).

- [ ] **Step 3: Write minimal implementation**

- `DataStoreEnhancementSelectionStore(store: DataStore<Preferences>)` with keys `skills_selection`, `mcp_selection` (comma-joined ids, caps 20/5, `intersectWithCatalog` applied by callers).
- `ChatViewModel`: on capabilities/catalog load, `selectionStore` empty → default all enabled (`Skill: isEnabled && status==Active`; MCP: `isEnabled && status==Ok`); toggle actions persist; `sendMessage` builds `ChatRunOptions(..., skillIds = state.selectedSkillIds, mcpServerIds = state.selectedMcpServerIds)`; stream `SiteBuildProgress/Ready` envelopes update `siteBuilds[sessionId]`/`siteVersions[sessionId]` via Task 6 helpers; entering a session triggers `sitesBySession` poll fallback merging versions (streaming state wins while `status==Streaming`); `OnSiteRetry` optimistic `Starting`; `OnSiteRollback(version)` updates stable on success, error event on 409.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:chat:presentation:testAndroidHostTest :feature:chat:data:testAndroidHostTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/data/src/commonMain/.../data/DataStoreEnhancementSelectionStore.kt feature/chat/presentation/src/commonMain/.../presentation/ChatViewModel.kt feature/chat/presentation/src/commonTest/.../ChatViewModelTest.kt
git commit -m "feat(chat): enhancement selection and site wiring"
```

### Task 10: Composer rows + management screens + site panel UI

**Files:**
- Modify: `.../presentation/component/ComposerSheets.kt` (Skills/MCP `EnhancementRow`s), `ComposerBar.kt` if trigger strip needed.
- Create: `.../presentation/component/SkillsSheets.kt` (`SkillsListUi`, `SkillEditorUi`, `SkillsViewModel` + State/Action/Event in same feature presentation package), `McpSheets.kt` (mirror + test checklist), `SiteBuildPanel.kt` (card + preview dialog + version select + retry/rollback buttons).
- Previews: every state (loading/empty/error/populated + in-flight) with `@AnrealPreviews` + `AnrealPreview` light/dark.

**Interfaces:**
- Consumes: Task 9 state/actions.
- Produces: visual QA via previews + Roborazzi.

- [ ] **Step 1: Write the failing preview/test**

Add `@AnrealPreviews` composables for `SiteBuildPanel` ready/failed/running + `SkillsListScreen` populated/empty/error. For Roborazzi, mirror `ChatScreensScreenshotTest.kt` pattern in `androidHostTest` if present; else previews suffice for this task and screenshots land in Task 11 verification.

- [ ] **Step 2: Run to verify it fails**

Run compile: `.\gradlew.bat :feature:chat:presentation:compileAndroidMain --console=plain`
Expected: FAIL — missing composables.

- [ ] **Step 3: Write minimal implementation**

Follow `FeaturesSheet` switch pattern, `WorkspaceScreen` list + `AlertDialog` create/rename/delete, `AccountSettingsLayout` drill-down (160ms directional `AnimatedContent`), `AnrealBottomSheet` wrappers, `AnrealEmpty/AnrealError/AnrealSkeleton`, `UiText` errors, resource `contentDescription`. MCP editor: Test button → `testConnection` → checklist `Tools (checked/total)` → Save enabled only with fresh review + ≥1 tool; token `PasswordVisualTransformation`, never prefilled. Site panel in `composerTopSlot`: phase steps, preview dialog with Android `WebView` (`settings.javaScriptEnabled = true`, no auth headers), download via FileKit save, retry/rollback buttons dispatching Task 9 actions. iOS: `expect/actual` stubs compiling.

- [ ] **Step 4: Run to verify it passes**

Run: `.\gradlew.bat :feature:chat:presentation:compileAndroidMain :feature:chat:presentation:compileKotlinIosArm64 --console=plain`
Expected: PASS (warnings are errors — zero warnings).

- [ ] **Step 5: Commit**

```bash
git add feature/chat/presentation/src/commonMain/.../presentation/component/
git commit -m "feat(chat): skills mcp site UI"
```

### Task 11: Nav + DI + full verification

**Files:**
- Modify: `.../presentation/ChatGraph.kt` (`SkillsRoute`, `McpRoute`), `ChatPresentationModule.kt` (`viewModelOf(::SkillsViewModel)`, `viewModelOf(::McpViewModel)`), `.../data/ChatDataModule.kt` final check.

**Interfaces:**
- Consumes: all tasks.
- Produces: shippable slice.

- [ ] **Step 1: Write the failing check**

```kotlin
@Test
fun chat_graph_contains_skills_and_mcp_routes() {
    assertThat(chatDestinations).contains(SkillsRoute::class, McpRoute::class)
}
```

If no destination registry exists, replace with a compile-level check: navigate actions in `ChatGraph.kt` reference the new routes (this step then just runs the compile in Step 2).

- [ ] **Step 2: Run full unit + compile to verify**

Run: `.\gradlew.bat :feature:chat:domain:testAndroidHostTest :feature:chat:data:testAndroidHostTest :feature:chat:presentation:testAndroidHostTest :feature:chat:presentation:compileAndroidMain :feature:chat:presentation:compileKotlinIosArm64 --console=plain`
Expected: all green (first run FAILs on missing routes).

- [ ] **Step 3: Write minimal implementation**

Type-safe `@Serializable SkillsRoute`/`McpRoute`, intra-feature `navController` navigation from composer gear rows, transitions from root `NavHost` (`anrealEnter/anrealExit`) — no new graph. Manual staging checklist: skill draft→review→enable→chat follows; MCP test→save→disable→count 0; site request→progress→preview→zip→retry; regression old-capabilities parse, chat without ids, public preview logged-out.

- [ ] **Step 4: Re-run to verify green**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/presentation/src/commonMain/.../presentation/ChatGraph.kt feature/chat/presentation/src/commonMain/.../presentation/ChatPresentationModule.kt
git commit -m "feat(chat): skills mcp navigation and di"
```

## Self-Review

- Spec coverage: §1 arch → Tasks 1–11 file map; §2 DTOs/validation → Tasks 1–5 (client pre-validation + server authoritative; per-field server `issues` surface as readable form-level `serverMessage` since `parseServerErrorPayload` keeps primitives only — NO core:data change, documented); §3 flow → Tasks 6–9 (default-all-enabled, persist, metadata, reducer + poll, preview URL only, retry/rollback); §4 UI → Task 10 (rows, management, panel, tokens, previews); §5 errors/tests → per-task MockEngine cases (400 issues message, 404 foreign, test `ok:false`, retry/rollback 409, download 404 via `getBytes` error path).
- Placeholder scan: no TBD/TODO in product code paths (only iOS actual stubs allowed); every step has exact code + exact run command + exact commit paths.
- Type consistency: `SkillStatus.Active/Invalid/Draft`, `McpStatus.Ok/Untested/Error`, `SiteStatus/SiteBuildPhase`, `ChatCapabilities(userSkillsCount,userMcpCount)`, `ChatRunOptions(skillIds,mcpServerIds)` used identically across tasks; DTO field names match backend contract (`allowedToolsJson`, `stableVersion`, `previewUrl/downloadUrl`).
- Review Focus: each of the 5 lines is pinned — frontmatter mismatch → Task 2 test + Task 10 guard; MCP re-test guard → Task 10; siteId reset → Task 6 test; empty by-session during run → Task 9 poll-merge rule; logged-out preview → Task 10 WebView no-auth.

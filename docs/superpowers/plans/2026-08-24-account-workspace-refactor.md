# Account & Workspace Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor Account Settings to drill-down menu + Workspace to gallery-hybrid gallery while keeping Anreal animated radial gradient, dynamic color, and unifying skeleton loading to pen.dev style.

**Architecture:** Keep `feature:chat:presentation` owned `AccountRoute` and `feature:workspace:presentation` browser; UI changes are presentation-only, ViewModels keep StateFlow + Channel. Extract reusable `Skeleton` and `GroupedCard` to `core:design-system`. Navigation stays via callbacks in `:app`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material3 Expressive + Haze (`GlassSurface`, `AnrealAtmosphere`), Koin, Room, Coil3, Material Symbols Rounded, Roborazzi, Turbine.

## Global Constraints
- Module deps: presentation→ own domain + core:domain/presentation/design-system; no feature↔feature deps (AGENTS.md §2)
- Colors: `MaterialTheme.colorScheme.*` only, never `#E8A317` raw (DESIGN.md §2)
- Spacing: 4dp grid, `AnrealSpacing`, `screenCompact` 16dp, touch 48dp
- Motion: `AnrealMotion` tokens, `graphicsLayer` only, no scale(0), ≤300ms daily, reduce-motion fade
- Glass: Haze via `core:design-system` wrappers only, `AnrealAtmosphere` single at NavHost
- State: `State` holds loading/empty/error/populated, previews for each
- Strings: `AnrealCopy` resources, `UiText`
- Build: `.\gradlew.bat :app:assembleDebug --console=plain`, `allWarningsAsErrors`

---

### Task 1: Skeleton refactor to pen.dev style

**Files:**
- Modify: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/component/AnrealSkeleton.kt`
- Modify: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/component/GlassSurface.kt` (if needed for inset)
- Test: `core/design-system/src/commonTest/kotlin/.../AnrealSkeletonTest.kt` (new)
- Preview: `.../preview/AnrealPreview.kt` (update)

**Interfaces:**
- Consumes: `AnrealSpacing`, `GlassTone`
- Produces: `AnrealSkeleton(height, width?)`, `AnrealSkeletonList`, `WorkspaceImageSkeleton` updated to use inset padding `AnrealSpacing.screenCompact`

- [ ] **Step 1: Write failing test for inset skeleton**
```kotlin
@Test fun skeleton_hasInsetPadding() {
  // assert AnrealSkeletonList uses screenCompact padding
}
```
- [ ] **Step 2: Run test fails**
Run: `.\gradlew.bat :core:design-system:testDebugUnitTest --tests "*AnrealSkeletonTest*" --console=plain`
Expected: FAIL
- [ ] **Step 3: Implement skeleton with GlassSurface + fillContainer + cornerRadius 12, height param, shimmer via `LoadingIndicator` style**
- [ ] **Step 4: Pass test + add @AnrealPreviews for loading/empty/error**
- [ ] **Step 5: Commit**
```bash
git add core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/component/AnrealSkeleton.kt
git commit -m "refactor(design): unify skeleton to pen.dev inset style"
```

### Task 2: Account Settings — drill-down menu (remove segmented tabs, FAB divider, home line)

**Files:**
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/account/component/AccountSettingsLayout.kt`
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/account/AccountViewModel.kt` (add Appearance section, keep State)
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/account/AccountState.kt`
- Modify: `DESIGN.md:11` — document drill-down vs segmented
- Test: `feature/chat/presentation/src/commonTest/kotlin/co/ratmo/anreal/feature/chat/presentation/account/AccountViewModelTest.kt`
- Preview: `AccountScreen.kt` previews

**Interfaces:**
- Consumes: `AccountState`, `AppThemeMode`, `GlassSurface`, `AnrealAtmosphere`, `ChatBottomSheet`
- Produces: `AccountMenu`, `AppearanceDetail`, `UsageDetail` composables, `onSelectSection` still via `AccountAction.OnSelectSection`

- [ ] **Step 1: Write VM test for Appearance section navigation**
```kotlin
@Test fun `select Appearance updates section`() = runTest { ... }
```
- [ ] **Step 2: Run fails**
- [ ] **Step 3: Implement menu card with 48dp Icon 40→48 + chevron, grouped card, danger zone card (not FAB), no top divider (GlassTopBar frosted=false), no home line**
- [ ] **Step 4: Verify with `ChatViewModelTest` + screenshot Roborazzi `AccountScreensScreenshotTest`**
- [ ] **Step 5: Commit**

### Task 3: Workspace — gallery hybrid (grid/list toggle, chips, 48dp overflow sheet)

**Files:**
- Modify: `feature/workspace/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/workspace/presentation/WorkspaceScreen.kt`
- Modify: `feature/workspace/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/workspace/presentation/WorkspaceState.kt` (add `viewMode: Grid/List`)
- Modify: `feature/workspace/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/workspace/presentation/WorkspaceViewModel.kt`
- Modify: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/component/GlassSurface.kt` (if needed)
- Test: `feature/workspace/presentation/src/commonTest/kotlin/co/ratmo/anreal/feature/workspace/presentation/WorkspaceViewModelTest.kt`
- Preview: `WorkspaceScreen` previews for grid/list/loading/empty/error

**Interfaces:**
- Consumes: `WorkspaceState`, `WorkspaceSection`, `ChatBottomSheet`
- Produces: `WorkspaceCard` with 48dp overflow, `WorkspaceImageGrid`, `FilterChips`, `LoadMoreRow`

- [ ] **Step 1: Write test for grid toggle**
```kotlin
@Test fun `toggle viewMode switches grid to list`() = runTest { ... }
```
- [ ] **Step 2: Run fails**
- [ ] **Step 3: Implement `SegmentedGridListToggle` + `LazyVerticalGrid` 2-col for Images, `LazyColumn` for Projects/Documents, overflow `IconButton` 48dp → `ChatBottomSheet` with Rename/Delete/Share**
- [ ] **Step 4: Verify with `WorkspaceViewModelTest` + screenshots**
- [ ] **Step 5: Commit**

### Task 4: Atmosphere & Dynamic color regression

**Files:**
- Modify: `core/design-system/src/androidMain/kotlin/co/ratmo/anreal/core/designsystem/theme/ApplySystemBars.android.kt` (verify transparent + theme-aware)
- Modify: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/AnrealTheme.kt`
- Modify: `shared/src/commonMain/kotlin/co/ratmo/anreal/App.kt` (ensure single AnrealAtmosphere around NavHost)
- Test: manual device + `ApplySystemBarsTest`

**Interfaces:**
- Consumes: `AnrealTheme`, `dynamicLightColorScheme`
- Produces: aurora still animated radial gradient (16-32s), reducedMotion hides it

- [ ] **Step 1: Write test for theme switch 200ms**
- [ ] **Step 2: Run**
- [ ] **Step 3: Verify on Infinix X6853 via `adb exec-out screencap` vs pen design**
- [ ] **Step 4: Commit**

### Task 5: Verification — build, tests, screenshots vs pen

**Files:**
- Run: `.\gradlew.bat :app:assembleDebug --console=plain`
- Run: `.\gradlew.bat :feature:chat:presentation:compileAndroidMain :feature:chat:presentation:compileKotlinIosArm64 --console=plain`
- Run: Roborazzi `record`/`verify`
- Device: `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" exec-out screencap -p` compare to `design-resarch/combined/anreal-account-workspace-v1.pen` screenshots

- [ ] **Step 1: Build debug succeeds with no warnings**
- [ ] **Step 2: All unit tests pass**
- [ ] **Step 3: Screenshots match pen.dev (check no top divider, no home line, frames not overlapping, skeletons inset)**
- [ ] **Step 4: Commit final**


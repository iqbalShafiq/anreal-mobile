# Design System Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Geist typography + honor Android high-contrast in Anreal's design system, and align `DESIGN.md` / `design-resarch` with the real MaterialKolor Expressive palette.

**Architecture:** Geist fonts live as compose resources in `:core:design-system`; one `anrealTypography()` builds the full M3 scale (including Expressive emphasized variants) and is wired into `MaterialExpressiveTheme`. High contrast is a platform `expect/actual` (`UiModeManager.getContrast()` on API 34+, legacy `high_text_contrast_enabled` setting below) fed into both color schemes as `contrastLevel` and into a new `LocalAnrealHighContrast` that border/glass helpers read.

**Tech Stack:** Kotlin Multiplatform · Compose Multiplatform 1.11.1 · Material3 1.11.0-alpha07 (Expressive) · MaterialKolor 5.0.0 · Haze 1.7.2 · Robolectric + Roborazzi 1.72.0 · JUnit4 + assertk.

**Spec:** `docs/superpowers/specs/2026-09-26-design-system-contract-design.md`

## Global Constraints

- `allWarningsAsErrors` aktif: tidak boleh ada unused import/parameter; jangan tinggalkan kode mati.
- Gradle selalu dijalankan langsung di PowerShell dari root repo: `.\gradlew.bat <task> --console=plain` (tanpa `cmd /c`, tanpa `Start-Process`).
- Sumber font: commit `main` repo `vercel/geist-font` (SIL OFL 1.1). Nama file lokal **lowercase underscore**: `geist_regular.ttf`, `geist_medium.ttf`, `geist_semibold.ttf`, `geist_mono_regular.ttf`, `OFL.txt`. Ukuran unduhan yang diharapkan: 126,048 / 127,660 / 127,872 / 149,284 / 4,383 byte.
- Paket generated compose resources dipatok: `co.ratmo.anreal.core.designsystem.resources` (di-set eksplisit di `core/design-system/build.gradle.kts`).
- Tidak ada string user-facing baru di plan ini; jika muncul, ikuti pola `AnrealCopy` + `UiText`.
- Warna/typography hanya lewat `AnrealTheme`/`MaterialTheme`; tidak ada token hardcoded baru di feature.
- `commonMain` harus tetap compile untuk iOS: `.\gradlew.bat :core:design-system:compileKotlinIosArm64 --console=plain`.
- Jangan menyentuh `design-resarch/*.pen` atau `gradle.properties` milik user. `design-resarch/README.md` hanya bagian `## Tokens` (sudah disetujui).
- Commit kecil per task, pesan conventional.

## Review Focus

1. **Font asset hilang/rusak** → accessor generated gagal saat build; family salah pasang ditangkap Task 1 test.
2. **Varian `*Emphasized` hilang** setelah swap Typography → Task 1 test mengiterasi seluruh 30 style.
3. **High-contrast OFF harus no-op** → Task 4 test mapping (`0f → false`, border default tetap `outlineVariant`).
4. **Nilai kontras medium (0.5)** → hairline menjadi `outline` dan skema naik proporsional (bukan 100%); Task 4 test mapping mencakup 0.5.
5. **Regresi metrik font pada fontScale besar** (tab/baris padat) → Task 2 mewajibkan review PNG padat; otomasi screenshot fontScale didelegasikan ke Spec A yang memiliki preview matrix.

---

### Task 1: Geist typography pipeline

**Files:**
- Create: `core/design-system/src/commonMain/composeResources/font/geist_regular.ttf`
- Create: `core/design-system/src/commonMain/composeResources/font/geist_medium.ttf`
- Create: `core/design-system/src/commonMain/composeResources/font/geist_semibold.ttf`
- Create: `core/design-system/src/commonMain/composeResources/font/geist_mono_regular.ttf`
- Create: `core/design-system/src/commonMain/composeResources/font/OFL.txt`
- Create: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/AnrealTypography.kt`
- Modify: `core/design-system/build.gradle.kts`
- Modify: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/AnrealTheme.kt`
- Modify: `DESIGN.md` (§3 dan tabel §13)
- Test: `core/design-system/src/androidHostTest/kotlin/co/ratmo/anreal/core/designsystem/theme/AnrealTypographyTest.kt`

**Interfaces:**
- Produces: `object AnrealFontFamily { @Composable fun Geist(): FontFamily; @Composable fun Mono(): FontFamily }` dan `@Composable fun anrealTypography(): Typography` — dipakai Task 3 (Mono) dan Task 4 (wiring theme).
- Consumes: —.

- [ ] **Step 1: Unduh aset font** (jalankan di PowerShell dari root repo)

```powershell
$fontDir = "core\design-system\src\commonMain\composeResources\font"
New-Item -ItemType Directory -Force -Path $fontDir | Out-Null
$base = "https://raw.githubusercontent.com/vercel/geist-font/main"
Invoke-WebRequest "$base/fonts/Geist/ttf/Geist-Regular.ttf" -OutFile "$fontDir\geist_regular.ttf"
Invoke-WebRequest "$base/fonts/Geist/ttf/Geist-Medium.ttf" -OutFile "$fontDir\geist_medium.ttf"
Invoke-WebRequest "$base/fonts/Geist/ttf/Geist-SemiBold.ttf" -OutFile "$fontDir\geist_semibold.ttf"
Invoke-WebRequest "$base/fonts/GeistMono/ttf/GeistMono-Regular.ttf" -OutFile "$fontDir\geist_mono_regular.ttf"
Invoke-WebRequest "$base/OFL.txt" -OutFile "$fontDir\OFL.txt"
Get-ChildItem $fontDir | Select-Object Name, Length
```

Expected: 5 file dengan ukuran sesuai Global Constraints (toleransi ±0 byte).

- [ ] **Step 2: Konfigurasi build** — `core/design-system/build.gradle.kts`

Tambahkan setelah blok `plugins { … }`:

```kotlin
compose.resources {
    packageOfResClass = "co.ratmo.anreal.core.designsystem.resources"
}
```

Dan tambahkan (permanen) di dalam `kotlin { … }` setelah blok `androidMain.dependencies`:

```kotlin
sourceSets.getByName("androidHostTest").dependencies {
    implementation(libs.junit)
    implementation(libs.robolectric)
    implementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 3: Tulis test yang gagal** — `AnrealTypographyTest.kt`

```kotlin
package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AnrealTypographyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun every_style_uses_geist() {
        var typography: Typography? = null
        composeTestRule.setContent {
            AnrealTheme(settings = ThemeSettings(dynamicColor = false)) {
                typography = MaterialTheme.typography
            }
        }
        composeTestRule.waitForIdle()
        val t = typography ?: error("typography not captured")
        val styles: List<Pair<String, TextStyle>> = listOf(
            "displayLarge" to t.displayLarge,
            "displayMedium" to t.displayMedium,
            "displaySmall" to t.displaySmall,
            "headlineLarge" to t.headlineLarge,
            "headlineMedium" to t.headlineMedium,
            "headlineSmall" to t.headlineSmall,
            "titleLarge" to t.titleLarge,
            "titleMedium" to t.titleMedium,
            "titleSmall" to t.titleSmall,
            "bodyLarge" to t.bodyLarge,
            "bodyMedium" to t.bodyMedium,
            "bodySmall" to t.bodySmall,
            "labelLarge" to t.labelLarge,
            "labelMedium" to t.labelMedium,
            "labelSmall" to t.labelSmall,
            "displayLargeEmphasized" to t.displayLargeEmphasized,
            "displayMediumEmphasized" to t.displayMediumEmphasized,
            "displaySmallEmphasized" to t.displaySmallEmphasized,
            "headlineLargeEmphasized" to t.headlineLargeEmphasized,
            "headlineMediumEmphasized" to t.headlineMediumEmphasized,
            "headlineSmallEmphasized" to t.headlineSmallEmphasized,
            "titleLargeEmphasized" to t.titleLargeEmphasized,
            "titleMediumEmphasized" to t.titleMediumEmphasized,
            "titleSmallEmphasized" to t.titleSmallEmphasized,
            "bodyLargeEmphasized" to t.bodyLargeEmphasized,
            "bodyMediumEmphasized" to t.bodyMediumEmphasized,
            "bodySmallEmphasized" to t.bodySmallEmphasized,
            "labelLargeEmphasized" to t.labelLargeEmphasized,
            "labelMediumEmphasized" to t.labelMediumEmphasized,
            "labelSmallEmphasized" to t.labelSmallEmphasized,
        )
        for ((name, style) in styles) {
            assertTrue(style.fontFamily is FontListFontFamily, "$name must use the Geist family")
            assertEquals(3, (style.fontFamily as FontListFontFamily).fonts.size, "$name must carry 3 Geist weights")
        }
    }

    @Test
    fun mono_family_is_single_weight() {
        var family: FontFamily? = null
        composeTestRule.setContent { family = AnrealFontFamily.Mono() }
        composeTestRule.waitForIdle()
        val resolved = family ?: error("mono family not captured")
        assertTrue(resolved is FontListFontFamily, "Mono must be a custom family")
        assertEquals(1, (resolved as FontListFontFamily).fonts.size)
    }
}
```

- [ ] **Step 4: Jalankan test untuk memastikan gagal**

Run: `.\gradlew.bat :core:design-system:testAndroidHostTest --tests "co.ratmo.anreal.core.designsystem.theme.AnrealTypographyTest" --console=plain`
Expected: FAIL — `every_style_uses_geist` gagal dengan "displayLarge must use the Geist family" (theme masih system font). `mono_family_is_single_weight` gagal karena `AnrealFontFamily` belum ada → **catatan**: test tidak akan compile sebelum Step 5; itu kegagalan yang diharapkan. Lanjut ke Step 5 tanpa menunggu perbaikan.

- [ ] **Step 5: Implementasi `AnrealTypography.kt`**

```kotlin
package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import co.ratmo.anreal.core.designsystem.resources.Res
import co.ratmo.anreal.core.designsystem.resources.geist_medium
import co.ratmo.anreal.core.designsystem.resources.geist_mono_regular
import co.ratmo.anreal.core.designsystem.resources.geist_regular
import co.ratmo.anreal.core.designsystem.resources.geist_semibold
import org.jetbrains.compose.resources.Font

/** Geist families bundled in :core:design-system. See composeResources/font/OFL.txt (SIL OFL 1.1). */
object AnrealFontFamily {

    @Composable
    fun Geist(): FontFamily = FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium),
        Font(Res.font.geist_semibold, FontWeight.SemiBold),
    )

    @Composable
    fun Mono(): FontFamily = FontFamily(
        Font(Res.font.geist_mono_regular, FontWeight.Normal),
    )
}

/** Full M3 type scale (baseline + Expressive emphasized) on the Geist family. */
@Composable
fun anrealTypography(): Typography = Typography().withFontFamily(AnrealFontFamily.Geist())

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = family),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = family),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = family),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = family),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = family),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = family),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = family),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = family),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = family),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = family),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = family),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = family),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = family),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = family),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = family),
)
```

- [ ] **Step 6: Wiring di `AnrealTheme.kt`** — tambahkan `typography = anrealTypography()` ke `MaterialExpressiveTheme`:

```kotlin
        MaterialExpressiveTheme(
            colorScheme = dynamicScheme ?: brandScheme,
            motionScheme = MotionScheme.standard(),
            typography = anrealTypography(),
            content = content,
        )
```

- [ ] **Step 7: Jalankan test + compile iOS**

Run: `.\gradlew.bat :core:design-system:testAndroidHostTest --tests "co.ratmo.anreal.core.designsystem.theme.AnrealTypographyTest" --console=plain`
Expected: PASS (2 test).
Run: `.\gradlew.bat :core:design-system:compileKotlinIosArm64 --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Selaraskan DESIGN.md**

Di §3 ganti baris: `Face: **Geist** (UI) + **Geist Mono** (tokens, timestamps, model ids). SIL OFL, same as web.` menjadi:

```markdown
Face: **Geist** (400/500/600) + **Geist Mono** (400), dibundel di `:core:design-system` sebagai compose resources (`Res.font.geist_*`; lisensi SIL OFL di `composeResources/font/OFL.txt`). Skala M3 penuh (termasuk varian *Emphasized*) dipasang lewat `anrealTypography()` di `MaterialExpressiveTheme`.
```

Di tabel §13 (Implementation map) tambahkan baris:

```markdown
| Typography | `anrealTypography()` → `MaterialExpressiveTheme(typography = …)`; keluarga Geist via compose resources |
```

- [ ] **Step 9: Commit**

```powershell
git add core/design-system/src/commonMain/composeResources/font core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/AnrealTypography.kt core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/AnrealTheme.kt core/design-system/build.gradle.kts core/design-system/src/androidHostTest DESIGN.md
git commit -m "feat(design-system): ship Geist typography"
```

---

### Task 2: Visual regression pass (Roborazzi)

**Files:**
- Modify (hanya jika ditemukan regresi): file layout feature terkait, mis. `feature/**/presentation/src/commonMain/.../*.kt`
- Refresh (tidak di-commit): `docs/design-review-m3/app/*.png`

**Interfaces:** — (verifikasi).

- [ ] **Step 1: Rekam ulang screenshot auth + workspace**

```
.\gradlew.bat :feature:auth:presentation:recordRoborazziAndroidHostTest :feature:workspace:presentation:recordRoborazziAndroidHostTest --console=plain
```

Expected: BUILD SUCCESSFUL; PNG baru di `feature/*/presentation/build/outputs/roborazzi/`.

- [ ] **Step 2: Rekam ulang chat (difilter; `ChatViewModelTest` punya kegagalan pre-existing di branch ini)**

```
.\gradlew.bat :feature:chat:presentation:recordRoborazziAndroidHostTest --tests "co.ratmo.anreal.feature.chat.presentation.ChatScreensScreenshotTest" --tests "co.ratmo.anreal.feature.chat.presentation.account.AccountScreensScreenshotTest" --tests "co.ratmo.anreal.feature.chat.presentation.SiteBuildPanelScreenshotTest" --tests "co.ratmo.anreal.feature.chat.presentation.VisualProbePhoneTest" --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Review PNG padat** (baca dengan tool `read`) — minimal daftar ini, bandingkan dengan tampilan sebelumnya di `docs/design-review-m3/app/`:

```
feature/auth/presentation/build/outputs/roborazzi/*AuthScreensScreenshotTest.boardingIdleLight.png
feature/auth/presentation/build/outputs/roborazzi/*AuthScreensScreenshotTest.loginErrorDark.png
feature/chat/presentation/build/outputs/roborazzi/*ChatScreensScreenshotTest.populatedChatLight.png
feature/chat/presentation/build/outputs/roborazzi/*ChatScreensScreenshotTest.streamingChatDark.png
feature/chat/presentation/build/outputs/roborazzi/*ChatScreensScreenshotTest.modelAndReasoningSheetLight.png
feature/chat/presentation/build/outputs/roborazzi/*account.AccountScreensScreenshotTest.accountPopulatedLight.png
feature/workspace/presentation/build/outputs/roborazzi/*WorkspaceScreensScreenshotTest.sitesPopulatedLight.png
feature/workspace/presentation/build/outputs/roborazzi/*WorkspaceScreensScreenshotTest.tasksPopulatedLight.png
```

Yang dilihat: label tab tidak terpotong, teks tombol tidak wrap ke 2 baris, baris padat (Account menu, model options) tidak clipping, titik-titik elipsis tidak muncul di akhir yang tidak diharapkan.

- [ ] **Step 4: Perbaikan minimal bila ada regresi** — contoh pola umum: naikkan `maxLines`/`minWidth` pada komponen yang terbukti terpotong, tanpa mengubah token. Setelah perbaikan, jalankan ulang Step 1–3. Bila tidak ada regresi: lewati tanpa perubahan.

- [ ] **Step 5: Perbarui galeri review (opsional, tidak di-commit)**

```powershell
Copy-Item "feature/auth/presentation/build/outputs/roborazzi/co.ratmo.anreal.feature.auth.presentation.AuthScreensScreenshotTest.boardingIdleLight.png" "docs/design-review-m3/app/auth-boarding-light.png" -Force
Copy-Item "feature/chat/presentation/build/outputs/roborazzi/co.ratmo.anreal.feature.chat.presentation.ChatScreensScreenshotTest.populatedChatLight.png" "docs/design-review-m3/app/chat-populated-light.png" -Force
Copy-Item "feature/chat/presentation/build/outputs/roborazzi/co.ratmo.anreal.feature.chat.presentation.account.AccountScreensScreenshotTest.accountPopulatedLight.png" "docs/design-review-m3/app/account-menu-light.png" -Force
Copy-Item "feature/workspace/presentation/build/outputs/roborazzi/co.ratmo.anreal.feature.workspace.presentation.WorkspaceScreensScreenshotTest.sitesPopulatedLight.png" "docs/design-review-m3/app/workspace-sites-light.png" -Force
```

- [ ] **Step 6: Commit (hanya bila Step 4 mengubah kode)**

```
git add <file-yang-diperbaiki>
git commit -m "fix(ui): adjust layout after Geist metrics"
```

---

### Task 3: Geist Mono application

**Files:**
- Modify: `feature/chat/presentation/src/commonMain/kotlin/co/ratmo/anreal/feature/chat/presentation/component/SkillsSheets.kt:728`
- Modify (kondisional): file yang me-render model-id/timestamp, bila grep Step 2 menemukannya.
- Modify: `DESIGN.md` (§3 kalimat mono).
- Test: `feature/chat/presentation/src/androidHostTest/.../VisualProbePhoneTest.kt` (rekam ulang, bukan test baru).

**Interfaces:**
- Consumes: `AnrealFontFamily.Mono()` dari Task 1.

- [ ] **Step 1: Konfirmasi titik monospace saat ini**

Run: `Select-String -Path (Get-ChildItem -Recurse -Filter *.kt feature,core).FullName -Pattern "FontFamily.Monospace"`
Expected: hanya `SkillsSheets.kt:728`.

- [ ] **Step 2: Cari kandidat tambahan (model id / timestamp numerik)**

```
Select-String -Path (Get-ChildItem -Recurse -Filter *.kt feature).FullName -Pattern "model\.id|modelId|HH:mm|formatTime|timestampLabel" | Select-Object -First 20
```

Jika ada tempat yang benar-benar menampilkan **id** model atau **timestamp numerik**, ubah `fontFamily`-nya ke `AnrealFontFamily.Mono()`. Jika tidak ada (hasil yang diperkirakan), lanjut Step 3 dan selesaikan dengan Step 5 versi "hanya editor".

- [ ] **Step 3: Ubah editor skill**

Di `SkillsSheets.kt` ganti:

```kotlin
textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
```

menjadi:

```kotlin
textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = AnrealFontFamily.Mono()),
```

Tambahkan import `co.ratmo.anreal.core.designsystem.theme.AnrealFontFamily`; hapus import `androidx.compose.ui.text.font.FontFamily` **hanya bila** tidak lagi dipakai di file itu.

- [ ] **Step 4: Verifikasi visual + compile**

```
.\gradlew.bat :feature:chat:presentation:recordRoborazziAndroidHostTest --tests "co.ratmo.anreal.feature.chat.presentation.VisualProbePhoneTest" --console=plain
```

Baca `feature/chat/presentation/build/outputs/roborazzi/*probeSkillsPopulated.png` dan `*probeSkillsEditor.png` — badan editor harus monospace, UI lain tidak berubah.

- [ ] **Step 5: Selaraskan DESIGN.md (§3)**

Ganti frasa `tokens, timestamps, model ids` mengikuti kenyataan. Bila hanya editor yang memakai mono, tulis:

```markdown
Geist Mono dipakai untuk badan editor skill; komponen token/diagnostik berikutnya mengikuti keluarga yang sama (`AnrealFontFamily.Mono()`).
```

Bila Step 2 menemukan titik tambahan, sebutkan titik-titik itu.

- [ ] **Step 6: Commit**

```
git add feature/chat/presentation DESIGN.md
git commit -m "feat(design-system): use Geist Mono in skill editor"
```

---

### Task 4: Platform high contrast

**Files:**
- Create: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/HighContrast.kt`
- Create: `core/design-system/src/androidMain/kotlin/co/ratmo/anreal/core/designsystem/theme/HighContrast.android.kt`
- Create: `core/design-system/src/iosMain/kotlin/co/ratmo/anreal/core/designsystem/theme/HighContrast.ios.kt`
- Modify: `core/design-system/src/commonMain/.../theme/AnrealTheme.kt`
- Modify: `core/design-system/src/commonMain/.../theme/PlatformDynamicColorScheme.kt`
- Modify: `core/design-system/src/androidMain/.../theme/PlatformDynamicColorScheme.android.kt`
- Modify: `core/design-system/src/commonMain/.../component/GlassSurface.kt`
- Modify: `core/design-system/src/commonMain/.../component/GlassDrawer.kt`
- Modify: `core/design-system/src/commonMain/.../preview/AnrealPreview.kt`
- Modify: `feature/auth/.../AuthScreensScreenshotTest.kt`, `feature/chat/.../ChatScreensScreenshotTest.kt`, `feature/workspace/.../WorkspaceScreensScreenshotTest.kt` (tambah 1 test masing-masing)
- Test: `core/design-system/src/androidHostTest/kotlin/co/ratmo/anreal/core/designsystem/theme/HighContrastTest.kt`

**Interfaces:**
- Produces: `@Composable fun rememberPlatformContrast(): Float`; `val LocalAnrealHighContrast: ProvidableCompositionLocal<Boolean>`; `fun glassHairlineColor(): Color`; `AnrealTheme(..., highContrast: Boolean? = null)`; `AnrealPreview(..., highContrast: Boolean = false)`.
- Consumes: `AnrealTheme` (Task 1), `glassChromeTargets` yang sudah ada.

- [ ] **Step 1: Pure mapping + test yang gagal** — `HighContrast.kt` (commonMain)

```kotlin
package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Android exposes platform contrast since API 34 (UiModeManager.getContrast()). */
internal const val PlatformContrastApiLevel = 34

/** True when the platform asks for elevated contrast (medium or high). */
val LocalAnrealHighContrast = staticCompositionLocalOf { false }

@Composable
expect fun rememberPlatformContrast(): Float

internal fun highContrastActive(platformContrast: Float): Boolean = platformContrast > 0f

internal fun contrastLevelFor(platformContrast: Float): Float = platformContrast.coerceIn(-1f, 1f)

internal fun platformContrastValue(
    sdkInt: Int,
    uiModeContrast: Float?,
    legacyEnabled: Boolean,
): Float = when {
    sdkInt >= PlatformContrastApiLevel -> uiModeContrast ?: 0f
    legacyEnabled -> 1f
    else -> 0f
}
```

Test `HighContrastTest.kt`:

```kotlin
package co.ratmo.anreal.core.designsystem.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HighContrastTest {

    @Test
    fun default_contrast_is_inactive() {
        assertFalse(highContrastActive(0f))
        assertEquals(0f, contrastLevelFor(0f))
    }

    @Test
    fun medium_and_high_contrast_are_active_minimum_is_not() {
        assertTrue(highContrastActive(0.5f))
        assertTrue(highContrastActive(1f))
        assertFalse(highContrastActive(-1f))
    }

    @Test
    fun contrast_level_is_clamped_passthrough() {
        assertEquals(-1f, contrastLevelFor(-2f))
        assertEquals(0.5f, contrastLevelFor(0.5f))
        assertEquals(1f, contrastLevelFor(2f))
    }

    @Test
    fun platform_value_maps_by_api_level() {
        assertEquals(1f, platformContrastValue(sdkInt = 33, uiModeContrast = null, legacyEnabled = true))
        assertEquals(0f, platformContrastValue(sdkInt = 33, uiModeContrast = null, legacyEnabled = false))
        assertEquals(0.5f, platformContrastValue(sdkInt = 34, uiModeContrast = 0.5f, legacyEnabled = false))
        assertEquals(0f, platformContrastValue(sdkInt = 34, uiModeContrast = null, legacyEnabled = true))
    }
}
```

Run: `.\gradlew.bat :core:design-system:testAndroidHostTest --tests "co.ratmo.anreal.core.designsystem.theme.HighContrastTest" --console=plain`
Expected: FAIL (compile — `rememberPlatformContrast` belum ada).

- [ ] **Step 2: Android actual** — `HighContrast.android.kt`

```kotlin
package co.ratmo.anreal.core.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Settings.Secure key; not exposed in the public SDK 37 stubs. */
private const val HighTextContrastEnabledKey = "high_text_contrast_enabled"

@Composable
actual fun rememberPlatformContrast(): Float {
    val context = LocalContext.current
    val uiModeManager = remember(context) { context.getSystemService(UiModeManager::class.java) }
    var contrast by remember(context) {
        mutableFloatStateOf(
            platformContrastValue(
                sdkInt = Build.VERSION.SDK_INT,
                uiModeContrast = uiModeManager?.contrastOrNull(),
                legacyEnabled = readLegacyHighTextContrast(context),
            ),
        )
    }
    DisposableEffect(context, uiModeManager) {
        if (Build.VERSION.SDK_INT >= PlatformContrastApiLevel && uiModeManager != null) {
            val listener = UiModeManager.ContrastChangeListener { value -> contrast = value }
            uiModeManager.addContrastChangeListener(context.mainExecutor, listener)
            onDispose { uiModeManager.removeContrastChangeListener(listener) }
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    contrast = platformContrastValue(
                        sdkInt = Build.VERSION.SDK_INT,
                        uiModeContrast = null,
                        legacyEnabled = readLegacyHighTextContrast(context),
                    )
                }
            }
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(HighTextContrastEnabledKey),
                false,
                observer,
            )
            onDispose { context.contentResolver.unregisterContentObserver(observer) }
        }
    }
    return contrast
}

private fun UiModeManager.contrastOrNull(): Float? =
    if (Build.VERSION.SDK_INT >= PlatformContrastApiLevel) contrast else null

private fun readLegacyHighTextContrast(context: Context): Boolean =
    Settings.Secure.getInt(context.contentResolver, HighTextContrastEnabledKey, 0) == 1
```

- [ ] **Step 3: iOS actual** — `HighContrast.ios.kt`

```kotlin
package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformContrast(): Float {
    // TODO(iOS): map UIAccessibilityDarkerSystemColorsEnabled / increase-contrast when iOS platform code lands.
    return 0f
}
```

- [ ] **Step 4: Skema + theme**

`PlatformDynamicColorScheme.kt` (commonMain) ganti signature:

```kotlin
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean, contrastLevel: Float): ColorScheme?
```

`PlatformDynamicColorScheme.android.kt`:

```kotlin
@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean, contrastLevel: Float): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return null
    }
    val context = LocalContext.current
    return if (darkTheme) {
        dynamicDarkColorScheme(context, contrastLevel)
    } else {
        dynamicLightColorScheme(context, contrastLevel)
    }
}
```

`AnrealTheme.kt` — ganti implementasi menjadi:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnrealTheme(
    settings: ThemeSettings = ThemeSettings(),
    reduceMotion: Boolean = false,
    reduceTransparency: Boolean = false,
    highContrast: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = settings.resolveDark(isSystemInDarkTheme())
    ApplySystemBars(darkTheme)
    val platformContrast = rememberPlatformContrast()
    val resolvedContrast = if (highContrast == null) {
        contrastLevelFor(platformContrast)
    } else {
        if (highContrast) 1f else 0f
    }
    val highContrastOn = highContrast ?: highContrastActive(platformContrast)
    val dynamicScheme = if (settings.dynamicColor) {
        platformDynamicColorScheme(darkTheme, resolvedContrast)
    } else {
        null
    }
    val brandScheme = rememberDynamicColorScheme(
        seedColor = Color(AnrealBrand.seedArgb),
        isDark = darkTheme,
        isAmoled = false,
        style = PaletteStyle.Expressive,
        contrastLevel = resolvedContrast.toDouble(),
    )

    CompositionLocalProvider(
        LocalAnrealReduceMotion provides (reduceMotion || rememberReduceMotion()),
        LocalAnrealReduceTransparency provides (reduceTransparency || highContrastOn || rememberReduceTransparency()),
        LocalAnrealHighContrast provides highContrastOn,
    ) {
        MaterialExpressiveTheme(
            colorScheme = dynamicScheme ?: brandScheme,
            motionScheme = MotionScheme.standard(),
            typography = anrealTypography(),
            content = content,
        )
    }
}
```

Catatan: bila `contrastLevel` bukan nama parameter MaterialKolor (compile error), periksa `rememberDynamicColorScheme` lalu sesuaikan nama; laporkan bila API berbeda.

- [ ] **Step 5: Hairline & glass helpers**

`GlassDrawer.kt` — tambah helper dan ubah `glassDrawerBorderColor`:

```kotlin
@Composable
fun glassHairlineColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (LocalAnrealHighContrast.current) scheme.outline else scheme.outlineVariant
}

@Composable
fun glassDrawerBorderColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val alpha = if (LocalAnrealHighContrast.current) 0.6f else if (dark) 0.18f else 0.28f
    return glassHairlineColor().copy(alpha = alpha)
}
```

`GlassSurface.kt` — default border dan `GlassChrome` memakai helper:

- Ganti `else -> scheme.outlineVariant.copy(alpha = 0.45f)` dengan `else -> glassHairlineColor().copy(alpha = 0.45f)`.
- Di `GlassChrome`, ganti `borderColor = scheme.outlineVariant.copy(alpha = borderAlpha)` dengan `borderColor = glassHairlineColor().copy(alpha = borderAlpha)`.
- Hapus import/variabel yang menjadi tidak terpakai (`scheme` di `GlassChrome` masih dipakai untuk tintTarget; pastikan tidak ada variabel yatim).

- [ ] **Step 6: Preview + screenshot high contrast**

`AnrealPreview.kt` tambah parameter:

```kotlin
@Composable
fun AnrealPreview(
    dark: Boolean? = null,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = dark ?: isSystemInDarkTheme()
    AnrealTheme(
        settings = ThemeSettings(
            mode = if (isDark) ThemeMode.Dark else ThemeMode.Light,
            dynamicColor = false,
        ),
        highContrast = highContrast,
        content = content,
    )
}
```

Tambahkan satu test per modul dengan menyalin test populated yang sudah ada di file yang sama, lalu menambahkan `highContrast = true` pada `AnrealPreview`:

- `AuthScreensScreenshotTest`: salin `boardingIdleLight` → `boardingHighContrastLight` (`AnrealPreview(dark = false, highContrast = true)`).
- `ChatScreensScreenshotTest`: salin `populatedChatLight` → `populatedChatHighContrastDark` (`AnrealPreview(dark = true, highContrast = true)`).
- `WorkspaceScreensScreenshotTest`: salin `sitesPopulatedLight` → `sitesHighContrastLight` (`AnrealPreview(dark = false, highContrast = true)`).

Catatan: `AnrealPreview` yang ada memakai `AnrealPreview(dark = …)`; tambahkan argumen `highContrast = true` tanpa mengubah state fixture.

- [ ] **Step 7: Test + screenshot + iOS**

Run: `.\gradlew.bat :core:design-system:testAndroidHostTest --tests "co.ratmo.anreal.core.designsystem.theme.HighContrastTest" --console=plain`
Expected: PASS (4 test).
Run: `.\gradlew.bat :core:design-system:compileKotlinIosArm64 --console=plain`
Expected: BUILD SUCCESSFUL.
Run: `.\gradlew.bat :feature:auth:presentation:recordRoborazziAndroidHostTest :feature:workspace:presentation:recordRoborazziAndroidHostTest --console=plain`
Expected: BUILD SUCCESSFUL; tiga PNG baru high-contrast (auth + workspace; chat pada Step 8).
Baca PNG high-contrast: border lebih tegas, kartu/panel solid, teks tetap terbaca.

- [ ] **Step 8: Rekam chat (termasuk test baru)**

```
.\gradlew.bat :feature:chat:presentation:recordRoborazziAndroidHostTest --tests "co.ratmo.anreal.feature.chat.presentation.ChatScreensScreenshotTest" --console=plain
```

Expected: BUILD SUCCESSFUL + `populatedChatHighContrastDark.png` ada.

- [ ] **Step 9: DESIGN.md §9.4**

Ganti baris tabel `| High contrast / \`isHighContrast\` | Hairlines become \`outline\`; raise container contrast |` menjadi:

```markdown
| High contrast | `rememberPlatformContrast()` — API 34+ `UiModeManager.getContrast()`, di bawahnya setting `high_text_contrast_enabled` — dikirim sebagai `contrastLevel` ke skema dynamic & brand; hairline memakai `outline`; glass menjadi solid (setara reduce transparency). iOS masih stub `0f`. |
```

- [ ] **Step 10: Commit**

```powershell
git add core/design-system feature/auth/presentation/src/androidHostTest feature/chat/presentation/src/androidHostTest feature/workspace/presentation/src/androidHostTest DESIGN.md
git commit -m "feat(design-system): honor platform high contrast"
```

---

### Task 5: Palette contract docs

**Files:**
- Modify: `DESIGN.md` (§1, §2)
- Modify: `design-resarch/README.md` (bagian `## Tokens`)
- Modify: `core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/ThemeSettings.kt` (komentar)

**Interfaces:** — (dokumen).

- [ ] **Step 1: DESIGN.md §1** — ganti kalimat `Dark is black-led (#050505 family), not charcoal-grey panels. Light is paper-led, not pure white.` menjadi:

```markdown
Dark is seed-derived (MaterialKolor `PaletteStyle.Expressive`): permukaan hangat gelap (`surface` `#171308`) dengan aksen lavender — bukan charcoal netral dan bukan `#050505` (`#050505` hanya tint kaca bubble). Light adalah krem hangat (`#FFF8F0`), bukan putih murni.
```

- [ ] **Step 2: DESIGN.md §2** — tambahkan blok tepat setelah paragraf `**Contrast rule:** …`:

```markdown
### Palette contract

Sumber kebenaran warna adalah **seed `#E8A317` + `PaletteStyle.Expressive`** (`rememberDynamicColorScheme`); hex di bawah adalah contoh hasil, bukan konstanta:

| Peran | Light | Dark |
|---|---|---|
| primary | `#774E8B` | `#E5B5FA` |
| primaryContainer | `#F6D9FF` | `#5E3672` |
| secondaryContainer | `#DDE8B3` | `#414B24` |
| tertiary | `#6C5E1C` | `#D9C679` |
| surface | `#FFF8F0` | `#171308` |
| surfaceContainer | `#F7EDDA` | `#231F14` |
| outlineVariant | `#D1C6AA` | `#4E4632` |
| error | `#BA1A1A` | `#FFB4AB` |
```

Dan pada baris `Fallback and "brand" mode:` ganti nama komponen menjadi `rememberDynamicColorScheme` (MaterialKolor) — bukan `DynamicMaterialExpressiveTheme`.

- [ ] **Step 3: design-resarch/README.md** — ganti baris `## Tokens` lama dengan:

```markdown
## Tokens

Warna app diturunkan dari seed `#E8A317` + `PaletteStyle.Expressive` (MaterialKolor): `$surface` light `#FFF8F0` / dark `#171308`, `$primary` `#774E8B` / `#E5B5FA`, `$secondaryContainer` `#DDE8B3` / `#414B24`, `$outlineVariant` `#D1C6AA` / `#4E4632`. Token di file `.pen` tetap otoritatif untuk **layout**; untuk **warna**, kontrak seed + style di atas yang berlaku. Amber `#E8A317` tetap dipakai untuk unread/storage pada ilustrasi, bukan sebagai fill UI.
```

- [ ] **Step 4: Komentar `ThemeSettings.kt`** — tambahkan di atas `object AnrealBrand`:

```kotlin
/** Seed + PaletteStyle.Expressive adalah kontrak palette; hex di DESIGN.md adalah contoh hasil. */
```

- [ ] **Step 5: Verifikasi dokumen**

Run: `Select-String -Path DESIGN.md -Pattern "black-led|#050505"`
Expected: hanya menyebut `#050505` pada konteks tint kaca (tidak ada klaim "dark is black-led").
Run: `Select-String -Path DESIGN.md -Pattern "Palette contract"`
Expected: 1 match.

- [ ] **Step 6: Commit**

```
git add DESIGN.md design-resarch/README.md core/design-system/src/commonMain/kotlin/co/ratmo/anreal/core/designsystem/theme/ThemeSettings.kt
git commit -m "docs: align palette contract with Expressive output"
```

---

## Self-review (dilakukan saat menulis plan)

- Cakupan spec: §4.1 → Task 1+3; §4.2 → Task 5; §4.3 → Task 4; §4.4 → Task 1 (deps) + Task 4 (test baru) + Task 2 (rekam ulang); §7 kriteria → Task 1/2/4.
- Tidak ada placeholder "TBD"; semua perintah dan kode konkret. Satu ketergantungan runtime didokumentasikan eksplisit: nama parameter `contrastLevel` (MaterialKolor) dan `contrastLevel` (Material3) diverifikasi saat compile di Task 4 Step 7.
- Tipe & nama konsisten: `AnrealFontFamily`/`anrealTypography` (Task 1) dipakai Task 3/4; `rememberPlatformContrast`/`LocalAnrealHighContrast`/`glassHairlineColor` (Task 4) tidak dipakai task lain.
- Review Focus tiap baris punya pin: (1)(2) Task 1 test; (3)(4) Task 4 test; (5) Task 2 Step 3 review PNG.

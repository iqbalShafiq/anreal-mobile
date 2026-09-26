# Spec — Kontrak Design System: Typografi Geist, Palette Expressive, High-Contrast

- **Status:** menunggu review Anda
- **Tanggal:** 26 September 2026
- **Siklus:** Spec B dari 3 (B → A → C). Spec A dan C ditulis terpisah.
- **Referensi desain:** `docs/design-review-m3/REVIEW.md`; keputusan brainstorming diambil via visual companion (`Expressive`, surface dark tetap, high-contrast diimplementasikan, Geist dikirim).

## 1. Keputusan yang sudah dikunci

| Topik | Keputusan |
|---|---|
| Typeface | Kirim **Geist** + **Geist Mono** (SIL OFL), wire ke Material 3 typography |
| Palette | **Pertahankan** `PaletteStyle.Expressive` dengan seed `#E8A317`; **dokumen yang menyesuaikan**, bukan kode |
| Permukaan dark | **Tetap** `surfaceContainer` seperti sekarang (opsi A — rasio 1.13 dipertahankan; keputusan sadar) |
| High contrast | **Implementasikan sekarang** (sistem → `contrastLevel` + hairline) |

## 2. Tujuan

1. Satukan kontrak tipografi: DESIGN.md menjanjikan Geist, kode memakai system font — setelah spec ini, kode memakai Geist dan DESIGN.md akurat.
2. Hilangkan drift dokumen palette: tiga sumber berbeda (`#050505`, `#0B1210`, hasil nyata `#171308`) menjadi satu kontrak: **seed + palette style**, dengan contoh hex hasil probe.
3. Hormati preferensi aksesibilitas high-contrast yang saat ini dijanjikan DESIGN.md §9.4 tapi belum ada di kode.

## 3. Non-tujuan (di luar spec ini)

- Mengubah palette, permukaan dark, motion, atau bentuk komponen.
- Migrasi string (`AnrealCopy`) — itu Spec C.
- Perbaikan semantics/a11y komponen (error field, selected state, touch target) — itu Spec A.
- Menyediakan font untuk layar iOS secara spesifik — compose resources berlaku lintas platform; iOS tetap TODO di level platform lain.

## 4. Desain

### 4.1 Tipografi (Geist)

**Aset** — `core/design-system/src/commonMain/composeResources/font/`:

| File | Berat | Dipakai untuk |
|---|---|---|
| `geist_regular.ttf` | 400 | Body, label normal |
| `geist_medium.ttf` | 500 | Title, label, emphasized |
| `geist_semibold.ttf` | 600 | Emphasized headlines/display |
| `geist_mono_regular.ttf` | 400 | Geist Mono (`AnrealFontFamily.Mono`) |

Sumber `vercel/geist-font` (SIL OFL 1.1); sertakan `OFL.txt` di folder yang sama dan URL sumber di komentar file tema. Ukuran total diharapkan ≤ ~1 MB.

**Kode** — `core/design-system/.../theme/AnrealTypography.kt`:

```kotlin
object AnrealFontFamily {
    @Composable fun Geist(): FontFamily   // Font(Res.font.geist_*, weight)
    @Composable fun Mono(): FontFamily    // Font(Res.font.geist_mono_regular)
}

@Composable fun anrealTypography(): Typography
```

- `anrealTypography()` membangun dari `Typography()` default M3 lalu `copy(fontFamily = …)` pada **seluruh field**, termasuk varian *emphasized* Expressive (`displayLargeEmphasized` … `labelSmallEmphasized`) supaya tidak hilang.
- `remember` di-key pada family yang dibangun agar tidak menyusun ulang tiap recomposition.
- Wiring di `AnrealTheme.kt`: `MaterialExpressiveTheme(colorScheme = …, motionScheme = …, typography = anrealTypography())`.

**Penerapan Geist Mono** (DESIGN.md §3 menyebut "tokens, timestamps, model ids"):
1. Editor skill (`SkillsSheets.kt:728` — sudah memakai `FontFamily.Monospace`, ganti ke `AnrealFontFamily.Mono()`).
2. Saat implementasi, cari rendering **model id / timestamp** di UI; terapkan mono di situ bila ada.
3. Jika ternyata hanya titik (1) yang ada, perbarui kalimat mono di DESIGN.md §3 agar mencerminkan pemakaian nyata (editor + komponen token) — tidak menambah pemakaian baru demi janji dokumen.

**Preview & screenshot:** seluruh `@AnrealPreviews` otomatis memakai typography baru (lewat `AnrealTheme`).

### 4.2 Kontrak palette (hanya dokumen)

**Kontrak:** `AnrealBrand.seedArgb = #E8A317` + `PaletteStyle.Expressive` adalah sumber kebenaran. Hex peran warna adalah **contoh hasil**, bukan konstanta yang dijanjikan.

**Perubahan dokumen:**

| Dokumen | Bagian | Perubahan |
|---|---|---|
| `DESIGN.md` | §1 | Ganti klaim "Dark is black-led (#050505)" → permukaan dark diturunkan dari seed oleh MaterialKolor Expressive (`surface` dark `#171308`), tetap gelap dan hangat; `#050505` hanya tint kaca bubble. |
| `DESIGN.md` | §2 | Tambah blok "Palette contract" (seed + style + tabel contoh light/dark dari probe); hapus larangan absolut `#050505` yang menyesatkan. |
| `DESIGN.md` | §9.4 | High-contrast: dari "rencana" → perilaku yang diimplementasikan (§4.3 spec ini). |
| `DESIGN.md` | §13 | Tambah baris implementasi `Typography` → `anrealTypography()`. |
| `design-resarch/README.md` | Tokens | Selaraskan token contoh ($bg/$surface/$primary) dengan hasil Expressive nyata; tambahkan catatan bahwa file `.pen` tetap otoritatif untuk layout, sedangkan seed + palette style adalah otoritatif untuk warna. |
| `AnrealTheme.kt` / `ThemeSettings.kt` | komentar | Tegaskan seed + style sebagai kontrak. |

Tidak ada perubahan nilai kode pada palette.

### 4.3 High contrast

**API baru** — `core/design-system/.../theme/HighContrast.kt`:

```kotlin
@Composable expect fun rememberHighContrast(): Boolean
```

- **Android actual:** API 35+ → `UiModeManager.contrast >= UiModeManager.CONTRAST_HIGH` (verifikasi nama konstanta saat implementasi); API < 35 → `Settings.Secure.HIGH_TEXT_CONTRAST_ENABLED == 1`; default `false`.
- **iOS actual:** stub `false` dengan komentar TODO.
- Nilai dibaca ulang saat `ON_RESUME` (pola `DisposableEffect` lifecycle) supaya perubahan setting tidak basi — memperbaiki kelas masalah yang sama dengan `rememberReduceMotion`.

**Wiring di `AnrealTheme.kt`:**

```kotlin
val highContrast = rememberHighContrast()
val contrastLevel = if (highContrast) 1f else 0f
platformDynamicColorScheme(darkTheme, contrastLevel)   // android actual meneruskan ke dynamicXColorScheme(context, contrastLevel)
rememberDynamicColorScheme(seed, dark, isAmoled = false, style = Expressive, contrastLevel = contrastLevel)
```

- Verifikasi dukungan `contrastLevel` pada MaterialKolor 5.0.0 saat implementasi; jika tidak tersedia, brand scheme memakai peningkatan hairline/container saja dan spec ini mengangkatnya sebagai keterbatasan yang diketahui.
- **Hairline & glass:** saat high-contrast aktif, helper border memakai `outline` (bukan `outlineVariant`), dan glass diperlakukan setara reduce-transparency: `GlassSurface` memakai fallback solid `surfaceContainer` tanpa scrim transparan. Diterapkan di helper design-system (`glassDrawerBorderColor`, `GlassSurface` default border, `glassChromeTargets`), sehingga semua pemakai ikut tanpa perubahan feature.

**Preview & test:** tambah preview/screenshot `HighContrast` (light & dark) untuk satu layar per modul (auth, chat, workspace).

### 4.4 Perubahan build & test (pendukung)

- `core:design-system` **mendapat test deps permanen**: `junit`, `robolectric`, `androidx.compose.ui.test.junit4`, `androidx.compose.ui.test.manifest` (diperlukan untuk menguji typography & high-contrast di level design-system). Entry sementara yang sekarang dipakai probe palette digantikan entry permanen ini.
- Test baru:
  - `AnrealTypographyTest` — seluruh 15 style (+ varian emphasized) memakai family Geist; `AnrealFontFamily.Mono()` tidak sama dengan Geist.
  - `HighContrastTest` — fungsi mapping murni (`highContrast → contrastLevel`, `highContrast → border role`) tanpa Android framework; actual Android diuji via Robolectric dengan `Settings.Secure.putInt`/UiModeManager shadow bila memungkinkan.
- Verifikasi visual: rekam ulang Roborazzi (`recordRoborazziAndroidHostTest` untuk auth, chat, workspace) lalu bandingkan PNG mana pun yang layout-nya bergeser karena metrik font.

## 5. File yang disentuh

| File | Aksi |
|---|---|
| `core/design-system/src/commonMain/composeResources/font/*` | **Baru** (4 TTF + OFL.txt) |
| `core/design-system/.../theme/AnrealTypography.kt` | **Baru** |
| `core/design-system/.../theme/HighContrast.kt` (+ android/ios actual) | **Baru** |
| `core/design-system/.../theme/AnrealTheme.kt` | Wiring typography + contrastLevel |
| `core/design-system/.../theme/PlatformDynamicColorScheme.kt` (android) | Terima `contrastLevel` |
| `core/design-system/.../theme/GlassDrawer.kt`, `GlassSurface.kt` | Border/fallback high-contrast |
| `core/design-system/build.gradle.kts` | Test deps permanen |
| `feature/chat/.../SkillsSheets.kt` | Mono → `AnrealFontFamily.Mono()` |
| `DESIGN.md`, `design-resarch/README.md`, komentar tema | Dokumen selaras |
| `core/design-system/src/androidHostTest/...` | 2 test baru |

## 6. Risiko & mitigasi

| Risiko | Mitigasi |
|---|---|
| Metrik Geist menggeser layout (tab, tombol, baris) | Rekam ulang seluruh screenshot; review PNG yang berubah; perbaiki layout bila ada yang pecah |
| `contrastLevel` tidak ada di MaterialKolor 5.0.0 | Verifikasi API saat mulai; fallback hairline/container didokumentasikan |
| Ukuran APK bertambah (~0.5–1 MB) | Hanya 4 file static; tidak ada duplikat family |
| `UiModeManager.CONTRAST_HIGH` berbeda dari asumsi | Verifikasi konstanta saat implementasi; fallback `Settings.Secure` tetap ada |
| Font gagal dimuat (asset rusak) | Compose jatuh ke system font secara aman; test typography akan gagal lebih dulu |

## 7. Kriteria penerimaan

- [ ] `MaterialTheme.typography.bodyLarge.fontFamily` = Geist di runtime dan di preview/Roborazzi.
- [ ] Varian `*Emphasized` tidak hilang (masih ada di Typography baru).
- [ ] Geist Mono tersedia dan dipakai di titik yang disepakati §4.1.
- [ ] Setting high-contrast Android menaikkan `contrastLevel` + border `outline` + fallback glass solid; iOS mengembalikan `false` (stub).
- [ ] Tidak ada regresi layout yang tidak disengaja pada screenshot; semua screenshot yang berubah direview.
- [ ] `DESIGN.md` tidak lagi memuat klaim yang bertentangan dengan hasil nyata; `design-resarch/README.md` diselaraskan.
- [ ] `.\gradlew.bat :core:design-system:testAndroidHostTest` lulus; build Android + compile iOS commonMain lulus; warnings-as-errors tetap hijau.

## 8. Catatan implementasi

- Urutan disarankan: (1) aset + typography + wiring, (2) rekam ulang screenshot + review metrik, (3) high contrast, (4) dokumen, (5) test.
- Probe palette sementara (`PaletteProbeTest` + build deps temp) dihapus saat spec ini dieksekusi; nilainya sudah diarsipkan di spec A/B dan companion.
- Setelah spec disetujui → `writing-plans` menyusun rencana implementasi berurutan beserta perintah verifikasinya.

# Design System: Anreal

Anreal is the native client of DocChat — a document-grounded AI workspace. This file is the visual, motion, state, and accessibility contract. Feature UI follows it. If a screen cannot be described with these tokens, the screen is wrong, not the tokens.

**Stack:** Material 3 Expressive + semantic color + Haze glass + Geist.  
**Personality:** Calm, precise, slightly warm. The product should feel like a well-lit studio at night (dark) or a quiet paper desk (light) — never a neon dashboard, never a marketing landing page.

---

## 1. Visual theme and atmosphere

- **Density:** Daily-app balanced (5/10). Chat is the hero; chrome recedes.
- **Variance:** Predictable structure, one signature: aurora + frost.
- **Motion:** Subtle and physical (4/10). Feedback and spatial continuity, not spectacle.
- **Signature:** Warm aurora behind frosted M3 chrome. Amber is a *seed*, never a raw fill in feature code.

Dark is black-led (`#050505` family), not charcoal-grey panels. Light is paper-led, not pure white. Glass lets the aurora (or the wallpaper-derived surface) read through chrome.

Do not clone the desktop L-frame on a phone. Compact uses a modal drawer + top bar + floating composer — the same information architecture as the web's `<768px` layout, expressed with M3 components.

---

## 2. Color — semantic only

Feature code uses **only** `MaterialTheme.colorScheme.*`. Hex values live in theme generation, never in screens.

### Roles

| Role | Use |
|---|---|
| `primary` / `onPrimary` | Highest-emphasis actions (send, continue, allow) |
| `primaryContainer` / `onPrimaryContainer` | Selected session, emphasized chips |
| `secondary` / `onSecondary` | Supporting actions, filter chips |
| `tertiary` / `onTertiary` | Rare accent balance — not a second brand |
| `error` / `onError` / `errorContainer` | Errors, destructive confirm |
| `surface` / `onSurface` | App canvas, body text |
| `surfaceContainer*` | Cards, sheets, fields, drawers |
| `surfaceVariant` / `onSurfaceVariant` | Secondary text, quiet icons, helper |
| `outline` / `outlineVariant` | Hairlines, field borders |
| `inverseSurface` / `inverseOnSurface` | Toasts / snackbars |

**Contrast rule:** `onX` sits on `X`. `onXContainer` sits on `XContainer`. Mixing containers is a contrast bug.

### Light, dark, dynamic

1. Follow system theme by default (`isSystemInDarkTheme()`).
2. Settings: System / Light / Dark.
3. **Dynamic color on by default on Android 12+** via `dynamicLightColorScheme` / `dynamicDarkColorScheme`.
4. Fallback and “brand” mode: MaterialKolor `DynamicMaterialExpressiveTheme` with seed `#E8A317` (web accent), `SPEC_2025`, `PaletteStyle.Expressive`.
5. Theme changes ease over ~200ms (color only). No full-screen flash.

Glass tints sample `surface` / `surfaceContainer` at low alpha so frost works in light, dark, *and* wallpaper palettes. Never hardcode `rgb(5 5 5 / 0.82)` in mobile chrome.

**Banned:** raw `#E8A317` in a composable, `#000000` fills, neon/outer glows, purple-blue “AI” gradients, hardcoded grey text.

---

## 3. Typography

Face: **Geist** (UI) + **Geist Mono** (tokens, timestamps, model ids). SIL OFL, same as web.

Map onto the M3 type scale. Do not invent 13.sp / 17.sp one-offs. Use **emphasized** styles for auth titles and empty-state headlines.

| Role | Size / line | Tracking | Use |
|---|---|---|---|
| headlineSmall | 24 / 32 | tight | Empty-state title (emphasized) |
| titleLarge | 22 / 28 | tight | Auth title, settings section |
| titleMedium | 16 / 24 | 0.15 | Session title, dialog title |
| titleSmall | 14 / 20 | 0.1 | List primary when dense |
| bodyLarge | 16 / 24 | 0.15 | Message body, form values |
| bodyMedium | 14 / 20 | 0.25 | Supporting copy |
| bodySmall | 12 / 16 | 0.4 | Timestamps, helper |
| labelLarge | 14 / 20 | 0.1 | Buttons |
| labelMedium | 12 / 16 | 0.5 | Chips, badges |
| labelSmall | 11 / 16 | 0.5 | Field labels (uppercase + 0.08em only for auth field captions) |

Always `sp`. Layout must survive font scale 1.3 and 2.0. Body reading width ~40–60ch. Large type tightens tracking; small type loosens it.

**Banned:** Inter, system-serif for UI, all-caps paragraphs, gradient text.

---

## 4. Spacing, shape, touch

**Grid:** 4.dp. Scale: 4, 8, 12, 16, 24, 32, 40, 48.

- Compact horizontal inset: 16.dp
- Medium+: 24.dp
- Minimum touch target: **48.dp** (icon-only controls get a 48.dp hit box even if the glyph is 24.dp)
- Single-line text fields: **56.dp** (`AnrealSpacing.field`). Password uses a 48.dp trailing `IconButton` (visibility), not a TextButton — the field height must match Name / Email.
- Icon glyph: 24.dp, tint `LocalContentColor`

**Shape:** M3 scale (`extraSmall` 4 → `extraLarge` 28). Composer and fields use `large` / `extraLarge`. Sheets use the platform sheet shape. Pills/chips use full rounding.

---

## 5. Components — Material 3 Expressive

Do not rebuild buttons, fields, dialogs, lists, or navigation. Theme them.

| Job | Component |
|---|---|
| Primary action | `AnrealPrimaryButton` (glass) — not a filled M3 `Button` |
| Secondary | `FilledTonalButton` |
| Tertiary / cancel | `TextButton` |
| Icon-only | `IconButton` / `FilledTonalIconButton` |
| Auth fields | `AnrealTextField` / `AnrealPasswordField` (`BasicTextField` in `GlassSurface`, never `OutlinedTextField`) |
| Send + extras | `SplitButtonLayout` |
| Model / reasoning / feature cluster | `ButtonGroup` |
| Loading (indeterminate wait) | `LoadingIndicator` — not a circular spinner |
| Progress with known work | `LinearWavyProgressIndicator` / linear determinate |
| App bar | `TopAppBar` + Haze |
| Sessions | `ModalNavigationDrawer` (compact), `NavigationRail` (medium), permanent drawer (expanded) |
| Sheets | `ModalBottomSheet` |
| Lists | Expressive `ListItem` |
| Search | `SearchBar` |
| Confirm | M3 dialog, not a custom card unless glass is required |
| Snackbar | M3 `Snackbar` on `inverseSurface` |

**Icons:** Material Symbols **Rounded** only (`com.composables:icons-material-symbols-rounded-cmp`). Filled rounded only for selected/active. Do not depend on `material-icons-extended`.

Pressable things scale to **0.97** on press (see Motion). Hover styles are pointer-fine only.

### 5.1 Chat UI is app-owned

We do **not** port `@anvia/react-ui`. On web that package is headless (structure + `data-anvia-*`) and is patched so the composer stays editable while streaming. Mobile already owns the composer, so that lock must never ship.

- Thread, composer, and later tool/approval cards are **slot composables** in `feature:chat:presentation` (typed params, M3 + Anreal tokens).
- Do not add a separate unstyled chat-ui module until a second feature needs the same primitives.
- Composer field stays enabled during a run. Stop replaces Send. Queue uses the same field.
- Model + reasoning is **one** composer trigger and **one** sheet. Concatenate the effort onto the model label when it is not None. The add control and model trigger are matching tonal capsules; the model pill wraps its label and ellipsizes between add and send.
- Persist the last valid model + reasoning choice in app preferences. Revalidate both against the live catalog and clear a removed model or unsupported effort.
- The model sheet always exposes catalog loading, empty, and retryable error states; its trigger and option content are left-aligned.
- Context usage is a compact circular indicator immediately before Documents in the top bar. Its detail (model, tokens, ratio, thresholds, and reasoning) lives in a bottom sheet, never in the composer.
- Attachments are entered from the composer `+` feature sheet; do not spend a second composer action slot on a standalone attachment icon.

---

## 6. Glassmorphism (Haze — the only correct path)

Compose has no CSS `backdrop-filter`. These are **wrong**:

| Approach | Why it fails |
|---|---|
| `Modifier.blur()` / `BlurEffect` on the chrome | Blurs the chrome's *own* pixels |
| Tint-only overlay | Plastic, not glass (fallback only) |
| Home-grown RenderNode snapshot | Leaks, scroll bugs, OOM |
| Persistent `transform` on a glass ancestor | Kills backdrop sampling (web already hit this) |

**Correct:** [Haze](https://chrisbanes.github.io/haze/) — `hazeSource` on aurora + thread, `hazeBlur` on chrome. Feature modules never import Haze. They use:

- `GlassSurface` — chips, panes, user bubbles, auth fields, primary buttons
- `GlassTopBar`
- `GlassBottomBar` / composer shell
- `GlassSheet` / `GlassDialog`
- `GlassDrawer` — left workspace (start radii), right documents (end radii, `fromEnd = true`)

`AnrealAtmosphere` owns aurora + the Haze source. The root `NavHost` sits in **one** atmosphere. Nested calls are a passthrough so transitions do not remount aurora. The aurora is the single haze source; chrome and bubbles apply the thin Haze material directly over it. Do **not** wrap scrollable content in a nested `hazeSource` — a glass element inside a nested source samples the nested source (itself excluded) instead of the aurora, so its frost silently disappears. Top bars use the same thin Haze material, tint composition, hairline, and fallback color as the left drawer so app chrome reads as one glass layer. Dark drawer frost is black-led (`canvas` `#050505` family at low alpha), not a muddy `surface` tint. Selected tiles use `glassHighlightColor()`, muted drawer text uses `glassMutedTextColor()` / `glassFaintTextColor()` — do not rely on `onSurfaceVariant` alone in dark previews.

**Scroll-aware top chrome:** `GlassTopBar` is clear over the status bar and app bar until the scrolling surface *under it* has left the start (`rememberFrostedTopBar`). Frost then fades in at `durationFast` / `easeOut` via Haze `alpha` (not `graphicsLayer` on the glass ancestor). Reduced motion snaps. Account / Settings and Workspace keep `frosted = false` because only the pane below the segmented tabs scrolls — the top bar never has content sliding under it.

Recipes:

| Chrome | Material | Notes |
|---|---|---|
| Top bar, composer | Thin frost | Highest blur; hairline `outlineVariant` ~12% |
| Drawer, sheets | Regular | Heavier so content behind doesn't compete |
| Chips, selected row | Ultra-thin / pane | No extra shadow |
| User bubble | Thin frost (matches top bar chrome) | Same hairline (`glassDrawerBorderColor`) and tint/fallback (`glassDrawerFallbackColor`) as `GlassTopBar`, plus `glassBubbleTintColor()` scrim so the panel reads on a dim aurora (thin haze alone is invisible there) |

Fallback when Haze disables blur (low-end / reliability gate): opaque-enough `surfaceContainer` scrim, same shape, no fake blur.

Aurora is the haze **source**. Animate it with independent orb wander (position + opacity) on a 16–32s cycle — slow enough to stay atmospheric, large enough to read through frost. **Disable it** under reduced motion and reduced transparency.

Never stack a light translucent surface on another light translucent surface.

---

## 7. Motion

Motion is a product feature. It is also a footgun. Every animation must answer: **should it animate, why, which properties, which curve, how it interrupts, how reduced motion treats it.**

### 7.1 Gate — should this animate?

| Frequency | Decision |
|---|---|
| 100+/day (send while typing, token stream, list scroll) | **No enter/exit animation.** Instant. Streaming text must never fade per token. |
| Tens/day (row select, tab, icon toggle) | Near-imperceptible: color/opacity ≤ 160ms, or nothing |
| Occasional (drawer, sheet, dialog, snackbar) | Standard motion |
| Rare (first empty state, theme switch, success after a long wait) | Small delight budget |

Keyboard / IME send is a disqualifier for decorative motion.

**Valid purposes only:** feedback, spatial consistency, state indication, preventing a jarring change. “It looks cool” is not a purpose on a daily surface.

### 7.2 Tokens

Named after the web, implemented as Compose specs. Do not invent a second system.

| Token | Compose | Use |
|---|---|---|
| `easeOut` | `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` | Enter, exit, press release |
| `easeInOut` | `CubicBezierEasing(0.77f, 0f, 0.175f, 1f)` | On-screen move / morph |
| `easeDrawer` | `CubicBezierEasing(0.32f, 0.72f, 0f, 1f)` | Drawer / sheet |
| `durationFast` | 160ms | Press, color, chips |
| `durationMed` | 220ms | Popovers, dialogs, snackbars |
| `durationDrawer` | 280–320ms | Navigation drawer, sheets |
| `durationPage` | 420ms | Full-screen vertical pager only (boarding → login/register, login ↔ register). Occasional; allowed above the 300ms daily-chrome cap. |
| `durationSplash` | 1100ms | First-run compose splash hold after session resolve. Reduced motion uses `durationFast`. |
| `durationBoardingHold` | 4500ms | Auto-advance on the signed-out boarding carousel. Pause while the email field is focused. |
| `pageSpec` | 420ms `easeDrawer` | Boarding → Login/Register and Login ↔ Register: both pages start immediately and settle. Do not use the punchy `easeInOut` here — it hesitates then rushes. |
| `drawerSpec` | 280ms `easeDrawer` | Horizontal push (auth ↔ chat, chat ↔ account), drawers |

M3 components use `MotionScheme.standard()` as the **app default**. Expressive bounce is reserved for rare hero moments (empty-state mark, first-run). Daily chrome must not overshoot.

**UI motion stays ≤ 300ms** unless it is a gesture spring (sheet dismiss) whose settle time is physics, not a timer.

### 7.3 Properties

Animate **`transform` and `opacity` only**, via `graphicsLayer` / `offset { }` / `animateFloatAsState` applied in draw — never `Modifier.alpha` / `Modifier.scale` on every frame (recomposition).

- Never `scale(0)`. Enter from **scale 0.96 + alpha 0**.
- Never animate `width` / `height` / `padding` / `top` / `left`. Accordion-style collapse is the only height exception, and it must stay ≤ 200ms.
- Popovers originate at the trigger (`transformOrigin` = tap). Dialogs stay centered.
- Exit the way it entered.
- Transitions, not one-shot keyframes, for anything a user can fire twice in a second (toasts, toggles).
- Stagger 30–50ms, max ~6 items, never blocks input.
- Press: `scale(0.97)` for 100–160ms on pointer-down, not on release.

### 7.4 Recipe map (Anreal)

| Surface | Motion | Reduced motion |
|---|---|---|
| Button / icon / send | Press scale 0.97, 120ms easeOut | Keep scale *or* color flash only |
| Drawer | Slide from start + fade 280ms easeDrawer | Fade 160ms, no slide |
| Splash → boarding / chat | System splash hands off to compose splash (aurora + mark + version). Compose splash is **not** a nav destination. After session resolve, hold `durationSplash` then fade 160ms onto the start route. Aurora is already mounted. | Fade 160ms; hold `durationFast` |
| Boarding carousel | Horizontal pager. Auto-advance after `durationBoardingHold` (`easeInOut` 220ms). User swipe is the same pager. Pause on email focus and reduced motion. Indicator pill uses `offset {}`, not width animation. | Instant page change, no auto-advance |
| Boarding → Login / Register | **One-way vertical pager.** Boarding is replaced by the selected form; both move **up**. Forms have no back affordance and system back cannot reveal boarding. 420ms `easeDrawer` (no fade). Hide IME before the navigate. | Fade 160ms, no slide |
| Login ↔ Register | Same vertical pager with replace-current navigation. Login → Register: both move **up**. Register → Login: both move **down**. Neither form accumulates history. | Fade 160ms, no slide |
| Auth ↔ Chat / Chat ↔ Account | Horizontal push, full width, no fade. Logout returns to **boarding** (the reverse). | Fade 160ms |
| Sheet | Slide from bottom, interruptible, velocity handoff | Fade 160ms |
| Dialog | Scale 0.96 + fade 220ms, centered | Fade only |
| Snackbar | Slide from bottom 220ms | Fade |
| Popover / menu | Scale 0.96 from trigger 180ms | Fade |
| Tooltip | 125ms; skip delay after the first in a cluster | Instant show, no move |
| List first paint | Optional 40ms stagger × 6, 8.dp rise | Instant |
| Streaming tokens | **None** | — |
| Skeleton → content | Crossfade 160ms; optional 2.dp blur seam | Instant swap |
| Theme switch | Color 200ms | Instant |
| Aurora | Independent orb wander, 16–32s | Hidden |
| Success check | 200ms scale 0.96→1 once | Static icon |
| Top bar frost | Opacity 160ms `easeOut` when the list under the bar can scroll backward | Snap |

Sheets and drawers that the user can drag use **springs** (critically damped). Hand off release velocity. Rubber-band past the edge. Never lock input during the transition.

### 7.5 Compose implementation rules

- Prefer M3 component motion (it already reads `MotionScheme`).
- Custom motion lives in `:core:design-system` (`AnrealMotion`) and root nav helpers (`anrealEnter` / `anrealExit` in `shared`).
- Features do not pick random `tween(400)`.
- `LocalAnrealReduceMotion` (Android: animator duration scale = 0) is provided by `AnrealTheme`. Reduced motion replaces slides with a 160ms fade.
- Full-screen page swipes use `slideIntoContainer` / `slideOutOfContainer` so the distance is the **container**, not wrap-content form height. `sizeTransform` is a clip-only snap — never a size fade.

---

## 8. UX states — every screen, every list, every action

UX is the product. A screen that only implements the happy path is unfinished. Every user-facing surface declares these states in its `State` and has a preview for each.

### 8.1 Canonical states

| State | What the user sees | Rules |
|---|---|---|
| **Idle / populated** | Real content | Default. Realistic previews. |
| **Loading (first)** | Skeleton that **matches the layout**, not a centered spinner | Shimmer on `surfaceContainer`. Announce “Loading” politely once. |
| **Loading (refresh)** | Content stays; quiet indicator at the edge | Do not replace the whole screen. |
| **Loading (action)** | Control shows a trailing `LoadingIndicator`, disables itself | Button label stays (“Signing in…”) so the verb doesn’t vanish. |
| **Empty** | Icon + title + one sentence + **one** primary action | Invitation, not apology. “Ask anything about your documents.” |
| **Error (page)** | What failed + how to fix + Retry | Inline, not a blank screen. `error` / `onError` only for the action, not the whole canvas. |
| **Error (inline)** | Field or row message | Under the field, `bodySmall` + `error`. Set `isError` on the field. |
| **Error (toast)** | Transient, non-blocking | After a mutation the user can retry later. |
| **Success** | Content updates in place | Toast only if the result is off-screen (“Chat deleted”). No confetti. |
| **Partial** | Show what we have + a banner for the rest | e.g. history loaded, stream failed. |
| **Offline / no network** | Banner + last known content if any | Map `DataError.Network.NO_INTERNET`. |
| **Unauthorized** | Navigate to login; do not loop a toast | 401 interceptor. |
| **Disabled** | 38% opacity, still focusable if it explains why | Tooltip / helper: “Send a message first”. |
| **In progress (long)** | Determinate if we can (upload, OCR) | Status text from the domain (“Extracting text…”). |
| **Confirm destructive** | Dialog: what is lost, confirm verb, cancel | “Delete chat” not “OK”. |
| **Permission / approval** | Allow once / Allow for session / Reject | Same copy as web. |
| **Conflict / stale** | Dialog with a resume path | 409 `RUN_ACTIVE`, stale session. |
| **Streaming** | Tokens appear; stop is always available | Composer **stays editable**. Send becomes Stop. Do not lock the field. |

### 8.2 Per-surface checklist

A PR that adds a screen must include previews or robots for: **populated, loading, empty, error**. Add success/confirm when the screen mutates.

| Surface | Empty | Loading | Error | Success |
|---|---|---|---|---|
| Splash | — | Aurora + mark + version | — | Fade to boarding / chat |
| Boarding | First slide | Auto-advance / swipe | Inline email on submit | Navigate to register |
| Login / register | — | Busy on submit | Inline field + form alert | Navigate |
| Session list | “No chats yet” + New chat | Skeleton rows | Retry banner | Rename/delete in place |
| Thread | Product empty state | History skeleton | Inline + retry load | Stream completes in place |
| Composer | Placeholder | Send shows indicator | Attachment reject chips | Queue item appears |
| Documents | “Upload a PDF or image” | Library skeletons + upload progress | Quota / failed ingest | Status → ready |
| Projects | “Create a project” | Skeletons | Retry | Open project |
| Gallery | “No images yet” | Grid skeletons | Retry | New image at start |
| Settings / account | Profile summary + name + email + fixed glass extended-FAB Log out dock | Health check | Retry API status | Persist appearance locally; sign out → boarding |
| Settings / usage | Zero-state usage | Section shimmer | Retry section with server-safe message | Storage and request/token breakdowns |
| Settings / personalization | “No profile yet…” | Section shimmer | Retry section | Reset profile confirm |
| Workspace / projects | “Create a project” | Project skeletons | Retry section | Create, edit, open, and delete projects |
| Workspace / documents | “Attach one from a chat” | Document skeletons | Retry section | Preview pages/images and delete |
| Workspace / images | “Generate or attach an image” | Image skeletons | Retry section | Authenticated image bytes and metadata |
| Approval / clarification | — | Card while pending | Late response is idempotent | Card dismisses |

### 8.3 Copy for states

- Sentence case. Active voice. Name the thing the user controls.
- Errors: what happened + what to do. No “Oops”. No apology theatre.
- Empty: invitation to act, one primary CTA.
- Buttons keep the same verb through the flow (“Delete” → toast “Chat deleted”).
- Loading labels keep the verb (“Signing in…”, “Creating account…”, “Sending…”).
- All strings are resources (`UiText`).

### 8.4 Perceived performance

- First paint: show chrome immediately. For chat history, render Room cache first and refresh the network in the background; only show the compact loading indicator when no cache exists.
- Optimistic user bubble on send; reconcile with history.
- Fast `LoadingIndicator` (M3) — a quicker indicator feels like a faster app.
- Never block the composer while a run is streaming (queue instead).

---

## 9. Accessibility

Accessibility ships with the component, not as a follow-up. Target **WCAG 2.2 AA** as the floor. M3 roles already meet contrast when used correctly — custom colors must be re-checked.

### 9.1 TalkBack / screen readers

- Every interactive or informative icon has a **string-resource** `contentDescription` (`cd_*`).
- Decorative aurora, grain, and brand glow: `contentDescription = null` and `hideFromAccessibility()`.
- Icon-only buttons: description is the action (“New chat”, “Open menu”), not the glyph name.
- Images: describe content (“Generated image of …”) or “Document page 2”.
- `semantics { liveRegion = Polite }` on status lines (ingest, “Signing in”, stream errors). Assertive only for destructive failures the user must hear now.
- Group session rows so TalkBack reads title + time + unread as one focusable.
- Don't rely on color alone for unread, error, or selected — pair with icon, weight, or text.
- Custom controls expose `role` (Button, Switch, Tab).

### 9.2 Focus and keyboard

- Visible focus indicator: `accent` ring, 2–3.dp, never removed.
- Tab / D-pad / keyboard order is reading order. Drawer, then bar, then thread, then composer.
- Dialogs and sheets trap focus and return it to the trigger on dismiss (`restoreFocus`).
- IME: the composer is multi-line; Enter inserts a newline and the visible Send / Queue button submits. Auth fields are single-line with Next between fields and Done on the final submit field. Single-field mutations such as rename use Done; errors are announced after submit.
- Auth IME: activity uses `adjustNothing`. Do not `imePadding()` a vertically centered form — that shrinks the viewport and recenters, leaving a hole above the fields. Measure the focused field vs the keyboard top and **translate the form block** (`rememberImeFocusShift`) by only the overlap. Fields stay packed. Password visibility is an icon with `Show password` / `Hide password` descriptions.

### 9.3 Touch and motor

- 48.dp minimum target; 8.dp gap between adjacent targets.
- Press feedback on pointer-down.
- Swipe-to-delete is never the only path — overflow menu always exists.
- Drag-to-dismiss sheets also have a close affordance.

### 9.4 Vision and motion preferences

| Signal | Behavior |
|---|---|
| Font scale | `sp` + flexible layouts; no clipped labels at 200% |
| `prefers-reduced-motion` / `isReduceMotionEnabled` | Crossfade instead of slide/scale; hide aurora; keep color/opacity |
| Reduce transparency | Glass becomes solid `surfaceContainer`; no Haze |
| High contrast / `isHighContrast` | Hairlines become `outline`; raise container contrast |
| Color inversion | Use theme colors, not bitmaps with baked greys |

### 9.5 Forms

- Label above the field (not placeholder-as-label). Placeholder is an example.
- `isError` + supporting text linked for TalkBack.
- Password: show/hide control labeled “Show password” / “Hide password”.
- Disable submit only while a request is in flight, not because the form is incomplete — show field errors on submit instead (matches web).

### 9.6 Dynamic color and contrast

After applying a dynamic scheme, do not overlay hardcoded amber. If a custom color is required (e.g. citation chip), **harmonize** it with `primary` via MaterialKolor.

### 9.7 Preview matrix (required)

Every Screen preview file includes at minimum:

- Light / Dark
- Populated / Loading / Empty / Error
- FontScale 1.5f (at least one screen per feature)

---

## 10. Copywriting

- Sentence case. No emoji. No “Elevate”, “Seamless”, “Unleash”.
- Specific names: “New chat”, “Documents”, “Projects”, “Allow once”.
- Reuse web strings when they already work.
- Auth: “Sign in”, “Create an account”, “Continue”, “Signing in…”.
- Destructive: say what is destroyed (“Delete chat and its messages”).

---

## 11. Layout

- Compact: left **workspace** `ModalNavigationDrawer` (All chats / Projects / Documents / Images, recent projects, date-grouped sessions, account footer) + `TopAppBar` (context ring, documents icon + badge) + right **session documents** drawer + thread + floating glass composer above IME + nav bar insets. Thread content draws behind both chrome surfaces; list content padding keeps the first/last bubble reachable. The top bar (status + app bar) is clear until the thread can scroll backward, then the thin frost fades in. Opening a project is a Chat **scope**, not a new destination: the session list filters to that project, the matching recent-project row is selected, the drawer auto-opens, and the top bar reads `{project} · {chat}`. All chats (or system back with the drawer closed) leaves the project and restores the last standalone session. Cold start stays a standalone New chat draft.

- Account / Settings is a **full screen** (not a web-style modal): Account, Usage, Personalization. Use the same reusable fixed glass segmented tabs as Workspace, grouped settings surfaces, and 160ms directional fade/8.dp translation between sections. Open it from the left-drawer account row. Log out is a glass extended FAB fixed in a floating bottom dock across this screen.
- Projects / Documents / Images open the type-safe **Workspace** destination from the left drawer. Compact uses the shared fixed glass segmented tabs and section-local loading, empty, error, and populated states; uploads stay session-scoped in the chat composer because the backend requires a session id.
- Thread opens at the latest message. It follows appended stream content only while the user is at the bottom; scrolling up suspends follow and reveals a smooth scroll-to-latest control. Sending a message snaps the thread to the bottom and resumes follow even if the user had scrolled up, and the composer dismisses the keyboard on send. Consecutive collapsible items (thought/tool) stay flush — no gap — even across message boundaries; content (text) keeps the 16.dp rhythm.
- Medium: `NavigationRail`.
- Expanded: permanent drawer; optional documents pane. Do not force a 3-column phone layout.
- Content never sits under system bars. Use `WindowInsets` (status, nav, ime, cutout).
- System bars are fully transparent. Icon and caption colors follow the **app** light/dark theme (`SystemBarStyle.light` / `dark`), not the OS night mode. Navigation-bar contrast enforcement is off so 3-button nav has no scrim.
- Thread: user bubbles end-aligned, assistant start-aligned, 8.dp vertical rhythm, 16.dp inset.

---

## 12. Anti-patterns (never)

- `Modifier.blur` / self `BlurEffect` for glass
- Hardcoded `#E8A317`, `#000000`, or web `rgb(5 5 5 / 0.82)` in feature UI
- `material-icons-extended` / `Icons.Default` as the long-term set
- `transition: all`, `scale(0)`, `ease-in` on UI, durations > 300ms on daily chrome
- Animating layout properties or streaming tokens
- Centered circular spinner as a page loader
- Empty “No data” without a next action
- Error that only logs
- Missing loading/empty/error preview
- Hover motion on touch
- Motion without a reduced-motion path
- Interactive icon without `contentDescription`
- Touch target < 48.dp
- Custom `CompositionLocal` for theme (use `MaterialTheme`)
- Confetti, bounce on every tap, looping aurora under reduced motion

---

## 13. Implementation map

| Token | Code |
|---|---|
| Theme | `AnrealTheme` → `MaterialExpressiveTheme` + dynamic/brand scheme |
| Motion | `AnrealMotion` + `MotionScheme.standard()` + `anrealEnter` / `anrealExit` |
| Auth IME | `adjustNothing` + `rememberImeFocusShift` — never centered `imePadding()` |
| Glass | Haze wrappers in `:core:design-system` |
| Space | `AnrealSpacing` |
| States | `AnrealEmpty`, `AnrealError`, `AnrealSkeleton`, `AnrealBanner` |
| A11y | string resources + semantics helpers |

When in doubt: quieter, faster, more explicit.

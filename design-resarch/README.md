# Design Research — Account & Workspace Refactor

Generated: 2026-08-24
Branch: codex/complete-api-integration
Device reference: Infinix X6853 (1080x2436) — screenshots at C\\temp\\anreal*

## Files
- ccount/account-settings-refactor-v1.pen — 3 phones: Menu (level 1) → Appearance → Usage. Drill-down pattern, danger zone, 48dp targets, Geist + M3 Expressive + aurora/frost.
- workspace/workspace-refactor-v1.pen — 3 phones: Projects list (search + chips + 48dp overflow → sheet) → Images 2-col grid + Docs list → Bottom sheet detail. Grid/List toggle, inset skeletons.
- combined/anreal-account-workspace-v1.pen — both covers + 6 phones in one canvas (y 24 and y 1350).

## Rationale (from web research + device)
- Account: from ccount-settings:363 Dribbble + setting.page + Android Settings Guidelines — limit 10-15 items/screen, grouped cards, profile hero, destructive isolated, no FAB over content.
- Workspace: from file-manager patterns (shadcn crud-file-manager, nixopus #349, Dribbble file manager) — grid/list toggle, 48dp overflow, bottom sheet, filter chips.

## How to open
Open any .pen in Pencil desktop (Pen.exe). The combined file shows the full flow; account/workspace files are standalone.

## Next steps
Pick Account A + Workspace B (recommended in earlier proposal) and implement in eature:chat:presentation / eature:workspace:presentation with AnrealMotion, GlassSurface, ChatBottomSheet.

Preview images in .pencil/previews/ are auto-generated.

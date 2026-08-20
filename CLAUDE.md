# Volunteer Hours Tracker — Project Context

This file gives the coding agent the standing rules for this repo. Read it before any task. Also read `Volunteer-Hours-PRD.html` (the spec) and `Volunteer-Hours-Screens.html` (the screen mockups) in the repo root — they are the source of truth for scope and visual design.

## What we're building
A single-user Android app for logging volunteer hours and reporting totals by period. Local-only storage, no accounts, no network. Replaces a spreadsheet-and-memory workflow. MVP scope is locked — build what the PRD says and no more.

## Locked technical decisions
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Repository. ViewModels expose UI state via `StateFlow`. Screens are stateless composables driven by state. Keep it as light as the app allows.
- **Dependency injection:** Manual. An `Application` subclass holds an `AppContainer` (database + repository); ViewModels are created with a small `ViewModelProvider.Factory`. **Do not add Hilt/Dagger/Koin.**
- **Persistence:** Room (SQLite). This is the single source of truth. No cloud, no DataStore-backed sync.
- **Dates/time:** `java.time` (`LocalDate`, etc.). `minSdk 26` makes this available without desugaring.
- **Navigation:** `androidx.navigation:navigation-compose` with a bottom navigation bar.
- **SDK:** `minSdk 26`, `compileSdk 35`, `targetSdk 35`. Single app module.
- **App id:** `com.dean.hourstracker`

## Non-negotiable constraints
- No internet permission. No analytics. No third-party SDKs beyond AndroidX/Compose/Room. The only way data leaves the device is the user-initiated CSV share (Phase 6).
- Every phase must **compile and run**. Never leave the app in a non-building state at a phase boundary.
- Don't build ahead. Implement only the current phase's scope; stub what later phases need with the minimum that compiles.

## Data model (Room)

**`Project`**
| field | type | notes |
|---|---|---|
| `id` | Long, PK, autoGenerate | |
| `name` | String | |
| `isPreset` | Boolean | seeded presets = true |
| `isArchived` | Boolean = false | archived projects leave the picker but stay on old entries |
| `createdAt` | Long | epoch millis; also used for stable ordering |

**`Entry`**
| field | type | notes |
|---|---|---|
| `id` | Long, PK, autoGenerate | |
| `projectId` | Long, indexed | references `Project.id` |
| `dateEpochDay` | Long | `LocalDate.toEpochDay()` — enables fast range queries |
| `hours` | Double | quarter-hour increments (0.25); always > 0 |
| `note` | String = "" | freeform |
| `createdAt` | Long | epoch millis; used to resolve "last-used project" and tie-break ordering |

- Display uses a relation POJO `EntryWithProject` (entry fields + project name).
- **Last-used project** is derived, not separately stored: `SELECT projectId FROM entries ORDER BY createdAt DESC LIMIT 1`. On first run (no entries), default to the first active project.
- **Seed presets** on database creation via a `RoomDatabase.Callback`: `Community Service`, `Food Bank`, `Animal Shelter`, `Beach Cleanup`, `Library` (all `isPreset = true`). These are examples — the user can rename/archive/add.

## Reporting periods (calendar-based)
Compute `[startEpochDay, endEpochDay]` with `java.time`, then query the range:
- **Week:** Monday–Sunday of the current week (`now.with(DayOfWeek.MONDAY)` … `+6 days`).
- **Month:** 1st … last day of current month.
- **Quarter:** current calendar quarter (Jan–Mar, Apr–Jun, Jul–Sep, Oct–Dec).
- **Calendar Year:** Jan 1 … Dec 31 of current year.

## Sage Design System — Compose tokens
One accent, used sparingly. White space is structure. Hierarchy over decoration.

```kotlin
// Color.kt
val Linen      = Color(0xFFF7F8F7) // page background
val White      = Color(0xFFFFFFFF) // cards / surfaces
val Mist       = Color(0xFFEBF7F5) // accent fills / highlights
val Sage       = Color(0xFF7EC8BB) // primary accent · interactive
val Teal       = Color(0xFF5AB5A6) // mid accent · hover/pressed
val DeepTeal   = Color(0xFF3A9085) // primary action · text on accent · emphasis
val Slate      = Color(0xFF6B7C79) // secondary/body text
val TextFaint  = Color(0xFFA8B8B5) // tertiary text / disabled
val Ink        = Color(0xFF1C2422) // primary text / headings
val BorderLt   = Color(0xFFE2EBEA) // hairline borders
val BorderDk   = Color(0xFFC8D8D5) // stronger borders
```

Material 3 `lightColorScheme` mapping:
- `primary = DeepTeal`, `onPrimary = White`
- `primaryContainer = Mist`, `onPrimaryContainer = DeepTeal`
- `secondary = Sage`, `background = Linen`, `onBackground = Ink`
- `surface = White`, `onSurface = Ink`, `surfaceVariant = Mist`
- `outline = BorderDk`, `outlineVariant = BorderLt`
- `onSurfaceVariant = Slate`
- Use a dark-on-light scheme only; **no dark theme for v1.**

**Typography** (Inter for text, JetBrains Mono for labels/mono):
- Display: Inter 800, 40sp, letter-spacing −0.03em
- Heading: Inter 700, 22sp, −0.02em
- Subheading/title: Inter 600, 15sp
- Body: Inter 400, 14sp, color Slate
- Label/mono: JetBrains Mono 500, 11sp, letter-spacing 0.12em, uppercase, color DeepTeal

Add Inter and JetBrains Mono as fonts (bundled `res/font` or Compose Downloadable Fonts from Google Fonts). Corners: 10–14dp on cards/fields; pill (100dp) for tags/badges. Soft shadows only (elevation 1–2dp).

## Screen inventory (see mockups for layout)
Bottom nav: **Log · History · Calendar · Reports**. **Projects** opens from the Log screen's project selector ("Manage").
1. **Log Hours** (home) — project (last-used default), date (today default), quarter-hour stepper + quick presets, freeform notes, Save.
2. **History** — reverse-chronological running table grouped by period, running total, tap row to edit/delete.
3. **Calendar** — month grid marking only days with entries; month total + week subtotals above; tap a day for its entries.
4. **Reports** — Week/Month/Quarter/Year toggle, period total, per-project breakdown, CSV export.
5. **Projects** — preset + custom add, rename, archive.

## Conventions
- Package layout: `data/` (entities, dao, db, repository), `di/` (AppContainer), `ui/theme/`, `ui/<screen>/` (screen composable + ViewModel + UiState), `ui/components/` (shared), `util/` (date helpers, csv).
- One ViewModel per screen. UI state as an immutable `data class`. Collect with `collectAsStateWithLifecycle()`.
- Keep composables preview-able with `@Preview` using fake data.
- Money-free, network-free, permission-free until Phase 6 (which adds only a FileProvider, not a runtime permission).

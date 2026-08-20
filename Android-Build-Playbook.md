# Volunteer Hours Tracker — Android Build Playbook

A phased plan for building the app with a coding agent (Claude Code), where **every phase ends in something you can build and review on device** before moving on. Each phase has a discrete, copy-paste prompt and a review checklist.

---

## How to use this

1. Create an empty folder for the project and drop these three files in the root: `Volunteer-Hours-PRD.html`, `Volunteer-Hours-Screens.html`, and `CLAUDE.md`.
2. Open Claude Code in that folder. It will read `CLAUDE.md` automatically for standing context (stack, data model, Sage tokens, constraints).
3. Work one phase at a time. Paste the phase prompt, let it work, then **build and run in Android Studio on your device** against the review checklist.
4. Only move to the next phase once the current one passes. If something's off, describe it and iterate before advancing.

**Why this sequence:** it front-loads the two things that are expensive to change later — the data model and the navigation shell — then adds one reviewable screen at a time. The capture loop (Log) comes before the read screens (History, Reports, Calendar) because those need real entries to review against. Export and hardening come last, once behavior is stable.

---

## Phase map

| # | Phase | Delivers | You can review on device |
|---|---|---|---|
| 0 | Skeleton, theme, nav | Buildable app, Sage theme, bottom-nav shell, empty screens | Launch, tab between screens, see the design language |
| 1 | Data layer + Projects | Room DB, repository, manual DI; Projects screen (add/rename/archive) | Presets seeded; project changes persist across restarts |
| 2 | Log Hours | The capture loop writing real entries | Log an entry; last-used project returns on reopen |
| 3 | History | Running table, grouped, edit/delete | Entries appear; edit and delete work; totals correct |
| 4 | Reports | Week/Month/Quarter/Year totals + per-project breakdown | Switch periods; totals match known data |
| 5 | Calendar | Month grid + window summaries + day detail | Only logged days marked; month/week totals correct |
| 6 | CSV export | CSV to the native Android share sheet | Export opens share sheet; CSV opens cleanly with right values |
| 7 | Polish & hardening | Empty states, validation, confirmations, a11y, icon | Full pass; nothing rough on the happy or edge paths |

**Dependencies:** 0 → 1 → 2 → (3, 4, 5 in any order) → 6 → 7.

---

## Ground rules to repeat if the agent drifts
- Compile and run at every phase boundary. Never hand back a non-building state.
- Only implement the current phase. Stub later needs minimally.
- No Hilt, no network, no third-party SDKs beyond AndroidX/Compose/Room.
- Follow the Sage tokens and the screen mockups. One accent, used sparingly.

---

# The Prompts

Copy each block verbatim into Claude Code when you reach that phase.

---

## Phase 0 — Project skeleton, Sage theme, navigation shell

> **Goal:** a buildable, runnable app with the Sage look and a working bottom nav, before any data or real screens.
>
> Set up a new single-module Android app from scratch in this repo. Read `CLAUDE.md` for all locked decisions and Sage design tokens, and `Volunteer-Hours-Screens.html` for the visual language.
>
> Requirements:
> - Gradle (Kotlin DSL), Jetpack Compose, Material 3. `minSdk 26`, `compileSdk 35`, `targetSdk 35`. App id `com.dean.hourstracker`. No internet permission in the manifest.
> - Implement the Sage theme in `ui/theme/`: `Color.kt` (the exact tokens from CLAUDE.md), `Type.kt` (Inter for text, JetBrains Mono for labels — use Compose Downloadable Fonts from Google Fonts), and `Theme.kt` with a single light `lightColorScheme` mapping from CLAUDE.md. No dark theme.
> - Add `androidx.navigation:navigation-compose`. Build a `MainActivity` with a `Scaffold` + `NavigationBar` with four destinations: **Log, History, Calendar, Reports** (use appropriate Material icons). Add a fifth route **Projects** that is NOT in the bottom bar (it will be opened from Log later).
> - Create five placeholder screen composables (`ui/log`, `ui/history`, `ui/calendar`, `ui/reports`, `ui/projects`), each just a centered title on the Linen background using the themed typography, so the design language is visible.
> - Create the package structure from CLAUDE.md's conventions (`data/`, `di/`, `ui/theme/`, `ui/<screen>/`, `ui/components/`, `util/`), even if some are empty.
>
> Do not add Room or any data yet. Make sure it builds and runs.
>
> **Acceptance:** app launches to the Log placeholder; bottom nav switches between the four screens; Sage colors and Inter/JetBrains Mono are visibly applied.

**Review checklist (on device):**
- [ ] App installs and launches without crash
- [ ] Bottom nav shows Log / History / Calendar / Reports and switches screens
- [ ] Background is Linen, text uses Inter, any mono label uses JetBrains Mono
- [ ] Deep Teal / Sage accents render correctly

---

## Phase 1 — Data layer + Projects screen

> **Goal:** the persistence foundation, exercised by the simplest real screen (Projects). Getting the schema right here avoids migrations later.
>
> Read `CLAUDE.md` (data model, seeding, conventions) and the Projects screen in `Volunteer-Hours-Screens.html`.
>
> Requirements:
> - Add Room. Create entities `Project` and `Entry` exactly as specified in CLAUDE.md, an `EntryWithProject` relation POJO, `ProjectDao` and `EntryDao`, and `AppDatabase`.
>   - `ProjectDao`: observe active projects (not archived) and all projects as `Flow`; insert; update (for rename/archive).
>   - `EntryDao`: insert, update, delete; the queries later phases need can be stubbed now, but include `SELECT projectId FROM entries ORDER BY createdAt DESC LIMIT 1` as `getLastUsedProjectId()`. Also expose per-project total hours so the Projects screen can show them.
> - Seed presets on DB creation via a `RoomDatabase.Callback` (the five names in CLAUDE.md, `isPreset = true`).
> - Add the manual DI: an `App : Application` holding an `AppContainer` (database + a `HoursRepository` that wraps the DAOs). Wire `App` in the manifest.
> - Add a small `ViewModelProvider.Factory` (or `viewModelFactory {}`) pattern so screens can get ViewModels with the repository.
> - Build the **Projects screen** to match the mockup: an "Add a project" row (text field + add button) that inserts a custom project; an **Active** section listing active projects with their total hours and entry count and a PRESET/CUSTOM/IN USE tag; an **Archived** section. Support rename and archive/unarchive (a row menu or swipe — your call, keep it simple and Sage-styled). Back-navigation returns to Log.
> - Wire Projects to open from the Log placeholder (a temporary button is fine until Phase 2).
>
> **Acceptance:** presets appear on first launch; adding, renaming, and archiving a project persists across an app restart; archived projects show in the Archived section.

**Review checklist (on device):**
- [ ] Five preset projects present on first run
- [ ] Add a custom project → it appears and survives restart
- [ ] Rename works and persists
- [ ] Archive moves a project to Archived; unarchive returns it
- [ ] Hours/entry counts show (0 for now — no entries yet)

---

## Phase 2 — Log Hours (the capture loop)

> **Goal:** the core five-second logging flow, writing real entries to Room.
>
> Read the Log Hours screen in `Volunteer-Hours-Screens.html` and CLAUDE.md (last-used logic, quarter-hour hours).
>
> Build the **Log Hours** screen and its ViewModel to match the mockup:
> - **Project selector:** defaults to the last-used project (`getLastUsedProjectId()`; fall back to the first active project on first run). Tapping it opens a picker of active projects, with a "Manage projects" action that navigates to the Projects screen.
> - **Date picker:** defaults to today. Use a Material 3 `DatePicker`. Show it as "Today · Mon D" when it's today.
> - **Hours control:** a large value display in a Mist card, with − / + stepper moving in **0.25 increments** (minimum 0.25), plus quick-pick chips (e.g. 0.25, 0.5, 1, 2.5). Never allow 0 or negative.
> - **Notes:** a freeform multi-line text field, optional.
> - **Save:** writes an `Entry` (project, date as epochDay, hours, note, createdAt = now), shows a brief confirmation (snackbar), and resets the form for the next entry while keeping the same project selected.
> - State lives in the ViewModel as an immutable UiState; the composable is stateless.
>
> **Acceptance:** you can log an entry; it persists (verify via the Projects screen hours count incrementing); closing and reopening the app pre-selects the project you last saved to; date defaults to today.

**Review checklist (on device):**
- [ ] Opens with last-used project + today's date pre-set
- [ ] Hours step in 0.25; can't save 0/negative
- [ ] Notes accept multi-line text
- [ ] Save persists; Projects screen totals reflect it
- [ ] Reopen app → last-used project still defaulted

---

## Phase 3 — History (running table)

> **Goal:** a trustworthy running record with edit/delete.
>
> Read the History screen in `Volunteer-Hours-Screens.html`.
>
> Build the **History** screen + ViewModel:
> - A running-total hero at top (sum of all entries, plus entry count).
> - Entries listed reverse-chronologically, grouped by relative period headers (This week / Last week / earlier months). Each row: project name (with a Sage dot), date + note, hours on the right. Use `EntryWithProject`.
> - Tap a row to **edit** (reuse the Log form as an edit mode, or an edit bottom sheet) and to **delete** (with a confirmation). Edits/deletes update Room and the list reactively.
> - Empty state when there are no entries.
>
> **Acceptance:** all logged entries appear correctly grouped; the running total is correct; editing an entry updates it everywhere; deleting removes it.

**Review checklist (on device):**
- [ ] Entries show newest-first, grouped, with correct running total
- [ ] Edit changes value and reflects in totals
- [ ] Delete asks to confirm, then removes
- [ ] Empty state is clean when no data

---

## Phase 4 — Reports (period totals)

> **Goal:** the one-tap answer to "how many hours this period?"
>
> Read the Reports screen in `Volunteer-Hours-Screens.html` and the reporting-periods section of CLAUDE.md.
>
> Build the **Reports** screen + ViewModel:
> - A segmented control: **Week / Month / Quarter / Year** (calendar-based per CLAUDE.md).
> - A large period total, with the human-readable date range beneath it in the mono label style.
> - A **By project** breakdown: each active project that has hours in the period, with its total and a Sage progress bar proportional to the period max. Sorted descending.
> - Compute ranges with `java.time`, query the Entry range, aggregate in the ViewModel. Handle empty periods gracefully.
> - Leave a placeholder Export button (wired in Phase 6).
>
> **Acceptance:** switching periods recomputes the total and breakdown; the numbers match a hand check against entries you logged.

**Review checklist (on device):**
- [ ] Week/Month/Quarter/Year each show the right total for known data
- [ ] Date range label is correct for each period
- [ ] Breakdown bars and per-project totals are right and sorted
- [ ] Empty period shows a clean zero state

---

## Phase 5 — Calendar (day view + window summaries)

> **Goal:** see logged days at a glance, with summarized window totals alongside the day-by-day detail.
>
> Read the Calendar screen in `Volunteer-Hours-Screens.html`.
>
> Build the **Calendar** screen + ViewModel:
> - A month grid (Sun–Sat columns) with prev/next month navigation. **Only days that have entries** get a Sage dot; empty days are quiet. The selected day is filled Deep Teal.
> - Above the grid, a **This month** summary total (hours + number of logged days).
> - Below the grid, a **By week** breakdown (week ranges within the month with subtotals and Sage bars).
> - Tapping a marked day shows that day's entries beneath, with the same row style as History and a path to edit/delete.
> - Query entries by `dateEpochDay` range for the visible month.
>
> **Acceptance:** dots appear only on days with entries; month and week summaries are correct; tapping a day shows its entries; month navigation updates everything.

**Review checklist (on device):**
- [ ] Dots only on logged days; selected day highlighted
- [ ] "This month" total and "By week" subtotals correct
- [ ] Tap a day → its entries show and are editable
- [ ] Prev/next month updates grid + summaries

---

## Phase 6 — CSV export via the native share sheet

> **Goal:** get hours out of the app with zero accounts or network.
>
> This is the only place data leaves the device, and it must stay offline: build a CSV file and hand it to the Android share sheet. No Google OAuth, no internet permission.
>
> Requirements:
> - Add a `FileProvider` (authority `${applicationId}.fileprovider`) with a `res/xml/file_paths.xml` pointing at a cache subfolder. No new runtime permissions.
> - Add a `util/CsvExporter` that writes the current Reports period to a CSV in cache with columns: `Date, Project, Hours, Note` (ISO date, proper CSV escaping for commas/quotes/newlines in notes). Filename like `volunteer-hours_<period>_<yyyyMMdd>.csv`.
> - Wire the Reports **Export CSV — Share…** button to build the CSV for the selected period and launch `ACTION_SEND` (type `text/csv`) via a chooser, so the user routes it to email/Drive/Sheets/Files.
> - Consider also offering "export all entries" — but keep the Reports-period export as the primary path.
>
> **Acceptance:** tapping export opens the Android share sheet; sending to Drive/Files/Sheets produces a CSV that opens correctly with the right rows and values, including notes that contain commas.

**Review checklist (on device):**
- [ ] Export opens the native share sheet
- [ ] Resulting CSV opens in Sheets/Excel with correct columns and values
- [ ] A note containing a comma or quote is escaped correctly
- [ ] No network permission was added

---

## Phase 7 — Polish & hardening

> **Goal:** make the happy path effortless and the edges safe. No new features — tighten what exists.
>
> Do a hardening pass across the app:
> - **Empty states** for History, Reports, Calendar when there's no data — friendly, Sage-styled, pointing to Log.
> - **Validation:** hours must be > 0 and a multiple of 0.25; a project must exist before logging (if all projects are archived, prompt to add one). Guard the date picker sensibly (allow past dates; decide on future — default: allow, since people log planned/backdated hours).
> - **Confirmations:** deletes (entries) and archive actions confirm; show undo where cheap.
> - **Last-used correctness:** verify the derived last-used project holds after edits/deletes and across restarts.
> - **Accessibility:** content descriptions on icon buttons, min 48dp touch targets, adequate contrast (Slate-on-Linen for secondary text is fine; check disabled states).
> - **Visual consistency pass** against the Sage tokens and mockups: spacing, corner radii, one-accent discipline, shadow weight.
> - **App icon** + label using the Sage palette (adaptive icon).
> - Basic `@Preview`s for each screen with fake data.
>
> **Acceptance:** a full walkthrough (log → history → edit → reports → calendar → export) feels finished; edge cases (no projects, no entries, huge notes, many entries) don't break or look rough.

**Review checklist (on device):**
- [ ] Every screen has a clean empty state
- [ ] Can't create invalid entries; helpful messages when blocked
- [ ] Destructive actions confirm; undo where offered works
- [ ] Icons/labels accessible; touch targets comfortable
- [ ] App icon set; overall look matches the mockups

---

## After v1 (deliberately deferred — not in these prompts)
Kept out to protect the "smallest version that delivers value." Revisit only as explicit decisions:
- Local backup / cloud sync (the durability gap — top candidate).
- Direct Google Sheets write (the share sheet already covers CSV → Sheets).
- Reminders, goals, streaks, notifications.
- iOS or web clients.

---

*Built from `Volunteer-Hours-PRD.html` v1.0 and `Volunteer-Hours-Screens.html`. Sage design system. Framework → Evidence → Recommendation → Implications.*

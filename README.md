# Volunteer Hours Tracker

A single-user Android app for logging volunteer hours and reporting totals by period. Built to replace a spreadsheet-and-memory workflow — local-only storage, no accounts, no network.

## Status

🚧 In active development, built phase-by-phase. See [Android-Build-Playbook.md](Android-Build-Playbook.md) for the full phase map. Phase 0 (project skeleton, theme, navigation shell) is complete and builds cleanly.

## What it does

- **Log Hours** — pick a project (defaults to last used), pick a date (defaults to today), enter hours in quarter-hour increments, add a note, save.
- **History** — reverse-chronological log grouped by period, with running totals. Tap any entry to edit or delete it.
- **Calendar** — month grid marking days with logged hours, with month and week subtotals.
- **Reports** — totals by Week / Month / Quarter / Calendar Year, broken down per project, exportable to CSV via the native Android share sheet.
- **Projects** — manage the list of projects: seeded presets (Community Service, Food Bank, Animal Shelter, Beach Cleanup, Library), plus add/rename/archive your own.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository, `StateFlow`-driven state |
| Persistence | Room (SQLite), on-device only |
| DI | Manual (`AppContainer` + a small `ViewModelProvider.Factory`) — no Hilt/Dagger/Koin |
| Navigation | `androidx.navigation:navigation-compose` |
| Dates | `java.time` |

`minSdk 26` · `compileSdk 35` · `targetSdk 35` · single app module · app id `com.dean.hourstracker`.

## Design system

A custom "Sage" design system: dark-on-light Material 3 theme, one accent color used sparingly, Inter for text and JetBrains Mono for labels. Tokens are documented in [CLAUDE.md](CLAUDE.md).

## Privacy

No internet permission, no analytics, no third-party SDKs beyond AndroidX/Compose/Room. All data stays in a local Room database on-device. The only way data leaves the device is a user-initiated CSV export through the Android share sheet.

## Building

```bash
./gradlew assembleDebug
```

Open in Android Studio and run on a device or emulator (`minSdk 26`+). No API keys or configuration required — it's local-only.

## Project docs

- [Volunteer-Hours-PRD.html](Volunteer-Hours-PRD.html) — full product spec, source of truth for scope
- [Volunteer-Hours-Screens.html](Volunteer-Hours-Screens.html) — screen mockups
- [Android-Build-Playbook.md](Android-Build-Playbook.md) — phase-by-phase build plan and prompts
- [CLAUDE.md](CLAUDE.md) — locked technical decisions, data model, and design tokens for this repo

## License

[MIT](LICENSE)

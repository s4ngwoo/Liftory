# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Korean translation: [ko/CHANGELOG.md](ko/CHANGELOG.md)

---

## [Unreleased]

### Added
- Standardized documentation system (`docs/` and `notes/`).
- Android Clean Architecture scaffold with Jetpack Compose.
- Workout session edit (name/notes) and deletion with confirmation dialog and CASCADE set cleanup.
- Routine template interactive card with exercise preview list, one-tap workout start, and routine deletion.
- Real-time cumulative workout elapsed timer and finish workout completion dialog.
- Live rest stopwatch/countdown timer (+30s, pause/resume, stopwatch mode) docked in session detail.
- Per-exercise evaluation feedback notes section on exercise group cards via `SessionNotesManager`.
- Real-time estimated 1RM calculation with RPE/RIR adjustment (`CalculateOneRepMaxUseCase`) and live feedback badge in `ExerciseSetEditorSheet`.
- Previous set one-touch copy button and ghost placeholders in `ExerciseSetEditorSheet`.
- Backup data import and restoration (`ImportWorkoutDataUseCase` & `DataImporterImpl`) supporting JSON and CSV in `StatisticsDashboardScreen`.
- Clean Architecture and TDD rules added to `GEMINI.md` and `.agents/rules/`.

### Fixed
- Fixed software keyboard Enter key inserting newlines instead of jumping to the next input field.
- Fixed `ExerciseSetEditorSheet` input field being pushed off-screen/hidden beneath the keyboard by adopting a compact horizontal 3-column row (64dp height), `skipPartiallyExpanded = true`, and vertical scrolling.

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
- Routine template editing: edit routine title, add/remove exercises, adjust target weight/reps presets, with search & equipment-filter exercise library picker (`UpdateRoutineTemplateUseCase`).
- Last workout history reference: view past session date and exact sets per exercise, with one-touch copy (`[지난 세션 복사]`) in set editor bottom sheet (`GetLastExerciseHistoryUseCase`).
- Exercise equipment categorization: Free Weight vs Machine (`EquipmentType`) division with machine brand selection (Hammer Strength, Cybex, Life Fitness, Newtech, etc.) and equipment filter chips.
- Database migration: Room schema v2 migration (`MIGRATION_1_2`) safely adding equipment type and machine brand columns with indices.
- Workout session edit (name/notes) and deletion with confirmation dialog and CASCADE set cleanup.
- Routine template interactive card with exercise preview list, one-tap workout start, and routine deletion.
- Real-time cumulative workout elapsed timer and finish workout completion dialog.
- Live rest stopwatch/countdown timer (+30s, pause/resume, stopwatch mode) docked in session detail.
- Per-exercise evaluation feedback notes section on exercise group cards via `SessionNotesManager`.
- Real-time estimated 1RM calculation with RPE/RIR adjustment (`CalculateOneRepMaxUseCase`) and live feedback badge in `ExerciseSetEditorSheet`.
- Previous set one-touch copy button and ghost placeholders in `ExerciseSetEditorSheet`.
- Backup data import and restoration (`ImportWorkoutDataUseCase` & `DataImporterImpl`) supporting JSON and CSV in `StatisticsDashboardScreen`.
- Session Detail UI Reorganization: Unified top time dashboard (cumulative elapsed timer, interactive rest timer with +30s/pause/stop/chips, finish workout button) and middle workout content zone (session title/notes, edit/delete, exercises, sets, notes), removing the bottom dock for a consistent user experience.
- In-App Persistent Workout Timer Banner: Prominent top banner displayed across all main app tabs during an ongoing workout, showing real-time ticking elapsed time and a one-touch `[운동 복귀 >]` quick navigation button.
- System Status Bar Chronometer Notification: Native Android Foreground Service (`WorkoutTimerService`) displaying an OS-managed ticking chronometer in the system status bar when switching to other apps or leaving the screen, with zero CPU wake battery drain and 1-tap return to the session.
- Reactive Active Workout Tracking (`ObserveActiveWorkoutSessionUseCase`): Clean architecture domain UseCase & repository flow for reactive real-time workout session tracking.

### Changed
- Sessions Screen FAB: Hide redundant bottom-right `+` Floating Action Button when sessions list is empty, keeping only the prominent central "Start Today's Workout" card button.
- Workout Timing Experience: Streamlined to System Status Bar chronometer notification (`WorkoutTimerService`) and in-app persistent top banner, completely removing the floating Picture-in-Picture (PiP) popup to eliminate multitasking obstruction.
- Foreground Service: Migrated Android 14+ FGS type from `health` to `specialUse` to prevent `SecurityException` when running without hardware sensor permissions.

### Fixed
- Fixed software keyboard Enter key inserting newlines instead of jumping to the next input field.
- Fixed `ExerciseSetEditorSheet` input field being pushed off-screen/hidden beneath the keyboard by adopting a compact horizontal 3-column row (64dp height), `skipPartiallyExpanded = true`, and vertical scrolling.

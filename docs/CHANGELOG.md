# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Korean translation: [ko/CHANGELOG.md](ko/CHANGELOG.md)

---

## [Unreleased]

- System Theme Inset Integration & Dark Status Bar Icons: Configured `WindowInsetsControllerCompat` in `StrengthLogTheme` to force dark status bar and navigation bar icons, ensuring high contrast on the bright Bento theme (`#FDF7FF`) even on system-wide dark mode devices.
- Safe Scroll Padding & Bottom Bar Occlusion Elimination: Extended list `contentPadding` across session detail (`bottom = 96.dp`), session list, routine list, and exercise library (`bottom = 88.dp`) to ensure the lowest items and buttons are never obscured by floating action buttons (FAB) or system navigation bars.
- Accessibility (TalkBack) Silence on Chronometer Ticks: Suppressed disruptive 1-second TalkBack announcements on real-time elapsed workout and rest countdown timers using `Modifier.clearAndSetSemantics { }`, replacing them with static parent descriptions and polite status notifications.
- Touch Target Expansion & Table Header Localization: Enlarged set completion checkbox hit targets (36dp) and standardized set table headers into clean Korean labels ("세트", "무게(kg)", "횟수", "RPE").
- Statistics Dashboard Bento Hierarchy & Period Filtering: Reorganized statistics into an intuitive 4-zone layout featuring top period filter chips (Last 7 Days, Last 30 Days, Last 3 Months, All Time) and a 3-column Bento KPI grid (Total Volume, Cardio Minutes, Workout Days), followed by period volume charts and PR badges.
- Safe Backup Restoration & JSON Pre-validation: Relocated destructive backup and restore controls to an isolated bottom card (Zone 4) and implemented real-time JSON validation with detailed entity counts ("✅ 확인됨: 세션 N개 · 세트 M개 포함") before executing data restoration.

- Navigation & Terminology Standardization: Differentiated bottom navigation bar icons with clear Korean labels ("운동", "루틴", "종목 도감", "통계"), replacing duplicate dumbbell icons and English tags.
- Date/Time & Default Title Localization: Formatted session dates to Korean standard ("M월 d일 (E) · 시작 HH:mm · N분 완료"), generated user-friendly default session titles ("M월 d일 운동"), and replaced arbitrary text truncation with dedicated completion status check icons.
- Routine Library & Editor Enhancements: Compacted routine preview lists to 3 items with an "외 N개" summary, streamlined CTA to "이 루틴으로 시작", and added exercise reordering arrows (up/down) and smart cardio target labels in the routine editor modal.
- Warm Completion Dialog Tone: Rephrased finish workout confirmation with gentle guidance ("운동을 마칠까요?" and "운동 마치기").
- Primary CTA Consolidation & Empty Session Overload Elimination: Eliminated 5 competing buttons on newly started sessions (`sets.isEmpty()`) in favor of a single focused welcome card with `[+ 첫 운동 추가]`, hiding the top bar finish button, dashboard set-complete button, and floating action button until exercises are added.
- Set Editor Input Modernization & Unit Clarity: Expanded weight and reps into a prominent 2-column input grid with explicit units (`+1.0 kg`, `+2.5 kg`, `+5.0 kg`, `+10.0 kg` / `+1 회`, `+2 회`, `+5 회` / `+5분`, `+10분`), moved RPE intensity into an optional secondary section with one-touch preset chips.
- Exercise Picker Separation: Separated exercise selection for session from exercise creation, changing the top bar title to "운동 선택", adding a distinct text action button for creating new exercises, hiding the FAB in picker mode, and adding explicit `[+ 추가]` buttons on exercise cards.
- Metrics Separation for Strength Volume vs Cardio Duration: Separated strength volume (kg) and cardio duration (minutes) in statistical queries (`ExerciseSetDao`), mapped human-readable exercise names to PR cards, and eliminated deceptive kg units on cardio exercises.
- Completed Session State Invariant & Read-Only Summary: Fixed session detail screen for completed workouts (`endTime != null`), freezing elapsed timer, removing finish button and rest timer controls, and rendering a read-only summary card to prevent accidental modifications.
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
- Cardio Exercise Library & Dedicated Set Logging UX:
  - Added 6 default cardio exercises: Treadmill (러닝머신 - DRAX), StairMaster (천국의 계단 / 스텝밀 - Matrix), Stationary Cycle (실내 사이클 - Concept2), Incline Treadmill (마이마운틴 / 인클라인 러닝 - MyMountain), Elliptical (일립티컬 - Life Fitness), and Rowing Machine (로잉머신 - Concept2).
  - Extended domain `EquipmentType` with `CARDIO` and `isCardio` helper property.
  - Dedicated `[🏃 유산소]` filter chips across Exercise Library, Custom Exercise Dialog, and Routine Template Exercise Picker.
  - Smart Set Editor: automatically transitions labels from "Weight / Reps" to "Speed·Level / Time (min)", hides irrelevant 1RM badge, and provides "+5min / +10min / +15min" quick delta chips.
  - Cardio Exercise Group Cards in session detail displaying sets as "속도 6.0 · 20분" and table header "Set | 속도/레벨 | 시간(분) | RPE".
- Single Active Session Enforcement & Duplicate Session Prevention:
  - Enforced a strict single active session rule (`endTime == null`) across Domain, Repository, and UI layers so a user cannot run multiple concurrent workouts.
  - Added `ActiveSessionAlreadyExistsException` and `finishExistingActive` option to `CreateWorkoutSessionUseCase` to automatically finish ongoing sessions when intentionally starting anew.
  - Implemented interactive conflict dialogs in `WorkoutSessionListScreen` (FAB and empty start button) and `RoutineTemplateListScreen` ("Start Routine"): prompts the user to either resume the existing session, finish it and start a new one, or cancel.
  - Enhanced session cards (`BentoSessionCard`) with vibrant `[🔥 진행 중]` badges, primary accent borders, and live in-progress indicators vs completed duration.
  - Self-healing legacy cleanup in `WorkoutSessionRepositoryImpl`: automatically closes orphaned active sessions from older versions on startup.

### Changed

- Sessions Screen FAB: Hide redundant bottom-right `+` Floating Action Button when sessions list is empty, keeping only the prominent central "Start Today's Workout" card button.
- Workout Timing Experience: Streamlined to System Status Bar chronometer notification (`WorkoutTimerService`) and in-app persistent top banner, completely removing the floating Picture-in-Picture (PiP) popup to eliminate multitasking obstruction.
- Foreground Service: Migrated Android 14+ FGS type from `health` to `specialUse` to prevent `SecurityException` when running without hardware sensor permissions.

### Fixed
- Fixed software keyboard Enter key inserting newlines instead of jumping to the next input field.
- Fixed `ExerciseSetEditorSheet` input field being pushed off-screen/hidden beneath the keyboard by adopting a compact horizontal 3-column row (64dp height), `skipPartiallyExpanded = true`, and vertical scrolling.

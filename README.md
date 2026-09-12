# Liftory

> Offline-first strength training log and analytics application built with Android Clean Architecture.

[한국어 문서 (Korean)](README_KO.md) · [Documentation](docs/README.md) · [Changelog](docs/CHANGELOG.md)

---

## Overview

**Liftory** is an Android application designed for fitness enthusiasts and athletes to log strength training sessions seamlessly, visualize progress, and manage routines offline-first with Cloud Firestore sync capabilities.

- **Offline-First**: Built with local database caching (Room / Local Storage) ensuring complete privacy and offline usability in gyms without stable connection.
- **Clean Architecture**: Separated into Domain, Data, and Presentation layers using Jetpack Compose and Modern Android Architecture.
- **AI-Powered Insights**: Integrated Gemini API assistance for routine suggestions and workout log analysis.

---

## Features

- **3-Page Focused Workout Mode & Session HUD**: Horizontal pager separating real-time Timer HUD, Current Exercise (planned vs. actual comparison), and Today's Plan overview with 48dp touch targets, TalkBack accessibility, and IME keyboard padding.
- **Persistent Timing Architecture**: Battery-friendly timing system using passive math projections (`TimerCalculator`), persistent in-app top navigation banner, and Android status bar chronometer notification via Foreground Service (`specialUse`).
- **Idempotent State Machine**: Finite state engine (`SetExecutionState`, `SessionExecutionState`) guarded by command ID idempotency and revision checks to prevent double-submits and state drift.
- **Smart Set Input & History Reference**: Compact horizontal 3-column input row with zero keyboard occlusion, previous set copy (`[↺ 이전 세트 복사]`), and one-touch past workout history copy (`[지난 세션 복사]`).
- **Routine & Plan Snapshots**: Decoupled session planning (`SessionPlan`) ensuring routines can be modified without altering ongoing or past workout histories.
- **Advanced Analytics & Load Science**: Clean metric separation (kg, sec, meters), RIR-adjusted e1RM (1~12 reps), session-RPE training load with EWMA decay series, Hooper recovery scores, and outlier-robust Theil-Sen trend regression.
- **Equipment & Gym Catalog**: Equipment models, 3-state inventory (null, 0, n), routine fulfillment evaluation, and in-session substitution.
- **Offline-First Sync & Conflict Resolution**: Local Room DB primary with revision/tombstone priority, user-isolated sync outboxes, and sensitive health payload privacy filtering.

---

## Documentation

All public project documentation is maintained in [`docs/`](docs/README.md):

| Document | English | 한국어 (Korean) | Description |
| :--- | :--- | :--- | :--- |
| **Documentation Index** | [docs/README.md](docs/README.md) | [docs/ko/README.md](docs/ko/README.md) | Documentation map |
| **Changelog** | [docs/CHANGELOG.md](docs/CHANGELOG.md) | [docs/ko/CHANGELOG.md](docs/ko/CHANGELOG.md) | Version history & changes |
| **UI/UX Overview** | [docs/UI_UX_OVERVIEW.md](docs/UI_UX_OVERVIEW.md) | [docs/ko/UI_UX_OVERVIEW.md](docs/ko/UI_UX_OVERVIEW.md) | Screen gallery & UX evaluation |
| **Contributing** | [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | [docs/ko/CONTRIBUTING.md](docs/ko/CONTRIBUTING.md) | Contribution guidelines |
| **Security** | [docs/SECURITY.md](docs/SECURITY.md) | [docs/ko/SECURITY.md](docs/ko/SECURITY.md) | Security vulnerability disclosure |
| **Release Guide** | [docs/RELEASING.md](docs/RELEASING.md) | [docs/ko/RELEASING.md](docs/ko/RELEASING.md) | Release & tagging checklist |

---

## Tech Stack

- **Platform**: Android (Min SDK 24, Target SDK 36)
- **Language**: Kotlin 2.x
- **UI**: Jetpack Compose, Material 3
- **Architecture**: MVVM / Clean Architecture
- **Backend / Services**: Google Cloud / Firebase (Firestore, Auth)

---

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/Liftory.git
   ```
2. Set up environment variables and keys:
   ```bash
   cp .env.example .env
   ```
3. Open the project in Android Studio and build:
   ```bash
   ./gradlew assembleDebug
   ```

---

## License

This project is licensed under the terms of the MIT License.

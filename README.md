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

- **Workout Logging & Unified Session HUD**: Real-time elapsed workout timer, interactive rest stopwatch/countdown (+30s, pause/resume, chips), per-exercise feedback notes, and estimated 1RM calculation.
- **Global Background & PiP Timer**: 3-tiered persistent timing system — in-app top navigation banner, Android status bar chronometer notification via Foreground Service (zero CPU wake lock, battery-friendly), and Picture-in-Picture (PiP) 16:9 floating HUD popup with auto-enter on swipe to home.
- **Smart Set Input & History Reference**: Compact horizontal 3-column input row with zero-keyboard-occlusion, previous set copy (`[↺ 이전 세트 복사]`), and one-touch past workout history copy (`[지난 세션 복사]`).
- **Routine Management & Customization**: Create, edit, and reorder routines with exercise presets, live last-used dates, and instant session start.
- **Equipment Categorization**: Free Weight vs. Machine classification with popular gym machine brand selection (Hammer Strength, Cybex, Life Fitness, Newtech, etc.).
- **Analytics & History**: Track 1RM progression, volume per muscle group, and JSON/CSV backup data export/import.
- **Cloud Backup & Sync**: Optional offline-first sync with Firebase / Firestore.

---

## Documentation

All public project documentation is maintained in [`docs/`](docs/README.md):

| Document | English | 한국어 (Korean) | Description |
| :--- | :--- | :--- | :--- |
| **Documentation Index** | [docs/README.md](docs/README.md) | [docs/ko/README.md](docs/ko/README.md) | Documentation map |
| **Changelog** | [docs/CHANGELOG.md](docs/CHANGELOG.md) | [docs/ko/CHANGELOG.md](docs/ko/CHANGELOG.md) | Version history & changes |
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

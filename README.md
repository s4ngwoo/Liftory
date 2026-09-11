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

- **Workout Logging**: Track exercises, sets, reps, weights, RPE, and notes.
- **Analytics & History**: Track 1RM progression, volume per muscle group, and consistency heatmaps.
- **Routine Management**: Create and follow custom workout splits and templates.
- **Cloud Backup & Sync**: Optional sync with Firebase/Firestore.

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

# Development setup

Korean translation: [ko/DEVELOPMENT.md](ko/DEVELOPMENT.md)

How to build, test, and debug Liftory. Verify commands against this repo; do not assume extra modules or flavors exist.

---

## Prerequisites

| Tool | Constraint (from Gradle) |
| :--- | :--- |
| JDK | **21** (`jvmToolchain(21)`, `sourceCompatibility` 21) |
| Android Studio | Recent stable; AGP **8.3.1**, Gradle **9.3.1** |
| SDK | `compileSdk` / `targetSdk` **36**, `minSdk` **24** |
| Kotlin | **2.0.0** (`gradle/libs.versions.toml`) |

Application id: `com.aistudio.strengthlog.vpmq`. Source namespace is still `com.example`.

---

## Clone and first build

```bash
git clone https://github.com/s4ngwoo/Liftory.git
cd Liftory
cp .env.example .env
./gradlew assembleDebug
```

Open the **root** Gradle project in Android Studio (not a nested module).

Secrets Gradle plugin (`app/build.gradle.kts`) reads `.env` with fallback `.env.example`. `.env` is gitignored. The example file only documents `GEMINI_API_KEY`; leave it commented unless you need Gemini/Firebase AI packaging.

`google-services` is set to **`MissingGoogleServicesStrategy.WARN`**. There is **no** `google-services.json` in the repo. Debug builds succeed without Firebase; remote sync then fails closed (`FirestoreSyncDataSource` logs and returns failure if Firebase is uninitialized or the user is logged out).

---

## Common commands

| Goal | Command |
| :--- | :--- |
| Debug APK | `./gradlew assembleDebug` |
| Unit tests (incl. Robolectric) | `./gradlew testDebugUnitTest` |
| Full check | `./gradlew check` |
| Instrumented tests | `./gradlew connectedDebugAndroidTest` |
| Release bundle | `./gradlew bundleRelease` (needs signing env; see [RELEASING.md](RELEASING.md)) |

Architecture import rules: `ArchitectureRulesTest` (`ARCH-01` domain, `ARCH-02` application).

---

## Debug vs release signing

```kotlin
// app/build.gradle.kts (abridged)
debug → ${rootDir}/debug.keystore  (alias androiddebugkey / password android)
release → KEYSTORE_PATH or ${rootDir}/my-upload-key.jks
         STORE_PASSWORD, KEY_PASSWORD; alias upload
```

`debug.keystore` is **gitignored**. If `assembleDebug` fails with a missing keystore, generate a standard Android debug keystore at the repo root or let Android Studio create one and point `storeFile` at it.

Do not commit `.env`, `local.properties`, keystores, or `notes/` (private AI/dev notes; listed in `.gitignore`).

---

## Runtime permissions and services

| Item | Where | Pitfall |
| :--- | :--- | :--- |
| `POST_NOTIFICATIONS` | `MainActivity` on API 33+ | Chronometer notification will not appear until granted; session still runs locally |
| `FOREGROUND_SERVICE_SPECIAL_USE` | `AndroidManifest.xml` + `WorkoutTimerService` | Android 14+ uses `specialUse`, not `health` (avoids `SecurityException` without body sensors) |
| TalkBack on timers | session / workout UI | Tick text is suppressed with `clearAndSetSemantics`; do not re-enable live region on the chronometer |

---

## Backup import/export

JSON backup schema lives in `ExportEntityPayload` (`CURRENT_SCHEMA_VERSION = 2`). `DataImporterImpl`:

1. Rejects malformed JSON and `schemaVersion` outside `1..2`.
2. Rejects sets whose `sessionId` is missing from both the payload and Room.
3. Inserts sessions then sets; returns imported row count.

CSV import is also supported (`ImportWorkoutDataUseCase.importFromCsv`). Treat restore as destructive: the statistics UI keeps backup controls isolated and pre-validates JSON before writing.

---

## Troubleshooting

| Symptom | Likely cause | What to check |
| :--- | :--- | :--- |
| `assembleDebug` fails on keystore | Missing gitignored `debug.keystore` | Create debug keystore at repo root |
| Google Services / Firebase warnings | No `google-services.json` | Expected in this repo; local logging still works |
| Sync never uploads | No network constraint, not logged in, Firebase missing, or `retryCount >= 5` | `SyncWorker` logs; outbox in Room |
| Cannot start a second workout | Single active session (`endTime == null`) | Resume, or `CreateWorkoutSessionUseCase(finishExistingActive = true)` |
| 3-page workout mode empty | `WorkoutModeViewModel` is not bound to `WorkoutExecutionRepository` | Domain/use cases exist; Room impl and DI wiring are still outstanding (see [ARCHITECTURE.md](ARCHITECTURE.md)) |
| Architecture test failure | Forbidden import in `domain` or `application` | Move Android/Room/Firebase types to `infrastructure` |
| Keyboard covers set inputs | IME padding / compact 3-column editor | Follow existing `ExerciseSetEditorSheet` pattern; do not stack extra vertical fields |
| Domain package still `com.example` | Historical namespace | Search/refactors must use `com.example.*`, not the Play applicationId |

---

## Workflow for a feature

1. Domain model / calculator / repository port first (pure Kotlin).
2. Failing unit test (`app/src/test/...`).
3. Application use case (`operator fun invoke`).
4. Infrastructure adapter + mapper (`toDomain()` / `toEntity()`).
5. Presentation: one `UiState` `StateFlow`, stateless composables.
6. `./gradlew testDebugUnitTest`.
7. User-facing change → `[Unreleased]` in both `docs/CHANGELOG.md` and `docs/ko/CHANGELOG.md`.

Details: [CONTRIBUTING.md](CONTRIBUTING.md), [ARCHITECTURE.md](ARCHITECTURE.md), [DOMAIN.md](DOMAIN.md).

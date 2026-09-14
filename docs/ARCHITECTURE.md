# Architecture

Korean translation: [ko/ARCHITECTURE.md](ko/ARCHITECTURE.md)

This document describes Liftory’s runtime architecture as implemented in source. It is the map for public interfaces, layer boundaries, and what is actually wired into the app vs. domain-only.

---

## Intent

Liftory is an **offline-first** Android workout logger. Local Room writes always succeed independently of network. Firestore sync is best-effort and must not block gym-floor UI.

The N00–N16 work introduced a four-layer package layout with **pure domain calculators** and **use-case orchestration**. Presentation talks to use cases (or ports), never to Room/Firebase types.

---

## Layer map

```
Presentation  →  Application  →  Domain  ←  Infrastructure
     UI / VM        UseCases      models,      Room, WorkManager,
                                  ports,       Firebase, FGS
                                  calculators
```

| Layer | Package | May depend on | Must not depend on |
| :--- | :--- | :--- | :--- |
| **Domain** | `com.example.domain` | Kotlin stdlib only | `presentation`, `infrastructure`, `android.*`, Room, WorkManager, Compose, Firebase |
| **Application** | `com.example.application` | Domain | `presentation`, `infrastructure`, `android.content.Context`, WorkManager, Compose |
| **Infrastructure** | `com.example.infrastructure` | Domain (+ Android / Room / Firebase) | Presentation UI types |
| **Presentation** | `com.example.presentation` | Application, Domain, `di.AppContainer` | Room entities, Firestore DTOs |

Enforced by `ArchitectureRulesTest` (`ARCH-01`, `ARCH-02`) under `app/src/test/java/com/example/architecture/`.

Domain repository **interfaces** live in `domain.repository`. Implementations live in `infrastructure.repository`. Framework clocks, notifications, and WorkManager are injected through `domain.port.*`.

---

## Composition root

Manual DI, not Hilt/Koin:

| Type | File | Role |
| :--- | :--- | :--- |
| `StrengthLogApplication` | `app/src/main/java/com/example/StrengthLogApplication.kt` | Holds `AppContainer` |
| `AppContainer` / `DefaultAppContainer` | `app/src/main/java/com/example/di/AppContainer.kt` | Lazy Room DB, repositories, use cases, clocks, `RestTimerManager` |
| `SyncWorker` | `infrastructure/work/SyncWorker.kt` | Reads the same container from `StrengthLogApplication` |

ViewModels are constructed in `presentation/Navigation.kt` and `presentation/MainScreen.kt` with `ViewModelProvider.Factory` lambdas that pull use cases from `AppContainer`.

---

## Persistence (what is on disk today)

Room database `strength_log.db`, **schema version 3**, exported to `app/schemas/`.

| Entity | Table | Notes |
| :--- | :--- | :--- |
| `WorkoutSessionEntity` | sessions | `endTime == null` means active |
| `ExerciseSetEntity` | exercise_sets | v3 added `isCompleted`, `targetReps` |
| `ExerciseEntity` | exercises | v2 added `equipmentType`, `machineBrand` |
| `RoutineTemplateEntity` + `ExercisePresetEntity` | routine templates / presets | Seeded Push / Pull / Leg defaults |
| `PendingUploadEntity` | pending uploads | Sync outbox |

Migrations: `MIGRATION_1_2`, `MIGRATION_2_3` in `StrengthLogDatabase`. First launch seeds default exercises (including six cardio machines) via `OnConflictStrategy.IGNORE`.

**Not yet backed by Room:** `SessionPlanRepository` and `WorkoutExecutionRepository` are domain ports with use cases and unit tests (fakes). There is no `*Impl` in `infrastructure`. The 3-page workout mode UI (`WorkoutModeViewModel`) currently holds execution in memory unless a test injects it.

---

## Navigation and workout UX

Root graph (`AppNavigation`):

```
login → main → session/{sessionId} → workout_mode/{sessionId}
```

Bottom tabs on `main` (`MainScreen`): `sessions` · `routines` · `exercises` · `stats` (labels: 운동 / 루틴 / 종목 도감 / 통계).

| Surface | Code | Wired persistence |
| :--- | :--- | :--- |
| Session list / detail, set editor | `WorkoutSessionViewModel` + Room use cases | Yes |
| Routines, exercise library, stats, backup | matching ViewModels + Room / export | Yes |
| 3-page workout mode | `WorkoutModeScreen` / `WorkoutModeViewModel` | Navigation only; ViewModel is constructed with `wallClock` and does not load `WorkoutExecutionRepository` |
| Auth | `LoginScreen` / `AuthViewModel` | Firebase Auth when `google-services.json` is present |

While a session is active (`endTime == null`), `MainActivity` starts `WorkoutTimerService` (`foregroundServiceType=specialUse`) via `NotificationScheduler`. Notification tap deep-links with `EXTRA_SESSION_ID` back to `session/{id}`. FGS or notification permission failure must not stop local session observation.

---

## Offline-first sync pipeline

1. Local Room write (source of truth).
2. Optional enqueue on `SyncQueueRepository` (`PendingUpload`).
3. `StartSyncWorkUseCase` → `SyncScheduler` (`WorkManagerSyncScheduler`) requires **network connected**.
4. `SyncWorker` pulls up to **20** pending items, skips `retryCount >= 5`, calls `RemoteSyncDataSource.sync`.
5. `FirestoreSyncDataSource` writes under `users/{uid}/sessions|sets|exercises|routines`. Missing Firebase or logged-out user → `Result.failure`; worker retries.

Conflict **policy** (domain, used by tests / future merge): `SyncConflictResolver` — tombstone + higher `revision` wins; equal revision uses `updatedAtEpochMs`. Concurrent active sessions keep local and set `isConflict` (never silently terminate). Outbox isolation is per `userId` (`InMemorySyncOutbox`). Payload privacy: `SharingPayloadFilter`. Schema: `SyncSchemaGate` rejects `schemaVersion` above the supported max.

Checked-in Firestore rules (`app/src/main/assets/firestore.rules`): authenticated user may read/write only `users/{userId}/**` when `request.auth.uid == userId`.

---

## Clocks and timers

| Port | Default impl | Purpose |
| :--- | :--- | :--- |
| `WallClock` | `WallClock.System` | Epoch millis for persisted intervals |
| `MonotonicClock` | `SystemMonotonicClock` | Elapsed realtime for duration math |
| `NotificationScheduler` | `AndroidNotificationScheduler` | Ongoing chronometer + rest alerts |
| `TimerCalculator` | domain object | Pure projection from stored intervals; UI ticks must not decrement stored seconds |

Rest start is **set completion epoch**, not confirmation wall time. Boot-id mismatch yields `TimerConfidence.Estimated`. See [DOMAIN.md](DOMAIN.md#timer).

---

## Tests that protect the architecture

| Area | Location |
| :--- | :--- |
| Layer imports | `architecture/ArchitectureRulesTest.kt` |
| Execution / plan use cases | `application/usecase/execution`, `application/usecase/plan` |
| Domain calculators | `domain/{execution,statistics,load,trend,gym,library,payment,operations,cohort}` |
| Room / export / sync | `infrastructure/` |
| Workout mode UI state | `presentation/workout/WorkoutModeViewModelTest.kt` |

Prefer lightweight fakes over mocking frameworks (`FakePlanRepository`, `FakeTransactionProvider`).

---

## Related docs

- [DOMAIN.md](DOMAIN.md) — public domain APIs, constraints, examples
- [DEVELOPMENT.md](DEVELOPMENT.md) — setup, commands, pitfalls
- [UI_UX_OVERVIEW.md](UI_UX_OVERVIEW.md) — screen-level UX
- [CONTRIBUTING.md](CONTRIBUTING.md) — PR and changelog rules

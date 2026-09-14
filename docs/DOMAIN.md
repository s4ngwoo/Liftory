# Domain APIs and constraints

Korean translation: [ko/DOMAIN.md](ko/DOMAIN.md)

Public **domain** types and calculators introduced with N00–N16. All snippets match production Kotlin. Wiring status is explicit so callers do not assume UI or Room coverage.

---

## Measurement profiles

`MeasurementProfile` / `MeasurementValue` (`domain/model/`):

| Profile | Typical use | Value type |
| :--- | :--- | :--- |
| `WEIGHT_AND_REPS` | Barbell / dumbbell | `WeightAndReps(weightKg, reps)` |
| `BODYWEIGHT_PLUS_REPS` | Pull-up, dip | `BodyweightPlusReps(additionalWeightKg, reps, isAssisted)` |
| `TIME_AND_LEVEL` | StairMill, bike | `TimeAndLevel(durationSeconds, levelOrSpeed)` |
| `TIME_AND_DISTANCE` | Treadmill | `TimeAndDistance(durationSeconds, distanceMeters, …)` |
| `TIMED_HOLD` | Stretch / plank | `TimedHold(durationSeconds, side: BodySide)` |
| `LEGACY_UNKNOWN` | Unmapped legacy | Excluded from library compatibility and most aggregates |

Do not coerce cardio speed into kg volume. Unilateral stretching uses `BodySide.LEFT` / `RIGHT` as separate planned sets.

---

## Execution state machine

**Files:** `domain/model/execution/SetExecutionState.kt`, `ExecutionStateMachine.kt`, `ExecutionCommandRunner.kt`, `WorkoutExecution.kt`  
**Use cases:** `application/usecase/execution/ExecutionUseCases.kt`

Set lifecycle:

```
Ready → Performing → AwaitingConfirmation → Resting → Performing …
                                         ↘ Completed (last set)
Ready → Skipped
```

Illegal transitions return `false` and leave state unchanged. Unconfirmed values (`AwaitingConfirmation`) must **not** enter statistics.

Session-level: `SessionExecutionState` = `READY | ACTIVE | PAUSED | COMPLETED`. `COMPLETED` is irreversible for timer resume (`EXEC-10`).

Commands are gated by `ExecutionCommandRunner`:

- Same `commandId` → success no-op (idempotent).
- `expectedRevision` must match; otherwise `Revision conflict`.
- Successful unique command increments `revision` by 1.

`StartSessionUseCase` requires a **confirmed** `SessionPlan` and fails with `ActiveSessionAlreadyExistsException` if a session or execution is already active. Rest intervals start at **completion** epoch, not confirm time.

In-progress exercise switch uses `InProgressSetDisposition` (`PEEK_ONLY` / `REQUIRE_EXPLICIT_CANCEL` / `SKIP_REMAINING`). Peeking another plan row must not change the execution target (`UI-04`).

**Wiring:** use cases + unit tests exist. No Room `WorkoutExecutionRepository` impl; `WorkoutModeViewModel` is in-memory unless injected in tests.

---

## Timer

**Files:** `domain/model/timer/TimerCalculator.kt`, `TimerModels.kt`  
**Use case:** `RestoreTimerSnapshotUseCase` (read-only snapshot; does not write DB)

Callers pass `now`. Example:

```kotlin
val remaining = TimerCalculator.restRemainingSeconds(restTarget, nowEpochMs)
val overtime = TimerCalculator.restOvertimeSeconds(restTarget, nowEpochMs)
// overtime > 0 does not auto-start the next set
```

`snapshotAfterMissedTicks(...)` rebuilds display after process death. If `bootId` does not match, confidence is `TimerConfidence.Estimated`. Session elapsed by default **includes** pause time (`excludePausesFromElapsed = false`).

---

## Session plan snapshots

**Type:** `SessionPlan` (`domain/model/plan/SessionPlan.kt`)  
**Use cases:** `CreatePlanFromRoutineUseCase`, `CreatePlanFromHistoryUseCase`, `UpdateDraftPlanUseCase`, `ConfirmPlanUseCase`

A plan copies exercise targets at creation. Later routine edits/deletes do **not** mutate an existing plan (`PLAN-01`, `PLAN-06`). `StartSessionUseCase` rejects `isConfirmed == false`.

Mixed drafts preserve stretch sides, strength kg/reps, and cardio time/level (`PLAN-03`).

**Wiring:** application use cases + fakes in tests. No Room `SessionPlanRepository` impl yet.

---

## Statistics and e1RM

**Type:** `WorkoutStatisticsCalculator` (`domain/statistics/WorkoutStatisticsCalculator.kt`)  
**UI path today:** `CalculateOneRepMaxUseCase` (set editor badge) + Room `StatisticsRepositoryImpl` for dashboard volume/PRs

Session summary rules:

- Strength volume = kg × reps; **warmup excluded** from work volume.
- Assisted bodyweight extra load does not count as external volume.
- Cardio → seconds / meters; timed holds → stretching seconds. Never mix units.
- Plan comparison: `achievementRatio` is `null` when planned strength volume is 0 (no divide-by-zero).

e1RM (Epley), domain calculator:

```kotlin
// Valid: weightKg > 0 and 1 <= reps + (rir ?: 0) <= 12
WorkoutStatisticsCalculator.calculateE1RM(weightKg = 100.0, reps = 5)     // 100 * (1 + 5/30)
WorkoutStatisticsCalculator.calculateE1RM(100.0, reps = 8, rir = 2)      // effective reps 10
WorkoutStatisticsCalculator.calculateE1RM(100.0, reps = 16)              // null
```

The live editor use case `CalculateOneRepMaxUseCase` uses the same Epley form with RPE→RIR (`10 - rpe`) and **does not** cap at 12 reps; it rounds to 0.1 kg. Prefer `WorkoutStatisticsCalculator.calculateE1RM` for analytics bounds.

---

## Training load and recovery

**Type:** `TrainingLoadCalculator` (`domain/load/TrainingLoadCalculator.kt`) — **domain tests only** (not in `AppContainer`)

| API | Constraint |
| :--- | :--- |
| `calculateSessionRpeLoad(sessionRpe, durationMinutes)` | AU = sRPE × minutes |
| `aggregateDailyLoad` | Missing RPE → `PARTIALLY_OBSERVED`; empty → `MISSING_LOG` |
| `calculateEwma` / `computeEwmaSeries` | `next = α·current + (1-α)·previous` |
| `calculateLoadTrendMetrics` | `zScore` is `null` when SD ≈ 0 |
| `calculateRecoveryScore` | Four 1–7 items; sleep as-is; stress/fatigue/soreness inverted as `8 - value`; total **4–28** |

---

## Theil–Sen trends

**Type:** `TheilSenTrendCalculator` (`domain/trend/TheilSenTrendCalculator.kt`) — **domain tests only**

- Needs ≥ 2 finite points with distinct `x`; otherwise `null`.
- Pairwise slope cap default `maxPairwiseSampleLimit = 10000`.
- `partitionTrainingBlocks` splits on equipment change or gap > `maxGapDays` (default 30).

---

## Sync conflict helpers

**File:** `domain/sync/SyncModelsAndBoundary.kt`

```kotlin
val winner = SyncConflictResolver.resolve(local, remote)
// local tombstone + revision >= remote → keep local delete
```

`SharingPayloadFilter.filterPersonalData` strips `sleep` / `heartRate` / `stress` / `fatigue` unless `allowHealthSharing`, and `cohortId` unless `allowCohortSharing`. `SyncSchemaGate` fails closed on newer `schemaVersion`.

Production upload path is `PendingUpload` → `SyncWorker` → Firestore (see [ARCHITECTURE.md](ARCHITECTURE.md#offline-first-sync-pipeline)). The in-memory `InMemorySyncOutbox` / `SyncEngineStub` are domain test doubles.

---

## Library, gym, operations, payment, cohort

These are **in-memory domain services** with unit tests. They are **not** registered on `AppContainer` and have **no** Compose screens.

| Service | Invariants (verified in tests) |
| :--- | :--- |
| `RoutineLibraryService` | Non-authors cannot see private drafts or mutate; import is idempotent on `commandId`; personal weights reset to `0.0` by default; `LEGACY_UNKNOWN` fails compatibility |
| `GymCatalogService` | Quantity `null` / `0` / `n`; serial dedupe; fulfillment: available vs out-of-order vs unknown; gym delete must not erase history links (tested) |
| `FacilityOperationsService` | Inactive operators fail access; expired or reused check-in token fails; occupancy is confirmed visits vs missing checkout; equipment “market share” carries a non-exaggeration caveat; dashboard does not expose personal RPE/notes |
| `PaymentProcessingService` | Client amount must match catalog; create is idempotent on `orderId`; webhooks idempotent on `eventId`; cancelled orders reject approval; refund cannot exceed remaining; ledger once per order (`pgFeeRate = 0.033`, `platformFeeRate = 0.05`) |
| `CohortService` | Upload requires opt-in; personal analytics stay on without consent; anonymized payload drops userId/email/raw sets; expose distribution only if `sampleCount >= 10`; disclaimer is non-causal |

---

## Related docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — layers, Room, navigation, DI
- [DEVELOPMENT.md](DEVELOPMENT.md) — build and pitfalls
- [CHANGELOG.md](CHANGELOG.md) — N00–N16 feature list

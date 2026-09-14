# 도메인 API와 제약

영문 원문: [../DOMAIN.md](../DOMAIN.md)

N00~N16에서 도입된 공개 **도메인** 타입과 계산기입니다. 코드 스니펫은 프로덕션 Kotlin과 일치합니다. UI나 Room 연결을 가정하지 않도록 배선 상태를 명시합니다.

---

## 측정 프로필

`MeasurementProfile` / `MeasurementValue` (`domain/model/`):

| 프로필 | 대표 용도 | 값 타입 |
| :--- | :--- | :--- |
| `WEIGHT_AND_REPS` | 바벨 / 덤벨 | `WeightAndReps(weightKg, reps)` |
| `BODYWEIGHT_PLUS_REPS` | 풀업, 딥 | `BodyweightPlusReps(additionalWeightKg, reps, isAssisted)` |
| `TIME_AND_LEVEL` | 스텝밀, 사이클 | `TimeAndLevel(durationSeconds, levelOrSpeed)` |
| `TIME_AND_DISTANCE` | 러닝머신 | `TimeAndDistance(durationSeconds, distanceMeters, …)` |
| `TIMED_HOLD` | 스트레칭 / 플랭크 | `TimedHold(durationSeconds, side: BodySide)` |
| `LEGACY_UNKNOWN` | 매핑되지 않은 레거시 | 라이브러리 호환 및 대부분 집계에서 제외 |

유산소 속도를 kg 볼륨으로 강제 변환하지 마세요. 편측 스트레칭은 `BodySide.LEFT` / `RIGHT`를 서로 다른 계획 세트로 둡니다.

---

## 실행 상태 머신

**파일:** `domain/model/execution/SetExecutionState.kt`, `ExecutionStateMachine.kt`, `ExecutionCommandRunner.kt`, `WorkoutExecution.kt`  
**UseCase:** `application/usecase/execution/ExecutionUseCases.kt`

세트 수명 주기:

```
Ready → Performing → AwaitingConfirmation → Resting → Performing …
                                         ↘ Completed (마지막 세트)
Ready → Skipped
```

불법 전이는 `false`를 반환하고 상태를 유지합니다. 미확인 값(`AwaitingConfirmation`)은 통계에 **들어가면 안 됩니다**.

세션 수준: `SessionExecutionState` = `READY | ACTIVE | PAUSED | COMPLETED`. `COMPLETED`는 타이머 재개에 대해 비가역입니다 (`EXEC-10`).

명령은 `ExecutionCommandRunner`가 게이트합니다.

- 같은 `commandId` → 성공 no-op (멱등).
- `expectedRevision` 불일치 → `Revision conflict`.
- 성공한 고유 명령은 `revision`을 1 증가.

`StartSessionUseCase`는 **확정된** `SessionPlan`이 필요하고, 이미 활성 세션/실행이 있으면 `ActiveSessionAlreadyExistsException`으로 실패합니다. 휴식 구간은 확인 시각이 아니라 **완료** epoch에서 시작합니다.

진행 중 종목 전환은 `InProgressSetDisposition` (`PEEK_ONLY` / `REQUIRE_EXPLICIT_CANCEL` / `SKIP_REMAINING`)을 씁니다. 다른 계획 행을 엿보는 것만으로 실행 타깃이 바뀌면 안 됩니다 (`UI-04`).

**배선:** UseCase와 단위 테스트는 있습니다. Room `WorkoutExecutionRepository` 구현은 없고, `WorkoutModeViewModel`은 테스트에서 주입하지 않으면 메모리만 사용합니다.

---

## 타이머

**파일:** `domain/model/timer/TimerCalculator.kt`, `TimerModels.kt`  
**UseCase:** `RestoreTimerSnapshotUseCase` (읽기 전용 스냅샷, DB 미기록)

호출자가 `now`를 넘깁니다. 예:

```kotlin
val remaining = TimerCalculator.restRemainingSeconds(restTarget, nowEpochMs)
val overtime = TimerCalculator.restOvertimeSeconds(restTarget, nowEpochMs)
// overtime > 0 이어도 다음 세트를 자동 시작하지 않음
```

`snapshotAfterMissedTicks(...)`는 프로세스 종료 후 표시값을 다시 만듭니다. `bootId`가 다르면 `TimerConfidence.Estimated`입니다. 세션 경과 시간은 기본적으로 일시정지 시간을 **포함**합니다 (`excludePausesFromElapsed = false`).

---

## 세션 계획 스냅샷

**타입:** `SessionPlan` (`domain/model/plan/SessionPlan.kt`)  
**UseCase:** `CreatePlanFromRoutineUseCase`, `CreatePlanFromHistoryUseCase`, `UpdateDraftPlanUseCase`, `ConfirmPlanUseCase`

계획은 생성 시점의 종목 타깃을 복사합니다. 이후 루틴 수정/삭제는 기존 계획을 바꾸지 않습니다 (`PLAN-01`, `PLAN-06`). `StartSessionUseCase`는 `isConfirmed == false`를 거절합니다.

혼합 초안은 스트레칭 좌우, 근력 kg/횟수, 유산소 시간/레벨을 보존합니다 (`PLAN-03`).

**배선:** Application UseCase와 테스트 페이크만 있습니다. Room `SessionPlanRepository` 구현은 아직 없습니다.

---

## 통계와 추정 1RM

**타입:** `WorkoutStatisticsCalculator` (`domain/statistics/WorkoutStatisticsCalculator.kt`)  
**현재 UI 경로:** 세트 편집 배지용 `CalculateOneRepMaxUseCase` + 대시보드 볼륨/PR용 Room `StatisticsRepositoryImpl`

세션 요약 규칙:

- 근력 볼륨 = kg × 횟수. **워밍업은** 본운동 볼륨에서 제외.
- 보조(assisted) 맨몸 추가 하중은 외부 볼륨으로 치지 않음.
- 유산소 → 초 / m, 정적 유지 → 스트레칭 초. 단위를 섞지 않음.
- 계획 비교: 계획 근력 볼륨이 0이면 `achievementRatio`는 `null` (0 나눗셈 없음).

e1RM (Epley), 도메인 계산기:

```kotlin
// 유효: weightKg > 0 이고 1 <= reps + (rir ?: 0) <= 12
WorkoutStatisticsCalculator.calculateE1RM(weightKg = 100.0, reps = 5)     // 100 * (1 + 5/30)
WorkoutStatisticsCalculator.calculateE1RM(100.0, reps = 8, rir = 2)      // 유효 횟수 10
WorkoutStatisticsCalculator.calculateE1RM(100.0, reps = 16)              // null
```

라이브 에디터 `CalculateOneRepMaxUseCase`는 같은 Epley 식에 RPE→RIR (`10 - rpe`)을 쓰지만 **12회 상한을 두지 않고** 0.1 kg로 반올림합니다. 분석 경계에는 `WorkoutStatisticsCalculator.calculateE1RM`을 쓰세요.

---

## 훈련 부하와 회복

**타입:** `TrainingLoadCalculator` (`domain/load/TrainingLoadCalculator.kt`) — **도메인 테스트만** (`AppContainer` 미등록)

| API | 제약 |
| :--- | :--- |
| `calculateSessionRpeLoad(sessionRpe, durationMinutes)` | AU = sRPE × 분 |
| `aggregateDailyLoad` | RPE 누락 → `PARTIALLY_OBSERVED`, 빈 목록 → `MISSING_LOG` |
| `calculateEwma` / `computeEwmaSeries` | `next = α·current + (1-α)·previous` |
| `calculateLoadTrendMetrics` | SD ≈ 0이면 `zScore`는 `null` |
| `calculateRecoveryScore` | 1~7 네 항목. 수면은 그대로, 스트레스/피로/근육통은 `8 - value`. 합계 **4~28** |

---

## Theil–Sen 추세

**타입:** `TheilSenTrendCalculator` (`domain/trend/TheilSenTrendCalculator.kt`) — **도메인 테스트만**

- 유한하고 `x`가 다른 점이 2개 이상 필요. 아니면 `null`.
- 쌍별 기울기 상한 기본값 `maxPairwiseSampleLimit = 10000`.
- `partitionTrainingBlocks`는 기구 변경 또는 간격 > `maxGapDays`(기본 30일)에서 블록을 나눔.

---

## 동기화 충돌 헬퍼

**파일:** `domain/sync/SyncModelsAndBoundary.kt`

```kotlin
val winner = SyncConflictResolver.resolve(local, remote)
// 로컬 tombstone + revision >= remote → 로컬 삭제 유지
```

`SharingPayloadFilter.filterPersonalData`는 `allowHealthSharing`이 아니면 `sleep` / `heartRate` / `stress` / `fatigue`를, `allowCohortSharing`이 아니면 `cohortId`를 제거합니다. `SyncSchemaGate`는 더 높은 `schemaVersion`에서 닫힌 실패입니다.

실제 업로드 경로는 `PendingUpload` → `SyncWorker` → Firestore입니다 ([ARCHITECTURE.md](ARCHITECTURE.md#오프라인-우선-동기화-파이프라인)). `InMemorySyncOutbox` / `SyncEngineStub`는 도메인 테스트 더블입니다.

---

## 라이브러리, 짐, 운영, 결제, 코호트

이들은 **인메모리 도메인 서비스**이며 단위 테스트가 있습니다. `AppContainer`에 등록되지 않았고 Compose 화면도 없습니다.

| 서비스 | 테스트로 확인된 불변식 |
| :--- | :--- |
| `RoutineLibraryService` | 비저자는 비공개 초안을 보거나 수정할 수 없음. import는 `commandId`에 멱등. 개인 중량 기본 리셋 `0.0`. `LEGACY_UNKNOWN`은 호환 실패 |
| `GymCatalogService` | 수량 `null` / `0` / `n`. 시리얼 중복 제거. 충족: 사용 가능 vs 고장 vs 미확인. 짐 삭제가 기록 링크를 지워서는 안 됨 (테스트됨) |
| `FacilityOperationsService` | 비활성 운영자는 접근 실패. 만료/재사용 체크인 토큰 실패. 점유는 확정 방문 vs 미체크아웃. 기구 “점유율”에는 과장 금지 주의문. 대시보드는 개인 RPE/노트를 노출하지 않음 |
| `PaymentProcessingService` | 클라이언트 금액은 카탈로그와 일치. `orderId` 생성 멱등. 웹훅은 `eventId` 멱등. 취소된 주문은 승인 거절. 환불은 잔액을 넘을 수 없음. 원장 주문당 1회 (`pgFeeRate = 0.033`, `platformFeeRate = 0.05`) |
| `CohortService` | 업로드는 옵트인 필요. 개인 분석은 동의 없이도 유지. 익명 페이로드는 userId/이메일/원본 세트 제거. 분포 노출은 `sampleCount >= 10`만. 면책 문구는 비인과 |

---

## 관련 문서

- [ARCHITECTURE.md](ARCHITECTURE.md) — 계층, Room, 내비게이션, DI
- [DEVELOPMENT.md](DEVELOPMENT.md) — 빌드와 함정
- [CHANGELOG.md](CHANGELOG.md) — N00~N16 기능 목록

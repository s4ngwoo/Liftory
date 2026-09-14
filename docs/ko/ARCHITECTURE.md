# 아키텍처

영문 원문: [../ARCHITECTURE.md](../ARCHITECTURE.md)

Liftory 런타임 구조를 소스 기준으로 정리한 문서입니다. 공개 인터페이스, 계층 경계, 앱에 실제로 연결된 경로와 도메인 전용 코드를 구분합니다.

---

## 의도

Liftory는 **오프라인 우선** 안드로이드 운동 기록 앱입니다. 로컬 Room 쓰기는 네트워크와 무관하게 성공해야 하며, Firestore 동기화는 최선 노력(best-effort)으로 헬스장 UI를 막지 않습니다.

N00~N16에서 **순수 도메인 계산기**와 **UseCase 오케스트레이션**을 갖춘 4계층 패키지 구조가 도입되었습니다. Presentation은 Room/Firebase 타입이 아니라 UseCase(또는 port)에만 의존합니다.

---

## 계층 맵

```
Presentation  →  Application  →  Domain  ←  Infrastructure
     UI / VM        UseCases      모델,        Room, WorkManager,
                                  포트,        Firebase, FGS
                                  계산기
```

| 계층 | 패키지 | 의존 가능 | 의존 금지 |
| :--- | :--- | :--- | :--- |
| **Domain** | `com.example.domain` | Kotlin 표준 라이브러리만 | `presentation`, `infrastructure`, `android.*`, Room, WorkManager, Compose, Firebase |
| **Application** | `com.example.application` | Domain | `presentation`, `infrastructure`, `android.content.Context`, WorkManager, Compose |
| **Infrastructure** | `com.example.infrastructure` | Domain (+ Android / Room / Firebase) | Presentation UI 타입 |
| **Presentation** | `com.example.presentation` | Application, Domain, `di.AppContainer` | Room Entity, Firestore DTO |

`app/src/test/java/com/example/architecture/ArchitectureRulesTest`의 `ARCH-01`, `ARCH-02`가 import를 검사합니다.

Repository **인터페이스**는 `domain.repository`, 구현체는 `infrastructure.repository`에 둡니다. 시계, 알림, WorkManager는 `domain.port.*`로 주입합니다.

---

## 컴포지션 루트

Hilt/Koin이 아닌 수동 DI입니다.

| 타입 | 파일 | 역할 |
| :--- | :--- | :--- |
| `StrengthLogApplication` | `app/src/main/java/com/example/StrengthLogApplication.kt` | `AppContainer` 보유 |
| `AppContainer` / `DefaultAppContainer` | `app/src/main/java/com/example/di/AppContainer.kt` | Room DB, Repository, UseCase, 시계, `RestTimerManager` lazy 생성 |
| `SyncWorker` | `infrastructure/work/SyncWorker.kt` | `StrengthLogApplication`에서 동일 컨테이너를 읽음 |

ViewModel은 `presentation/Navigation.kt`, `presentation/MainScreen.kt`에서 `AppContainer`의 UseCase를 `ViewModelProvider.Factory`로 주입합니다.

---

## 영속화 (현재 디스크에 있는 것)

Room DB `strength_log.db`, **스키마 버전 3**, `app/schemas/`에 스키마 export.

| Entity | 테이블 | 메모 |
| :--- | :--- | :--- |
| `WorkoutSessionEntity` | sessions | `endTime == null`이면 진행 중 |
| `ExerciseSetEntity` | exercise_sets | v3에서 `isCompleted`, `targetReps` 추가 |
| `ExerciseEntity` | exercises | v2에서 `equipmentType`, `machineBrand` 추가 |
| `RoutineTemplateEntity` + `ExercisePresetEntity` | 루틴 / 프리셋 | Push / Pull / Leg 기본 시드 |
| `PendingUploadEntity` | pending uploads | 동기화 아웃박스 |

마이그레이션: `StrengthLogDatabase`의 `MIGRATION_1_2`, `MIGRATION_2_3`. 최초 실행 시 기본 종목(유산소 6종 포함)을 `OnConflictStrategy.IGNORE`로 시드합니다.

**아직 Room에 없음:** `SessionPlanRepository`, `WorkoutExecutionRepository`는 도메인 포트와 UseCase·단위 테스트(페이크)만 있습니다. `infrastructure`에 `*Impl`이 없습니다. 3페이지 운동 모드 UI(`WorkoutModeViewModel`)는 테스트에서 주입하지 않으면 실행 상태를 메모리에만 둡니다.

---

## 내비게이션과 운동 UX

루트 그래프 (`AppNavigation`):

```
login → main → session/{sessionId} → workout_mode/{sessionId}
```

`main` 하단 탭 (`MainScreen`): `sessions` · `routines` · `exercises` · `stats` (라벨: 운동 / 루틴 / 종목 도감 / 통계).

| 화면 | 코드 | Room 연결 |
| :--- | :--- | :--- |
| 세션 목록/상세, 세트 편집 | `WorkoutSessionViewModel` + Room UseCase | 예 |
| 루틴, 종목 도감, 통계, 백업 | 해당 ViewModel + Room / export | 예 |
| 3페이지 운동 모드 | `WorkoutModeScreen` / `WorkoutModeViewModel` | 내비게이션만; ViewModel은 `wallClock`만 받고 `WorkoutExecutionRepository`를 로드하지 않음 |
| 인증 | `LoginScreen` / `AuthViewModel` | `google-services.json`이 있을 때 Firebase Auth |

세션이 진행 중이면 (`endTime == null`) `MainActivity`가 `NotificationScheduler`로 `WorkoutTimerService`(`foregroundServiceType=specialUse`)를 시작합니다. 알림 탭은 `EXTRA_SESSION_ID`로 `session/{id}`에 복귀합니다. FGS나 알림 권한 실패가 로컬 세션 관찰을 중단하면 안 됩니다.

---

## 오프라인 우선 동기화 파이프라인

1. 로컬 Room 쓰기 (소스 오브 트루스).
2. 선택적으로 `SyncQueueRepository`에 `PendingUpload` enqueue.
3. `StartSyncWorkUseCase` → `SyncScheduler` (`WorkManagerSyncScheduler`) — **네트워크 연결 필요**.
4. `SyncWorker`가 대기 항목을 최대 **20**개 읽고, `retryCount >= 5`는 건너뛴 뒤 `RemoteSyncDataSource.sync` 호출.
5. `FirestoreSyncDataSource`는 `users/{uid}/sessions|sets|exercises|routines`에 기록. Firebase 미초기화 또는 미로그인 → `Result.failure`, 워커가 재시도.

충돌 **정책** (도메인, 테스트/향후 병합용): `SyncConflictResolver` — tombstone + 더 높은 `revision` 우선, 동일 revision은 `updatedAtEpochMs`. 동시 활성 세션은 로컬을 유지하고 `isConflict`를 켜며 조용히 종료하지 않습니다. 아웃박스는 `userId`별로 격리 (`InMemorySyncOutbox`). 프라이버시: `SharingPayloadFilter`. 스키마: `SyncSchemaGate`는 지원 최대보다 높은 `schemaVersion`을 거절합니다.

체크인된 Firestore 규칙 (`app/src/main/assets/firestore.rules`): 인증된 사용자는 `request.auth.uid == userId`일 때만 `users/{userId}/**` 읽기/쓰기.

---

## 시계와 타이머

| 포트 | 기본 구현 | 용도 |
| :--- | :--- | :--- |
| `WallClock` | `WallClock.System` | 영속 구간의 epoch millis |
| `MonotonicClock` | `SystemMonotonicClock` | 지속 시간 계산용 elapsed realtime |
| `NotificationScheduler` | `AndroidNotificationScheduler` | 진행 중 크로노미터 + 휴식 알림 |
| `TimerCalculator` | 도메인 객체 | 저장된 구간에서 순수 투영; UI 틱이 저장 초를 감소시키면 안 됨 |

휴식 시작은 **세트 완료 epoch**이며 확인(confirm) 시각이 아닙니다. 부트 ID 불일치는 `TimerConfidence.Estimated`. 상세는 [DOMAIN.md](DOMAIN.md#타이머).

---

## 아키텍처를 지키는 테스트

| 영역 | 위치 |
| :--- | :--- |
| 계층 import | `architecture/ArchitectureRulesTest.kt` |
| 실행 / 계획 UseCase | `application/usecase/execution`, `application/usecase/plan` |
| 도메인 계산기 | `domain/{execution,statistics,load,trend,gym,library,payment,operations,cohort}` |
| Room / export / sync | `infrastructure/` |
| 운동 모드 UI 상태 | `presentation/workout/WorkoutModeViewModelTest.kt` |

복잡한 모킹보다 경량 페이크를 사용합니다 (`FakePlanRepository`, `FakeTransactionProvider`).

---

## 관련 문서

- [DOMAIN.md](DOMAIN.md) — 공개 도메인 API, 제약, 예시
- [DEVELOPMENT.md](DEVELOPMENT.md) — 환경 설정, 명령, 함정
- [UI_UX_OVERVIEW.md](UI_UX_OVERVIEW.md) — 화면 UX
- [CONTRIBUTING.md](CONTRIBUTING.md) — PR 및 변경 이력 규칙

# 개발 환경 설정

영문 원문: [../DEVELOPMENT.md](../DEVELOPMENT.md)

Liftory를 빌드·테스트·디버그하는 방법입니다. 이 저장소에 없는 모듈이나 플레이버를 가정하지 마세요.

---

## 사전 요구 사항

| 도구 | Gradle 기준 제약 |
| :--- | :--- |
| JDK | **21** (`jvmToolchain(21)`, `sourceCompatibility` 21) |
| Android Studio | 최신 안정판; AGP **8.3.1**, Gradle **9.3.1** |
| SDK | `compileSdk` / `targetSdk` **36**, `minSdk` **24** |
| Kotlin | **2.0.0** (`gradle/libs.versions.toml`) |

애플리케이션 ID: `com.aistudio.strengthlog.vpmq`. 소스 네임스페이스는 여전히 `com.example`입니다.

---

## 클론과 첫 빌드

```bash
git clone https://github.com/s4ngwoo/Liftory.git
cd Liftory
cp .env.example .env
./gradlew assembleDebug
```

Android Studio에서는 중첩 모듈이 아니라 **루트** Gradle 프로젝트를 엽니다.

Secrets Gradle 플러그인(`app/build.gradle.kts`)은 `.env`를 읽고 없으면 `.env.example`을 사용합니다. `.env`는 gitignore 대상입니다. 예제 파일은 `GEMINI_API_KEY`만 안내하며, Gemini/Firebase AI를 패키징할 때만 주석을 해제합니다.

`google-services`는 **`MissingGoogleServicesStrategy.WARN`**입니다. 저장소에 `google-services.json`이 **없습니다**. Firebase 없이도 디버그 빌드는 성공하고, 원격 동기화는 닫힌 실패입니다 (`FirestoreSyncDataSource`는 Firebase 미초기화 또는 미로그인 시 failure를 반환합니다).

---

## 자주 쓰는 명령

| 목적 | 명령 |
| :--- | :--- |
| 디버그 APK | `./gradlew assembleDebug` |
| 단위 테스트 (Robolectric 포함) | `./gradlew testDebugUnitTest` |
| 전체 검사 | `./gradlew check` |
| 계측 테스트 | `./gradlew connectedDebugAndroidTest` |
| 릴리즈 번들 | `./gradlew bundleRelease` (서명 환경 필요; [RELEASING.md](RELEASING.md) 참고) |

아키텍처 import 규칙: `ArchitectureRulesTest` (`ARCH-01` Domain, `ARCH-02` Application).

---

## 디버그 vs 릴리즈 서명

```kotlin
// app/build.gradle.kts (요약)
debug → ${rootDir}/debug.keystore  (alias androiddebugkey / password android)
release → KEYSTORE_PATH 또는 ${rootDir}/my-upload-key.jks
         STORE_PASSWORD, KEY_PASSWORD; alias upload
```

`debug.keystore`는 **gitignore**입니다. `assembleDebug`가 키스토어 없음으로 실패하면 저장소 루트에 표준 안드로이드 디버그 키스토어를 만들거나 Android Studio가 생성한 파일을 `storeFile`로 가리키세요.

`.env`, `local.properties`, 키스토어, `notes/`(비공개 AI/개발 노트, `.gitignore` 등록)는 커밋하지 않습니다.

---

## 런타임 권한과 서비스

| 항목 | 위치 | 함정 |
| :--- | :--- | :--- |
| `POST_NOTIFICATIONS` | API 33+ `MainActivity` | 허용 전까지 크로노미터 알림이 안 보임. 세션은 로컬에서 계속 진행 |
| `FOREGROUND_SERVICE_SPECIAL_USE` | `AndroidManifest.xml` + `WorkoutTimerService` | Android 14+는 센서 권한 없는 `health` 대신 `specialUse` (그렇지 않으면 `SecurityException`) |
| 타이머 TalkBack | 세션 / 운동 UI | 틱 텍스트는 `clearAndSetSemantics`로 억제. 크로노미터에 live region을 다시 켜지 말 것 |

---

## 백업보내기/가져오기

JSON 백업 스키마는 `ExportEntityPayload` (`CURRENT_SCHEMA_VERSION = 2`)입니다. `DataImporterImpl` 동작:

1. 잘못된 JSON과 `1..2` 밖 `schemaVersion` 거절.
2. payload와 Room 양쪽에 없는 `sessionId`를 참조하는 세트 거절.
3. 세션을 넣은 뒤 세트를 넣고, 가져온 행 수를 반환.

CSV도 지원합니다 (`ImportWorkoutDataUseCase.importFromCsv`). 복원은 파괴적 작업이므로 통계 화면은 백업 컨트롤을 분리하고 JSON을 미리 검증합니다.

---

## 문제 해결

| 증상 | 원인 후보 | 확인할 것 |
| :--- | :--- | :--- |
| `assembleDebug`가 키스토어에서 실패 | gitignore된 `debug.keystore` 없음 | 저장소 루트에 디버그 키스토어 생성 |
| Google Services / Firebase 경고 | `google-services.json` 없음 | 이 저장소에서는 정상. 로컬 기록은 동작 |
| 동기화가 올라가지 않음 | 네트워크 제약, 미로그인, Firebase 없음, 또는 `retryCount >= 5` | `SyncWorker` 로그, Room 아웃박스 |
| 두 번째 운동을 시작하지 못함 | 단일 활성 세션 (`endTime == null`) | 이어하기, 또는 `CreateWorkoutSessionUseCase(finishExistingActive = true)` |
| 3페이지 운동 모드가 비어 있음 | `WorkoutModeViewModel`이 `WorkoutExecutionRepository`에 연결되지 않음 | 도메인/UseCase는 있음. Room 구현과 DI 연결은 미완 ([ARCHITECTURE.md](ARCHITECTURE.md)) |
| 아키텍처 테스트 실패 | `domain` 또는 `application`의 금지 import | Android/Room/Firebase 타입을 `infrastructure`로 이동 |
| 키보드가 세트 입력을 가림 | IME 패딩 / 3열 컴팩트 에디터 | 기존 `ExerciseSetEditorSheet` 패턴을 따르고 세로 필드를 추가하지 말 것 |
| 도메인 패키지가 `com.example` | 역사적 네임스페이스 | 검색/리팩터는 Play applicationId가 아니라 `com.example.*` |

---

## 기능 추가 워크플로

1. 도메인 모델 / 계산기 / Repository 포트부터 (순수 Kotlin).
2. 실패하는 단위 테스트 (`app/src/test/...`).
3. Application UseCase (`operator fun invoke`).
4. Infrastructure 어댑터 + 매퍼 (`toDomain()` / `toEntity()`).
5. Presentation: 단일 `UiState` `StateFlow`, 상태 없는 Composable.
6. `./gradlew testDebugUnitTest`.
7. 사용자 영향 변경 → `docs/CHANGELOG.md`와 `docs/ko/CHANGELOG.md`의 `[Unreleased]`.

상세: [CONTRIBUTING.md](CONTRIBUTING.md), [ARCHITECTURE.md](ARCHITECTURE.md), [DOMAIN.md](DOMAIN.md).

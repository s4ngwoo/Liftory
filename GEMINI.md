# Liftory Project Instructions & Antigravity Rules

이 문서는 Antigravity 에이전트가 Liftory 프로젝트에서 작업할 때 항상 준수해야 하는 행동 지침, 클린 아키텍처 및 TDD 규칙입니다.

---

## 1. 프로젝트 핵심 원칙 (Core Principles)

1. **오프라인 우선 (Offline-First)**:
   - 모든 사용자 데이터(운동 기록, 세트, 루틴 등)의 생성/수정/삭제는 **반드시 로컬 Room Database를 우선**으로 처리합니다.
   - 네트워크 통신(Firestore 백엔드 동기화) 실패가 로컬 UI 흐름을 차단해서는 안 됩니다.

2. **클린 아키텍처 (Clean Architecture)**:
   - **의존성 역전 원칙 (Dependency Rule)**:
     ```
     Presentation  ──▶  Domain  ◀──  Data (Infrastructure)
     ```
   - **Domain Layer**:
     - 순수 Kotlin 모듈. Android 프레임워크(Context, View, Compose) 및 외부 라이브러리(Room, Firebase) 의존을 일절 금지합니다.
     - 핵심 비즈니스 로직과 계산(1RM, 볼륨 통계, 날짜/시간 유틸리티 등)은 `UseCase` 및 순수 유틸리티에 캡슐화합니다.
     - 데이터 접근은 `domain.repository`의 인터페이스를 통해서만 정의합니다.
   - **Data (Infrastructure) Layer**:
     - Domain의 Repository 인터페이스 구현체, Room Entity/DAO, Firestore 연동 및 DTO 매퍼.
     - 데이터베이스 엔티티와 도메인 모델을 엄격히 분리하고, 상호 매퍼 확장 함수(`toDomain()`, `toEntity()`)를 관리합니다.
   - **Presentation Layer**:
     - Jetpack Compose 및 MVVM 패턴.
     - ViewModel은 Repository를 직접 의존하기보다 Domain UseCase를 주입받아 비즈니스 흐름을 조율합니다.
     - 단일 `UiState` StateFlow 노출과 UDF(단방향 데이터 흐름) 원칙을 철저히 준수합니다.

3. **테스트 주도 개발 (TDD - Test-Driven Development)**:
   - **Red-Green-Refactor 사이클**:
     1. **Red**: 신규 기능 구현 및 UseCase/ViewModel 로직 작성 전, **실패하는 단위 테스트를 먼저 작성**합니다.
     2. **Green**: 테스트를 통과하기 위한 최소한의 프로덕션 코드를 구현합니다.
     3. **Refactor**: 테스트 통과를 보장한 상태에서 아키텍처 규칙과 가독성을 개선합니다.
   - **회귀 방지 (Regression Prevention)**:
     - 버그나 결함 수정 시, 해당 버그를 재현하는 단위 테스트를 먼저 작성하여 실패를 확인한 후 버그를 수정합니다.
   - **경량 Fake 패턴 우선**:
     - 복잡한 모킹(Mocking) 프레임워크 대신, 도메인 인터페이스를 구현한 경량 `FakeRepository`와 `FakeTransactionProvider`를 활용하여 빠르고 결정론적인(deterministic) 테스트 환경을 유지합니다.
   - **항상 테스트 검증**:
     - 코드 변경 완료 전 반드시 `./gradlew testDebugUnitTest`를 실행하여 100% 테스트 통과를 검증합니다.

---

## 2. 문서화 및 작업 관리 규칙 (Documentation System)

- **공개/비공개 이원화**:
  - `docs/` 및 `README.md`: 영문 원문(Canonical) 및 `docs/ko/` 미러. 사용자/기여자를 위한 공개 문서.
  - `notes/`: 개발자 및 AI 비공개 작업 노트 (`.gitignore` 등록됨, 절대 공개 레포에 커밋하지 않음).
- **작업 시작 시**:
  - `notes/ai-context.md`와 `notes/current-issues.md`를 먼저 확인하여 최신 진행 상황과 부채를 확인합니다.
- **작업 완료 시**:
  - 페이즈/Wave 단위의 기능 구현 후 `notes/logs/YYYY-MM-DD-*.md` 실행 로그를 작성하고 `notes/current-issues.md`를 갱신합니다.
  - 사용자에게 영향을 주는 기능이나 정책 변경 시 `docs/CHANGELOG.md`와 `docs/ko/CHANGELOG.md`의 `[Unreleased]`에 동시 기록합니다.
- **오류 및 실패 분석**:
  - 예상치 못한 빌드 실패나 버그를 디버깅할 때는 기각된 가설과 시도 과정을 `notes/errors/`에 **원인(Cause) → 가설(Hypothesis) → 시도(Attempts) → 실패(Failure) → 성공(Success) → 성공 원인(Success Cause)** 형식으로 남깁니다.

---

## 3. 코드 작성 및 Git 커밋 가이드라인

- **Kotlin / Compose**:
  - Jetpack Compose 최신 Material 3 컴포넌트 권장.
  - Stateless Composable 선호, 이벤트는 람다 콜백으로 상향 전달.
  - 모바일 실기기 사용성을 고려하여 IME 패딩 및 포커스 동선(`singleLine`, `FocusRequester`, `ImeAction`) 최적화.
- **Git 커밋**:
  - Conventional Commits 규격 준수: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `test:` 등.
  - `notes/` 하위 파일은 커밋 대상에 포함되지 않도록 주의.

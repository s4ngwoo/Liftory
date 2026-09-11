# Liftory Project Instructions & Antigravity Rules

이 문서는 Antigravity 에이전트가 Liftory 프로젝트에서 작업할 때 항상 준수해야 하는 행동 지침 및 아키텍처 규칙입니다.

---

## 1. 프로젝트 핵심 원칙 (Core Principles)

1. **오프라인 우선 (Offline-First)**:
   - 모든 사용자 데이터(운동 기록, 세트, 루틴 등)의 생성/수정/삭제는 **반드시 로컬 Room Database를 우선**으로 처리합니다.
   - 네트워크 통신(Firestore 백엔드 동기화) 실패가 로컬 UI 흐름을 차단해서는 안 됩니다.
2. **클린 아키텍처 (Clean Architecture)**:
   - **Domain**: 순수 Kotlin. Android 프레임워크 및 외부 라이브러리(Room, Firebase, UI) 의존 금지. 비즈니스 로직은 `UseCase`에 캡슐화.
   - **Data**: Domain Repository 인터페이스 구현체, Room Entity/DAO, Firestore 연동 및 DTO 매퍼.
   - **Presentation**: Jetpack Compose, MVVM (단일 `UiState` StateFlow 노출, UDF 원칙 준수).

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
  - 예상치 못한 빌드 실패나 버그를 디버깅할 때는 기각된 가설과 시도 과정을 `notes/errors/`에 남깁니다.

---

## 3. 코드 작성 및 Git 커밋 가이드라인

- **Kotlin / Compose**:
  - Jetpack Compose 최신 Material 3 컴포넌트 권장.
  - 비즈니스 계산(예: 1RM, 볼륨 계산)은 Composable 내부가 아닌 Domain UseCase에서 처리.
- **Git 커밋**:
  - Conventional Commits 규격 준수: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:` 등.
  - `notes/` 하위 파일은 커밋 대상에 포함되지 않도록 주의.

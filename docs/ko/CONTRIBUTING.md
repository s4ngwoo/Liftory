# 기여 가이드 (Contributing)

영문 원문: [../CONTRIBUTING.md](../CONTRIBUTING.md)

Liftory 프로젝트에 관심을 가져주셔서 감사합니다!

---

## 개발 워크플로

1. 저장소를 포크하고 기능 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`).
2. [ARCHITECTURE.md](ARCHITECTURE.md)의 4계층을 따릅니다: Domain(순수 Kotlin) → Application UseCase → Infrastructure 어댑터 → Presentation `UiState`.
3. 신규 비즈니스 로직은 프로덕션 코드보다 **먼저** 테스트를 작성합니다. 모킹보다 페이크를 우선합니다.
4. `./gradlew testDebugUnitTest`를 실행합니다 (`ARCH-01`/`ARCH-02` import 규칙 포함). 리뷰 전 `./gradlew check`.
5. 풀 리퀘스트(PR)를 제출합니다.

환경 설정과 흔한 실패: [DEVELOPMENT.md](DEVELOPMENT.md). 도메인 계약: [DOMAIN.md](DOMAIN.md).

---

## 커밋하지 말 것

- `notes/` (비공개 AI/개발 노트)
- `.env`, `local.properties`, `debug.keystore`, 업로드 키스토어
- 개인 절대 경로나 시크릿

---

## 문서 동기화

사용자에게 영향을 주거나 구조적 변경이 발생하는 경우:
- `docs/ko/CHANGELOG.md` 및 `docs/CHANGELOG.md`의 `[Unreleased]` 항목을 갱신합니다.
- 영문 원문(`docs/`)을 기준으로 작성하고 한국어 번역(`docs/ko/`)을 동기화합니다.
- 공개 인터페이스나 배선 상태가 바뀌면 변경 이력만 고치지 말고 [ARCHITECTURE.md](ARCHITECTURE.md) / [DOMAIN.md](DOMAIN.md)를 함께 갱신합니다.

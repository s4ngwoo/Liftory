# 기여 가이드 (Contributing)

영문 원문: [../CONTRIBUTING.md](../CONTRIBUTING.md)

Liftory 프로젝트에 관심을 가져주셔서 감사합니다!

---

## 개발 워크플로

1. 저장소를 포크하고 기능 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`).
2. Kotlin 공식 코딩 컨벤션 및 Android Clean Architecture 원칙을 준수합니다.
3. 새로운 비즈니스 로직 및 UseCase에 대한 단위 테스트를 작성합니다.
4. `./gradlew check` 및 `./gradlew test`를 통과하는지 확인합니다.
5. 풀 리퀘스트(PR)를 제출합니다.

---

## 문서 동기화

사용자에게 영향을 주거나 구조적 변경이 발생하는 경우:
- `docs/ko/CHANGELOG.md` 및 `docs/CHANGELOG.md`의 `[Unreleased]` 항목을 갱신합니다.
- 영문 원문(`docs/`)을 기준으로 작성하고 한국어 번역(`docs/ko/`)을 동기화합니다.

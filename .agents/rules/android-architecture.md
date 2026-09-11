# Android Clean Architecture & Compose Rule

Liftory 프로젝트의 아키텍처 및 안드로이드 코드 작성 규칙입니다.

---

## 1. 계층 간 의존성 규칙

```
Presentation -> Domain <- Data
```

- `Domain` 레이어는 `Data` 레이어나 `Presentation` 레이어를 참조해서는 안 됩니다.
- Repository 인터페이스는 `domain.repository` 패키지에 선언하고, 구현체는 `data.repository` (또는 `infrastructure.repository`)에 둡니다.
- 데이터베이스 Entity(Room)나 원격 DTO(Firestore)는 도메인 모델과 분리하고 상호 매핑 확장 함수(`toDomain()`, `toEntity()`)를 작성합니다.

## 2. Jetpack Compose UI 컨벤션

- **Stateless Composable 선호**: 재사용 가능한 UI 컴포넌트는 State 대신 값과 람다 이벤트 콜백을 전달받도록 설계합니다.
- **Preview 제공**: 주요 컴포넌트와 화면에는 `@Preview` 어노테이션을 작성하여 디자인 확인이 가능하도록 합니다.
- **단방향 데이터 흐름 (UDF)**:
  - ViewModel: 이벤트 수신 -> 상태 변경 -> `StateFlow<UiState>` 발행
  - UI: 상태 수신 -> 렌더링 -> 사용자 인터랙션 발생 시 람다 이벤트 ViewModel로 전달

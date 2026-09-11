---
name: liftory-clean-architecture
description: Guide and automated checks for implementing Domain, Data, and Presentation layers in Liftory according to Clean Architecture and Offline-First principles.
---

# Liftory Clean Architecture & Offline-First Workflow

This skill guides the implementation and validation of new features in Liftory.

## 1. 계층별 작업 순서 (Feature Implementation Flow)

1. **Domain Layer First**:
   - `com.example.domain.model`: 필요한 비즈니스 엔티티 정의 (순수 Kotlin)
   - `com.example.domain.repository`: 필요한 동작 인터페이스 선언 (`Result<T>` 또는 `Flow<T>`)
   - `com.example.domain.usecase`: 단일 책임 원칙을 따르는 UseCase 클래스 작성 (`operator fun invoke(...)`)

2. **Data Layer**:
   - `com.example.data.local`: Room Entity 및 DAO 정의 / 갱신
   - `com.example.data.repository`: Domain Repository 인터페이스 구현
   - **Offline-First 원칙**: 로컬 Room DB에 먼저 저장하고, 네트워크 가용 시 Firestore 백그라운드 동기화 큐에 등록.

3. **Presentation Layer**:
   - `com.example.presentation.*`:
     - ViewModel: UseCase를 주입받아 호출하고 `StateFlow<UiState>` 업데이트
     - Screen: Stateless Composable 분리 및 Preview 구성

---

## 2. 체크리스트

- [ ] Domain 모듈에 Android 의존성(`android.*`, `androidx.*`)이 없는가?
- [ ] Room Entity나 Firestore DTO가 Domain 엔티티와 분리되어 매퍼(`toDomain()`, `toEntity()`)를 거치는가?
- [ ] ViewModel이 단일 `UiState` 데이터 클래스를 발행하는가?
- [ ] 기능/버그 수정 완료 후 `notes/logs/`에 실행 로그를 남겼는가?

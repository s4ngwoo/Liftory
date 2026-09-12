# Liftory

> 클린 아키텍처(Clean Architecture) 기반 오프라인 우선 근력 운동 기록 및 통계 안드로이드 앱

영문 원문: [README.md](README.md) · [문서 목록](docs/ko/README.md) · [변경 이력](docs/ko/CHANGELOG.md)

---

## 개요

**Liftory**는 헬스 및 스트렝스 트레이닝을 기록하고 점진적 과부하를 추적할 수 있도록 설계된 안드로이드 애플리케이션입니다. 인터넷 연결이 원활하지 않은 헬스장 환경에서도 안정적인 오프라인 우선 경험을 제공하며, 선택적으로 클라우드 동기화를 지원합니다.

- **오프라인 우선 (Offline-First)**: 로컬 DB(Room) 기반으로 완전한 오프라인 사용성과 데이터 안정성 보장.
- **클린 아키텍처 (Clean Architecture)**: Domain, Data, Presentation 레이어가 명확히 분리된 테스트 가능하고 유지보수하기 쉬운 구조.
- **AI 기반 인사이트**: Gemini API 연동을 통한 운동 피드백 및 루틴 제안 기능.

---

## 주요 기능

- **3페이지 집중 운동 모드 & 세션 HUD**: 타이머 HUD ↔ 이번 종목(계획 vs 실제 비교) ↔ 오늘 계획을 분리한 수평 페이저, 48dp 터치 타겟, 스크린 리더(TalkBack) 최적화 및 키보드(IME) 가림 없는 동선.
- **영속 타이머 아키텍처**: 배터리 소모 없는 수학적 시간 투영(`TimerCalculator`), 인앱 상단 고정 네비게이션 배너 및 Android 상단바 포그라운드 서비스(`specialUse`) 네이티브 크로노미터 알림.
- **멱등 실행 상태 머신**: 세트 및 세션 상태 머신(`SetExecutionState`, `SessionExecutionState`)과 멱등 command ID / revision 게이트로 중복 저장 및 비정상 상태 전이 원천 방지.
- **스마트 세트 입력 & 이전 기록 참조**: 키보드 가림 0%의 가로 3열 컴팩트 입력창, 직전 세트 1터치 복사(`[↺ 이전 세트 복사]`), 동일 종목 지난 세션 기록 비교 및 1터치 복사(`[지난 세션 복사]`).
- **불변 세션 계획 스냅샷**: 루틴 수정/삭제 시에도 기존 및 진행 중인 운동 계획(`SessionPlan`)의 불변성을 완벽히 보존.
- **고급 통계 및 부하 과학**: 지표 분리(kg, 초, m), RIR 보정 추정 1RM(1~12회), session-RPE 기반 임의 단위(AU) 훈련 부하 및 EWMA 감쇠 시계열, Hooper 회복 척도, 극단치에 강건한 Theil-Sen 추세 분석.
- **기구 카탈로그 & 충족률 평가**: 기구 모델 분리, 3개 수량 상태(null, 0, n), 루틴 기구 충족률 평가 및 세션 내 실시간 대체 종목 계획 수립.
- **오프라인 우선 동기화 & 충돌 해결**: 로컬 Room DB 우선 쓰기, 로컬 최신 revision 및 삭제 tombstone 우선순위, 사용자별 동기화 큐 격리 및 민감 건강 데이터 프라이버시 필터링.

---

## 공개 문서

모든 공개 문서는 [`docs/`](docs/ko/README.md) 디렉터리에서 확인할 수 있습니다:

| 문서 | 한국어 (KO) | 영문 (EN) | 설명 |
| :--- | :--- | :--- | :--- |
| **문서 목차** | [docs/ko/README.md](docs/ko/README.md) | [docs/README.md](docs/README.md) | 전체 공개 문서 맵 |
| **변경 이력** | [docs/ko/CHANGELOG.md](docs/ko/CHANGELOG.md) | [docs/CHANGELOG.md](docs/CHANGELOG.md) | 버전별 변경 사항 및 Unreleased |
| **UI/UX 개요** | [docs/ko/UI_UX_OVERVIEW.md](docs/ko/UI_UX_OVERVIEW.md) | [docs/UI_UX_OVERVIEW.md](docs/UI_UX_OVERVIEW.md) | 화면 갤러리 및 UX 평가 가이드 |
| **기여 가이드** | [docs/ko/CONTRIBUTING.md](docs/ko/CONTRIBUTING.md) | [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | 기여 및 PR 규칙 |
| **보안 정책** | [docs/ko/SECURITY.md](docs/ko/SECURITY.md) | [docs/SECURITY.md](docs/SECURITY.md) | 보안 취약점 보고 절차 |
| **릴리즈 가이드** | [docs/ko/RELEASING.md](docs/ko/RELEASING.md) | [docs/RELEASING.md](docs/RELEASING.md) | 릴리즈 태그 및 배포 절차 |

---

## 기술 스택

- **플랫폼**: Android (Min SDK 24, Target SDK 36)
- **언어**: Kotlin 2.x
- **UI**: Jetpack Compose, Material 3
- **아키텍처**: MVVM / Clean Architecture
- **백엔드/서비스**: Firebase (Firestore, Auth), Google Cloud

---

## 시작하기

1. 저장소 복제:
   ```bash
   git clone https://github.com/your-username/Liftory.git
   ```
2. 환경 변수 및 설정 파일 준비:
   ```bash
   cp .env.example .env
   ```
3. Android Studio에서 프로젝트를 열거나 빌드 실행:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

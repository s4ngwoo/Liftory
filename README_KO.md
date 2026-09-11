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

- **운동 세션 기록 & 통합 세션 HUD**: 실시간 누적 운동 시간 계측, 인터랙티브 휴식 초시계/카운트다운(+30초 연장, 일시정지, 퀵 프리셋), 종목별 평가/피드백 메모 및 RPE 기반 실시간 1RM 계산.
- **전역 백그라운드 & PiP 타이머**: 3단계 지속 타이머 시스템 — 인앱 상단 고정 네비게이션 배너, Android 포그라운드 서비스 기반 상단바 네이티브 크로노미터 알림(CPU 웨이크락 제로, 배터리 최적화), 홈 스와이프 자동 진입을 지원하는 16:9 다크 모드 PiP 플로팅 HUD 팝업.
- **스마트 세트 입력 & 이전 기록 참조**: 키보드 가림 0%의 가로 3열 컴팩트 입력창, 직전 세트 1터치 복사(`[↺ 이전 세트 복사]`), 동일 종목 지난 세션 기록 비교 및 1터치 복사(`[지난 세션 복사]`).
- **루틴 및 템플릿 관리/수정**: 루틴 이름 변경, 종목 추가/삭제, 목표 무게/횟수 프리셋 설정, 종목 라이브러리 검색 및 기구 필터, 최근 수행 날짜 표시 및 [▶ 이 루틴으로 세션 시작] 지원.
- **운동 기구 유형 분류**: 프리웨이트 vs 머신 분류 및 주요 머신 브랜드(Hammer Strength, Cybex, Life Fitness, Newtech 등) 선택/필터 지원.
- **통계 및 히스토리**: 추정 1RM 진행도, 부위별 볼륨, JSON/CSV 백업 데이터 내보내기 및 가져오기(Import) 복원.
- **클라우드 백업 및 동기화**: Firebase / Firestore 기반 오프라인 우선 양방향 동기화.

---

## 공개 문서

모든 공개 문서는 [`docs/`](docs/ko/README.md) 디렉터리에서 확인할 수 있습니다:

| 문서 | 한국어 (KO) | 영문 (EN) | 설명 |
| :--- | :--- | :--- | :--- |
| **문서 목차** | [docs/ko/README.md](docs/ko/README.md) | [docs/README.md](docs/README.md) | 전체 공개 문서 맵 |
| **변경 이력** | [docs/ko/CHANGELOG.md](docs/ko/CHANGELOG.md) | [docs/CHANGELOG.md](docs/CHANGELOG.md) | 버전별 변경 사항 및 Unreleased |
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

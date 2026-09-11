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

- **운동 세션 기록**: 종목, 세트, 반복 수(Reps), 중량, RPE, 메모 등 상세 기록.
- **통계 및 히스토리**: 추정 1RM 진행도, 부위별 볼륨, 주간/월간 운동 빈도 시각화.
- **루틴 및 템플릿 관리**: 분할 루틴 및 개인 맞춤 템플릿 생성/불러오기.
- **클라우드 백업 및 동기화**: Firebase / Firestore 기반 백업 지원.

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

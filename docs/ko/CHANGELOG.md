# 변경 이력 (Changelog)

영문 원문: [../CHANGELOG.md](../CHANGELOG.md)

이 프로젝트의 주요 변경 사항은 이 문서에 기록됩니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며,
[유의적 버전(Semantic Versioning)](https://semver.org/lang/ko/)을 준수합니다.

---

## [Unreleased]

### 추가됨 (Added)
- 공개/비공개 이원화 문서화 시스템 구축 (`docs/` 및 `notes/`).
- Jetpack Compose 기반 Android Clean Architecture 기본 구조.
- 루틴 템플릿 세부 수정 기능: 루틴 이름 변경, 운동 종목 추가/삭제, 목표 무게/횟수 프리셋 설정, 검색 및 기구 필터가 지원되는 운동 라이브러리 피커 다이얼로그 (`UpdateRoutineTemplateUseCase`).
- 최근 동일 운동 수행 기록 참조 기능: 이전 세션 수행 날짜 및 세트별 무게/횟수/RPE 참조 카드 노출, 세트 입력 시 1터치 자동 복사(`[지난 세션 복사]`) 지원 (`GetLastExerciseHistoryUseCase`).
- 운동 기구 유형 분류 (프리웨이트 vs 머신): `EquipmentType` 도입 및 머신 브랜드(Hammer Strength, Cybex, Life Fitness, Newtech 등) 선택, 기구별 필터 칩 지원.
- 데이터베이스 마이그레이션: Room 스키마 v2 마이그레이션(`MIGRATION_1_2`) 안전 적용 (기구 타입 및 머신 브랜드 컬럼 추가 및 인덱스 생성).
- 운동 세션(제목/메모) 수정 및 세션 삭제 기능 (확인 다이얼로그 및 세트 동시 CASCADE 삭제 연동).
- 루틴 카드 전체 터치 및 포함된 운동 종목 불릿 프리뷰, [▶ 이 루틴으로 세션 시작] 버튼 및 루틴 삭제 지원.
- 실시간 누적 운동 시간 타이머(`⏱️ 00:23:45`) 및 세션 완료 다이얼로그.
- 세트 저장 시 자동 시작되는 실시간 휴식 초시계(+30초, 일시정지, 스톱워치 모드) 도킹 바.
- `SessionNotesManager` 기반 종목별 평가/피드백 코멘트 작성 및 저장 기능.
- Epley 공식 및 RPE/RIR 조정을 반영한 실시간 추정 1RM 계산(`CalculateOneRepMaxUseCase`) 및 세트 입력창 내 실시간 `🔥 예상 1RM` 배지 피드백.
- 세트 입력창 이전 세트 무게/횟수 원터치 자동 완성(`[↺ 이전 세트 복사]`) 버튼 및 연한 고스트 플레이스홀더 안내.
- JSON 및 CSV 백업 데이터 가져오기 및 Room DB 복원(`ImportWorkoutDataUseCase` & `DataImporterImpl`) 지원 및 통계 화면 UI 연동.
- 세션 상세 화면 UI 일관성 개편: 상단 시간 통합 대시보드(누적 운동 시간, 인터랙티브 휴식 타이머 [+30초/일시정지/종료/퀵 칩], 운동 완료 버튼)와 중단 운동 콘텐츠 영역(세션 제목/메모, 수정/삭제, 운동 종목 추가, 세트/무게/1RM/피드백)으로 화면을 명확히 양분하여 일관성 확보 및 하단 도크 제거.
- 인앱 상단 지속 운동 타이머 배너: 다른 탭(루틴, 종목, 통계)으로 이동해도 상단에 실시간 초 단위 타이머 배너가 고정 노출되며, `[운동 복귀 >]` 1터치로 즉시 진행 중인 세션으로 복귀 지원.
- 시스템 상단바 크로노미터 알림 (포그라운드 서비스): 앱을 백그라운드로 전환하거나 다른 앱을 사용할 때 Android OS 네이티브 크로노미터 기반 상단바 알림(`WorkoutTimerService`)이 배터리 소모 없이 실시간 시간을 계측하며 원터치 복귀 지원.
- 도메인 활성 세션 반응형 관찰 (`ObserveActiveWorkoutSessionUseCase`): 클린 아키텍처 기반 실시간 진행 중 운동 세션 Flow 관찰 UseCase 구현.
- 유산소(Cardio) 운동 종목 및 전용 세트 입력 UX 지원:
  - 6종 기본 유산소 종목 공식 등록: 러닝머신(DRAX), 천국의 계단(Matrix), 실내 사이클(Concept2), 마이마운틴 인클라인(MyMountain), 일립티컬(Life Fitness), 로잉머신(Concept2).
  - 도메인 `EquipmentType`에 `CARDIO` 및 `isCardio` 판별 헬퍼 추가.
  - 운동 라이브러리, 커스텀 운동 추가 다이얼로그, 루틴 운동 선택 창 전반에 `[🏃 유산소]` 칩 필터 제공.
  - 유산소 세트 입력 UX 최적화: `무게/횟수` 대신 `속도·레벨 / 시간(분)`으로 스마트 라벨 전환, 1RM 배지 자동 숨김, `+5분 / +10분 / +15분` 시간 증감 칩 지원.
  - 세션 상세 카드 내 `속도 6.0 · 20분` 형식 및 `Set | 속도/레벨 | 시간(분) | RPE` 전용 헤더 노출.
- 단일 활성 세션(Single Active Session) 정책 강제 및 동시 다중 세션 방지:
  - 도메인, 데이터, 프레젠테이션 전 계층에서 동시에 2개 이상의 운동 세션(`endTime == null`)이 실행될 수 없도록 단일 활성 세션 규칙 엄격 적용.
  - `CreateWorkoutSessionUseCase`에 활성 세션 중복 검증 로직 및 `ActiveSessionAlreadyExistsException` 예외 추가, 기존 운동 자동 완료 옵션(`finishExistingActive`) 지원.
  - 세션 목록 화면(FAB 및 시작 버튼)과 루틴 라이브러리 화면("루틴 시작")에서 이미 진행 중인 운동이 있을 때 친절한 충돌 다이얼로그 팝업 제공 (진행 중인 운동으로 이동 vs 기존 운동 종료 후 새로 시작 vs 취소).
  - 세션 목록 카드(`BentoSessionCard`)에서 진행 중인 세션에 `[🔥 진행 중]` 뱃지, 프라이머리 하이라이트 테두리 및 실시간 상태 안내 제공 (완료된 세션은 소요 시간 표시).
  - 레거시 데이터 자가 치유(Self-healing): 이전 빌드에서 중복 생성된 미종료 활성 세션들을 앱 구동 시 가장 최근 1개만 유지하고 이전 세션들은 안전하게 자동 완료 처리.

### 변경됨 (Changed)

- 세션 목록 화면 FAB 정리: 등록된 세션이 없는 초기 빈 상태에서는 우측 하단 `+` 플로팅 액션 버튼을 숨기고 중앙의 "오늘의 운동 시작하기" 카드 버튼만 깔끔하게 노출하도록 개선하여 시선 분산 방지.
- 타이머 경험 일원화: 다른 앱 사용 시 화면을 가리는 화면 속 화면(PiP) 플로팅 팝업을 완전히 제거하고, 상단바 네이티브 크로노미터 알림(`WorkoutTimerService`) 및 인앱 상단 고정 배너로 단독 집중.
- 포그라운드 서비스 권한 안정화: Android 14+ / Android 16에서 하드웨어 센서 권한 없이 `health` 타입 시작 시 발생하던 `SecurityException`을 해결하기 위해 `specialUse` 타입으로 전환.

### 수정됨 (Fixed)
- 세트 입력창에서 숫자 패드 엔터 클릭 시 줄바꿈이 되던 현상을 `singleLine = true`, `ImeAction.Next`/`Done`, `FocusRequester`로 해결.
- 키보드가 올라올 때 세트 입력 필드가 화면 밑으로 잘려 보이지 않던 문제를 가로 3열 컴팩트 배치(`Row`, 높이 64dp), `skipPartiallyExpanded = true` 및 `verticalScroll`을 적용하여 100% 가림 없는 시각적 노출로 완벽 해결.

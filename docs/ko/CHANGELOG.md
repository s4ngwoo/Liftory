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
- 세션 상세 화면 UI 일관성 개편: 상단 시간 통합 대시보드(누적 운동 시간, 인터랙티브 휴식 타이머 [+30초/일시정지/종료/퀵 칩], 운동 완료 버튼, PiP 버튼)와 중단 운동 콘텐츠 영역(세션 제목/메모, 수정/삭제, 운동 종목 추가, 세트/무게/1RM/피드백)으로 화면을 명확히 양분하여 일관성 확보 및 하단 도크 제거.
- 인앱 상단 지속 운동 타이머 배너: 다른 탭(루틴, 종목, 통계)으로 이동해도 상단에 실시간 초 단위 타이머 배너가 고정 노출되며, `[운동 복귀 >]` 1터치로 즉시 진행 중인 세션으로 복귀 지원.
- 시스템 상단바 크로노미터 알림 (포그라운드 서비스): 앱을 백그라운드로 전환하거나 다른 앱을 사용할 때 Android OS 네이티브 크로노미터 기반 상단바 알림(`WorkoutTimerService`)이 배터리 소모 없이 실시간 시간을 계측하며 원터치 복귀 지원.
- 화면 속 화면(PiP) 플로팅 팝업 타이머: 홈 제스처 스와이프 시 자동 진입(Android 12+) 또는 상단바 PiP 버튼 클릭 시 16:9 다크 모드 플로팅 HUD 팝업을 띄워 다른 앱 사용 중에도 누적 운동 시간, 휴식 타이머 카운트다운, 세션 제목을 실시간 모니터링 가능.
- 도메인 활성 세션 반응형 관찰 (`ObserveActiveWorkoutSessionUseCase`): 클린 아키텍처 기반 실시간 진행 중 운동 세션 Flow 관찰 UseCase 구현.

### 수정됨 (Fixed)
- CSV 백업 복원 시 모든 세션이 `endTime = null`로 들어와 진행 중 운동으로 되살아나던 문제를 수정. 내보내기 시 `sessionEndTime`을 포함하고, 해당 열이 없는 기존 CSV는 완료된 세션으로 복원한다.
- 세트 입력창에서 숫자 패드 엔터 클릭 시 줄바꿈이 되던 현상을 `singleLine = true`, `ImeAction.Next`/`Done`, `FocusRequester`로 해결.
- 키보드가 올라올 때 세트 입력 필드가 화면 밑으로 잘려 보이지 않던 문제를 가로 3열 컴팩트 배치(`Row`, 높이 64dp), `skipPartiallyExpanded = true` 및 `verticalScroll`을 적용하여 100% 가림 없는 시각적 노출로 완벽 해결.

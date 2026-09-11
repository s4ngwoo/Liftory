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
- 운동 세션(제목/메모) 수정 및 세션 삭제 기능 (확인 다이얼로그 및 세트 동시 CASCADE 삭제 연동).
- 루틴 카드 전체 터치 및 포함된 운동 종목 불릿 프리뷰, [▶ 이 루틴으로 세션 시작] 버튼 및 루틴 삭제 지원.
- 실시간 누적 운동 시간 타이머(`⏱️ 00:23:45`) 및 세션 완료 다이얼로그.
- 세트 저장 시 자동 시작되는 실시간 휴식 초시계(+30초, 일시정지, 스톱워치 모드) 도킹 바.
- `SessionNotesManager` 기반 종목별 평가/피드백 코멘트 작성 및 저장 기능.
- `GEMINI.md` 및 `.agents/rules/`에 클린 아키텍처 및 TDD 에이전트 규칙 공식 추가.

### 수정됨 (Fixed)
- 세트 입력창에서 숫자 패드 엔터 클릭 시 줄바꿈이 되던 현상을 `singleLine = true`, `ImeAction.Next`/`Done`, `FocusRequester`로 해결.
- 키보드가 올라올 때 세트 입력 필드가 화면 밑으로 잘려 보이지 않던 문제를 가로 3열 컴팩트 배치(`Row`, 높이 64dp), `skipPartiallyExpanded = true` 및 `verticalScroll`을 적용하여 100% 가림 없는 시각적 노출로 완벽 해결.

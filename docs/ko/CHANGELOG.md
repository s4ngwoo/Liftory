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
- `ExerciseSetEditorSheet` 하단 키보드 위치에 맞춘 상향식(Bottom-to-Top) 레이아웃 및 진입 시 자동 포커스.

### 수정됨 (Fixed)
- 세트 입력창에서 숫자 패드 엔터 클릭 시 줄바꿈이 되던 현상을 `singleLine = true`, `ImeAction.Next`/`Done`, `FocusRequester`를 연동하여 다음 필드 자동 이동 및 세트 저장으로 개선.

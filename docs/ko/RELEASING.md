# 릴리즈 가이드 (Release Process)

영문 원문: [../RELEASING.md](../RELEASING.md)

이 문서는 Liftory의 새 버전을 릴리즈하는 절차를 설명합니다.

---

## 릴리즈 체크리스트

1. **빌드 및 테스트 확인**:
   ```bash
   ./gradlew check
   ./gradlew test
   ```
2. **버전 번호 업데이트**:
   - `app/build.gradle.kts`의 `versionCode` 및 `versionName` 갱신.
3. **변경 이력 정리**:
   - `docs/ko/CHANGELOG.md` 및 `docs/CHANGELOG.md`의 `[Unreleased]` 항목을 `[x.y.z] - YYYY-MM-DD` 버전 헤더로 이동.
4. **Git 태그 생성 및 푸시**:
   ```bash
   git tag -a vx.y.z -m "Release vx.y.z"
   git push origin vx.y.z
   ```
5. **릴리즈 패키지 빌드**:
   ```bash
   ./gradlew bundleRelease
   ```

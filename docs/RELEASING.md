# Release Process

This document describes how to release a new version of Liftory.

Korean translation: [ko/RELEASING.md](ko/RELEASING.md)

---

## Release Checklist

1. **Check Build & Tests**:
   ```bash
   ./gradlew check
   ./gradlew test
   ```
2. **Update Version**:
   - Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
3. **Update Changelog**:
   - Move items from `[Unreleased]` to `[x.y.z] - YYYY-MM-DD` in both `docs/CHANGELOG.md` and `docs/ko/CHANGELOG.md`.
4. **Git Tag & Push**:
   ```bash
   git tag -a vx.y.z -m "Release vx.y.z"
   git push origin vx.y.z
   ```
5. **Build Release APK/AAB**:
   ```bash
   export KEYSTORE_PATH=/path/to/upload.jks   # default: ${rootDir}/my-upload-key.jks
   export STORE_PASSWORD=...
   export KEY_PASSWORD=...                    # alias is `upload`
   ./gradlew bundleRelease
   ```

   Debug builds use gitignored `${rootDir}/debug.keystore`. See [DEVELOPMENT.md](DEVELOPMENT.md) for JDK, secrets, and Firebase (`google-services.json` is optional; missing file is a Gradle warning).

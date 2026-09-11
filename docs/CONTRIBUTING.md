# Contributing to Liftory

Thank you for your interest in contributing to Liftory!

Korean translation: [ko/CONTRIBUTING.md](ko/CONTRIBUTING.md)

---

## Development Workflow

1. Fork the repo and create a feature branch (`git checkout -b feature/amazing-feature`).
2. Adhere to Kotlin and Android Clean Architecture guidelines.
3. Write tests for any new business logic or domain use cases.
4. Ensure code passes `./gradlew check` and tests pass `./gradlew test`.
5. Submit a Pull Request.

---

## Documentation Sync

If your PR introduces user-facing or architectural changes:
- Update `docs/CHANGELOG.md` under `[Unreleased]`.
- Keep English documents as canonical and mirror to `docs/ko/` where applicable.

# Contributing to Liftory

Thank you for your interest in contributing to Liftory!

Korean translation: [ko/CONTRIBUTING.md](ko/CONTRIBUTING.md)

---

## Development Workflow

1. Fork the repo and create a feature branch (`git checkout -b feature/amazing-feature`).
2. Follow the four-layer layout in [ARCHITECTURE.md](ARCHITECTURE.md): Domain (pure Kotlin) → Application use cases → Infrastructure adapters → Presentation `UiState`.
3. Write tests for new business logic **before** production code. Prefer fakes over mocks.
4. Run `./gradlew testDebugUnitTest` (covers `ARCH-01`/`ARCH-02` import rules). `./gradlew check` before review.
5. Submit a Pull Request.

Setup, secrets, and common failures: [DEVELOPMENT.md](DEVELOPMENT.md). Domain contracts: [DOMAIN.md](DOMAIN.md).

---

## Do not commit

- `notes/` (private AI/dev notes)
- `.env`, `local.properties`, `debug.keystore`, upload keystores
- Personal absolute paths or secrets

---

## Documentation Sync

If your PR introduces user-facing or architectural changes:
- Update `docs/CHANGELOG.md` under `[Unreleased]`.
- Keep English documents as canonical and mirror to `docs/ko/` where applicable.
- If a public interface or wiring status changes, update [ARCHITECTURE.md](ARCHITECTURE.md) / [DOMAIN.md](DOMAIN.md) rather than only the changelog.
